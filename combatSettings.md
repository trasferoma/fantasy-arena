# CombatSettings — guida ai parametri di bilanciamento

`CombatSettings` (`src/main/java/it/fantasyarena/combat/config/CombatSettings.java`) è il punto unico dove vivono i pesi, i costi e le soglie tarabili del motore di combattimento. Nessuna formula vive qui: solo dati, consumati dai resolver del package `combat.engine` e da `DefaultRatingStrategy`. I valori di default (`defaults()`) sono taratura empirica provvisoria.

Questo documento spiega, categoria per categoria, cosa fa ogni parametro e dove incide nel calcolo.

---

## 1. `RatingWeights` — Rating intrinseci, Health e Stamina massimi

Consumati da `DefaultRatingStrategy`, **una sola volta alla creazione del Fighter** (non turno per turno): trasformano caratteristiche del personaggio + arma/armatura/scudo in `offensiveRating`, `defensiveRating`, `maxHealth`, `maxStamina`.

### Offensive Rating

```
offensiveRating = strengthOffenseWeight × STRENGTH
                 + agilityOffenseWeight  × AGILITY
                 + weaponAttackWeight    × weapon.attack()
                 + offensiveClassBonus[classe]
                 + offensiveRaceBonus[razza]
```

| Parametro | Default | Effetto |
|---|---|---|
| `strengthOffenseWeight` | 1.0 | Quanto la Forza pesa sull'attacco. Alzarlo premia i personaggi forti. |
| `agilityOffenseWeight` | 0.5 | Quanto l'Agilità pesa sull'attacco (metà della Forza di default). |
| `weaponAttackWeight` | 2.0 | Moltiplicatore dell'attacco dell'arma: è il fattore con più leva, perché l'arma pesa il doppio di ogni punto caratteristica. |
| `offensiveClassBonus` | Warrior 4, Ranger 3, Thief 2, Mage 1 | Bonus fisso additivo per classe. Definisce la gerarchia offensiva tra classi a parità di stat. |
| `offensiveRaceBonus` | Orc 3, Undead 2, Human 1, Elf 1 | Bonus fisso additivo per razza. |

### Defensive Rating

```
defensiveRating = resistanceDefenseWeight × RESISTANCE
                 + agilityDefenseWeight    × AGILITY
                 + armourDefenseWeight     × armour.defense()
                 + shieldDefenseWeight     × shield.defense()   (se presente)
                 + defensiveClassBonus[classe]
                 + defensiveRaceBonus[razza]
```

| Parametro | Default | Effetto |
|---|---|---|
| `resistanceDefenseWeight` | 1.0 | Quanto la Resistenza pesa sulla difesa. |
| `agilityDefenseWeight` | 0.5 | Quanto l'Agilità pesa sulla difesa. |
| `armourDefenseWeight` | 2.0 | Moltiplicatore della difesa dell'armatura. |
| `shieldDefenseWeight` | 2.0 | Moltiplicatore della difesa dello scudo, se il combattente ne ha uno. |
| `defensiveClassBonus` | Warrior 4, Thief 1, Ranger 1, Mage 0 | Bonus fisso per classe: il Warrior domina anche in difesa, il Mage non ha bonus. |
| `defensiveRaceBonus` | Undead 3, Orc 2, Human 1, Elf 1 | Bonus fisso per razza. |

**Incidenza sul combattimento**: `offensiveRating`/`defensiveRating` sono gli ingredienti principali di `DamageCalculator.calculateDamage` (vedi §4) e di `computeParryChance` in `DefenseResolver` (vedi §3). Sono valori **statici per l'intero duello**: cambiarli sposta l'equilibrio "a monte", prima ancora che iniziscano i tiri di dado.

### Health e Stamina massimi

```
maxHealth  = maxHealthBase  + maxHealthPerResistance × RESISTANCE + maxHealthPerStamina × STAMINA
maxStamina = maxStaminaBase + maxStaminaPerStamina  × STAMINA
```

| Parametro | Default | Effetto |
|---|---|---|
| `maxHealthBase` | 20 | Vita minima garantita a chiunque, indipendente dalle stat. |
| `maxHealthPerResistance` | 5 | Ogni punto di Resistenza vale 5 HP. |
| `maxHealthPerStamina` | 2 | Ogni punto di Stamina vale 2 HP aggiuntivi. |
| `maxStaminaBase` | 10 | Stamina minima garantita. |
| `maxStaminaPerStamina` | 3 | Ogni punto della caratteristica Stamina vale 3 Stamina massima in combattimento. |

**Incidenza**: determinano quanti turni un combattente può incassare/agire prima di essere sconfitto o esaurito — influenzano indirettamente anche `fatigueMultiplier` (§3), perché quello lavora sul *rapporto* Stamina corrente/massima.

---

## 2. `MomentumWeights` — inerzia psicologica dello scontro

Consumati da `MomentumRules`. Il Momentum è un contatore per combattente che sale/scende in base agli eventi di turno e amplifica (o penalizza) danno/difesa.

| Parametro | Default | Effetto |
|---|---|---|
| `min` / `max` | -100 / 100 | Range in cui il Momentum viene sempre clampato (`clamp`). |
| `effectCap` | 0.15 | Effetto massimo (±15%) che il Momentum può avere sul moltiplicatore di danno/difesa, quando è al valore estremo (±100). |
| `gainOnHitLanded` | +8 | Momentum guadagnato colpendo l'avversario. |
| `gainOnCriticalDealt` | +15 | Momentum guadagnato infliggendo un colpo critico. |
| `gainOnParrySuccess` | +10 | Momentum guadagnato parando con successo. |
| `gainOnDodgeSuccess` | +10 | Momentum guadagnato schivando con successo. |
| `lossOnHitTaken` | -8 | Momentum perso subendo un colpo. |
| `lossOnCriticalTaken` | -15 | Momentum perso subendo un critico. |
| `lossOnMiss` | -5 | Momentum perso mancando un attacco. |

### Come incide nel calcolo

```
normalized      = momentum / max
capped          = clamp(normalized, -1, 1)
effectMultiplier = 1.0 + capped × effectCap
```

Questo moltiplicatore (tra `1 - effectCap` e `1 + effectCap`, cioè **0.85×–1.15× con i default**) si applica sia all'`effectiveOffense` sia all'`effectiveDefense` in `DamageCalculator` (vedi §4): un combattente "in fiducia" (Momentum alto) colpisce più forte e si difende meglio; uno "in crisi" (Momentum negativo) è penalizzato su entrambi i fronti.

**Leve di bilanciamento**: alzare `effectCap` rende gli scontri più "a valanga" (chi inizia bene tende a vincere più nettamente); alzare i `gainOn*`/abbassare i `lossOn*` rende il Momentum più facile da costruire e mantenere.

---

## 3. `StaminaWeights` — affaticamento e gestione delle risorse

Consumati da `StaminaRules`. Regolano il costo delle azioni e la penalità di affaticamento progressivo.

| Parametro | Default | Effetto |
|---|---|---|
| `attackCost` | 6 | Stamina consumata per ogni attacco tentato. |
| `parryCost` | 4 | Stamina consumata per parare. |
| `dodgeCost` | 5 | Stamina consumata per schivare. |
| `impactCost` | 2 | Stamina minima persa incassando un colpo pieno (pavimento, vedi sotto). |
| `impactStaminaDamageFactor` | 0.5 | Frazione del danno subito che si traduce in perdita di Stamina aggiuntiva. |
| `highRatioThreshold` | 0.50 | Sopra questo rapporto Stamina-corrente/massima: nessuna penalità. |
| `lowRatioThreshold` | 0.25 | Sotto questo rapporto: penalità pesante; tra le due soglie: penalità media. |
| `mediumFatiguePenalty` | 0.15 | Riduzione del 15% su attacco/difesa nella fascia intermedia. |
| `heavyFatiguePenalty` | 0.30 | Riduzione del 30% su attacco/difesa sotto la soglia bassa. |
| `restRecovery` | 12 | Stamina recuperata riposando invece di agire. |
| `restThreshold` | 11 | Soglia sotto cui conviene riposare invece di attaccare (copre il costo di un attacco + una difesa). |
| `chainMalusStep` | 2 | Incremento del costo d'attacco per ogni turno concatenato oltre il primo (vedi §6). |
| `chainMalusCap` | 6 | Tetto del malus di catena: il costo d'attacco non cresce oltre `attackCost + chainMalusCap`. |
| `passiveRecovery` | 4 | Stamina recuperata passivamente da chi perde l'iniziativa in un turno (vedi §6). |

### Come incide nel calcolo

Perdita di Stamina da colpo subito (`StaminaRules.impactStaminaLoss`):

```
impactStaminaLoss = max(impactCost, round(damage × impactStaminaDamageFactor))
```

Moltiplicatore di affaticamento (`StaminaRules.fatigueMultiplier`), applicato sia a `effectiveOffense` sia a `effectiveDefense` in `DamageCalculator`:

```
ratio = currentStamina / maxStamina

ratio > highRatioThreshold        → 1.0            (nessuna penalità)
lowRatioThreshold ≤ ratio ≤ high   → 1 - mediumFatiguePenalty   (-15%)
ratio < lowRatioThreshold          → 1 - heavyFatiguePenalty    (-30%)
```

`shouldRest` decide se conviene riposare (`currentStamina <= 0 || currentStamina < restThreshold`), usata dall'orchestratore di turno per decidere l'azione.

**Leve di bilanciamento**: `attackCost`/`parryCost`/`dodgeCost` regolano quanti turni "attivi" un combattente regge; le soglie e le penalità regolano quanto punisce l'affaticamento; `impactStaminaDamageFactor` lega l'usura fisica all'intensità dei colpi subiti, premiando chi incassa poco.

---

## 4. `ChanceWeights` — probabilità di colpire/schivare/parare/critico e fattori di danno

Il gruppo più esteso: consumato da `HitResolver`, `DefenseResolver` e `DamageCalculator`.

### Colpire (`HitResolver`)

```
hitChance = baseHitChance + hitChanceAgilityFactor × (agilitàAttaccante - agilitàDifensore)
hitChance = clamp(hitChance, minHitChance, maxHitChance)
```

| Parametro | Default | Effetto |
|---|---|---|
| `baseHitChance` | 0.75 | Probabilità base di colpire a parità di Agilità. |
| `hitChanceAgilityFactor` | 0.02 | Ogni punto di differenza di Agilità sposta la probabilità del 2%. |
| `minHitChance` / `maxHitChance` | 0.40 / 0.95 | Limiti: nessuno colpisce sempre né mai (tranne il "massimo naturale" del d20, sempre colpo a segno e critico). |

### Critico (`HitResolver`, riusa lo stesso tiro del test di colpire)

```
critChance = clamp(baseCritChance + critChanceLuckFactor × LUCK, minCritChance, maxCritChance)
```

| Parametro | Default | Effetto |
|---|---|---|
| `baseCritChance` | 0.05 | Probabilità base di critico. |
| `critChanceLuckFactor` | 0.01 | Ogni punto di Fortuna aggiunge l'1%. |
| `minCritChance` / `maxCritChance` | 0.05 / 0.40 | Limiti. |

### Schivare (`DefenseResolver`)

```
dodgeChance = baseDodgeChance + dodgeChanceAgilityFactor × (agilitàDifensore - agilitàAttaccante)
dodgeChance = clamp(dodgeChance, minDodgeChance, maxDodgeChance)
```

| Parametro | Default | Effetto |
|---|---|---|
| `baseDodgeChance` | 0.10 | Probabilità base di schivata. |
| `dodgeChanceAgilityFactor` | 0.02 | Ogni punto di differenza di Agilità sposta la probabilità del 2%. |
| `minDodgeChance` / `maxDodgeChance` | 0.02 / 0.50 | Limiti. |

### Parare (`DefenseResolver`)

```
parryChance = baseParryChance + defensiveRating / parryDefenseDivisor
parryChance = clamp(parryChance, minParryChance, maxParryChance)
```

| Parametro | Default | Effetto |
|---|---|---|
| `baseParryChance` | 0.10 | Probabilità base di parata. |
| `parryDefenseDivisor` | 200.0 | Divisore che scala il Defensive Rating in probabilità di parata (più basso = parata più frequente per chi ha Rating alto). |
| `minParryChance` / `maxParryChance` | 0.02 / 0.50 | Limiti. |

Nota: schivata e parata sono valutate sullo stesso tiro (`roll`), con la schivata testata per prima e la parata sul range residuo (`dodgeChance` a `dodgeChance + parryChance`): non sono eventi indipendenti sommati a caso, ma fasce ordinate sulla stessa retta di probabilità.

### Fattori di danno (`DamageCalculator`)

| Parametro | Default | Effetto |
|---|---|---|
| `criticalDamageMultiplier` | 1.75 | Moltiplicatore applicato al danno se il colpo è critico. |
| `parryDamageReduction` | 0.70 | Riduzione del danno finale se il colpo viene parato (70% in meno). |
| `dodgeDamageReduction` | 1.00 | Riduzione del danno se il colpo viene schivato (100%: azzerato). |
| `damageVarianceRange` | 0.10 | Ampiezza della variazione casuale sul danno grezzo (±10%). |

```
variedDamage   = rawDamage × (1 + variance)      variance ∈ [-damageVarianceRange, +damageVarianceRange]
criticalDamage = variedDamage × criticalDamageMultiplier   (se critico)
finalDamage    = criticalDamage × (1 - damageReduction)    (damageReduction da parata/schivata/0 se colpo pieno)
```

**Leve di bilanciamento**: i `base*Chance` fissano il "centro" statistico di ogni tipo di esito; i `*Factor` legano quel centro alla differenza di caratteristiche tra i due combattenti (più il fattore è alto, più le stat contano rispetto al caso); i `min`/`max` impediscono che una differenza di stat estrema renda un esito certo o impossibile; `criticalDamageMultiplier`, `parryDamageReduction`, `dodgeDamageReduction` e `damageVarianceRange` regolano la "spikiness" del danno, cioè quanto gli estremi (critico pieno vs. difesa riuscita) contano rispetto al colpo medio.

---

## 5. `maxTurns` — tetto del duello

| Parametro | Default | Effetto |
|---|---|---|
| `maxTurns` | 30 | Numero massimo di turni oltre il quale lo scontro termina per timeout, deciso ai punti in base al rapporto Health corrente/massima (vedi `CombatEngine.buildTimeoutResult`). |

**Incidenza**: non tocca alcuna formula di danno/probabilità, ma limita quanto a lungo l'effetto cumulativo di Momentum/Stamina/varianza può manifestarsi prima che si decida "ai punti".

---

## 6. `InitiativeWeights` — iniziativa ricalcolata a fine turno

Consumati da `InitiativeResolver`. A differenza del modello v1 (attaccante e difensore che si alternavano con uno swap fisso), il **prossimo attaccante è ricalcolato alla fine di ogni turno**, primo turno incluso. "Iniziativa" significa "chi attacca il turno successivo": il difensore resta reattivo e si difende. Il motore dell'iniziativa è il **rapporto Stamina corrente/massima**, così chi ha appena attaccato molto (e si è stancato) tende a cedere il tempo a chi ha subìto e recuperato.

```
initiativeScore = wStamina      × (staminaCorrente / staminaMax)
                 + wAgility       × AGILITY
                 + wIntelligence  × INTELLIGENCE
                 + wJitter         × tiroJitter          (dado a jitterDiceFaces facce)
```

| Parametro | Default | Effetto |
|---|---|---|
| `wStamina` | 25.0 | Peso del **rapporto** Stamina (0–1) sull'iniziativa: è il motore dominante. Usare il rapporto (non il valore assoluto) rende equi i confronti tra build con `maxStamina` diverse. |
| `wAgility` | 1.0 | Peso dell'Agilità: differenza statica tra i due combattenti, permette a chi è più agile di concatenare qualche attacco nonostante il calo di Stamina. |
| `wIntelligence` | 0.5 | Peso dell'Intelligenza (lettura del tempo): incide meno dell'Agilità. |
| `wJitter` | 0.5 | Peso del micro-jitter: piccolo contributo casuale il cui solo scopo è **rompere pareggi e simmetrie**, non far decidere il caso lo scontro. |
| `jitterDiceFaces` | 6 | Numero di facce del dado del jitter (`1`…`jitterDiceFaces`). Tarabile: pilota realmente il dado lanciato dallo shell. |

**Come si tara**: `wStamina` deve dominare senza schiacciare `wAgility`/`wIntelligence`, altrimenti nessuno riuscirebbe mai a concatenare attacchi (con i default, ~0.4 di divario di rapporto Stamina equivale a ~10 punti di Agilità di scarto). Alzare `wJitter`/`jitterDiceFaces` dà più peso al caso sui pareggi; abbassarli rende l'ordine più deterministico.

### Meccaniche di tempo collegate (parametri in §3 `StaminaWeights`)

- **Malus di catena** (`chainMalusStep`, `chainMalusCap`): il costo effettivo dell'attacco cresce con i turni concatenati, `effectiveAttackCost = attackCost + min(chainMalusCap, chainMalusStep × (turniConsecutivi − 1))`. Con i default: 6 → 8 → 10 → 12 (cap). Il contatore si azzera quando il combattente **perde l'iniziativa** oppure **riposa**. Scoraggia il concatenamento infinito senza vietarlo.
- **Recupero passivo** (`passiveRecovery`): chi perde l'iniziativa recupera un po' di Stamina nel turno, attutendo l'usura senza annullare i costi di difesa (para/schiva pagando comunque). È ciò che trasforma il sistema in un **pendolo** invece di una spirale: senza, chi resta indietro non recupererebbe mai.
- **Gate di affordabilità**: nessuna azione parte se non interamente pagabile (la Stamina non va mai sotto zero). Attacco non pagabile → si riposa; schivata non pagabile → ripiego sulla parata se pagabile, altrimenti colpo pieno.
- **Furto del tempo con la schivata**: una schivata riuscita assegna deterministicamente l'iniziativa del turno successivo allo schivatore (override della formula). La **parata** non ha questo effetto. Nota di bilanciamento: poiché la schivata cumula ora tre vantaggi (azzera il danno + dà Momentum + ruba il tempo), `dodgeCost` (§3) è il primo candidato a un rialzo in fase di taratura.

**Nota**: tutti i default nuovi (`InitiativeWeights`, `chainMalusStep`, `chainMalusCap`, `passiveRecovery`) sono **taratura empirica provvisoria**.

---

## Come modificare il bilanciamento in pratica

Tutti i valori sopra sono definiti nei metodi `defaults()` dei rispettivi record, dentro `CombatSettings.java`. Per pilotare il bilanciamento:

1. Modificare i valori nei `defaults()` (impatto globale, changelog centralizzato in un unico file), **oppure**
2. Costruire un `CombatSettings` alternativo (nuovi valori passati esplicitamente al costruttore dei record) e iniettarlo al posto di `CombatSettings.defaults()` nel punto in cui viene creato `Arena`/`CombatEngine` — utile per A/B testing o profili di difficoltà diversi, senza toccare i default.

Nessuna di queste modifiche richiede toccare i resolver (`HitResolver`, `DefenseResolver`, `DamageCalculator`, `MomentumRules`, `StaminaRules`, `DefaultRatingStrategy`): sono formule pure che leggono solo da `CombatSettings`.
