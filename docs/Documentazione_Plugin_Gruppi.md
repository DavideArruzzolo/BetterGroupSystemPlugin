# Documentazione Completa Plugin Gruppi: Analisi, Specifiche e Test

## 1. Analisi Configurazione e Gap Tecnici

Questa sezione evidenzia le proprietà mancanti, i rischi di sicurezza e le logiche da implementare prima o durante lo
sviluppo, basandosi sull'analisi del codice attuale.

### 1.1 Proprietà di Configurazione Mancanti o Consigliate

Le seguenti proprietà sono attualmente *hardcoded* o logiche necessarie ma assenti nel config:

| Proprietà Suggerita           | Valore Default  | Utilizzo                                                                                   |
|:------------------------------|:----------------|:-------------------------------------------------------------------------------------------|
| `maxCustomRoles`              | `10`            | Limite massimo di ruoli personalizzati (ottimizzazione memoria).                           |
| `teleportDelay`               | `5` (secondi)   | Tempo di attesa prima del teletrasporto home (anti-combat escape).                         |
| `allowCrossDimensionTeleport` | `false`         | Permesso di TP alla home se il player è in un'altra dimensione (Nether/End).               |
| `homeLimitScaling`            | `false`         | Se `true`, il limite home aumenta col livello della Gilda (attualmente fisso a `maxHome`). |
| `inviteExpiration`            | `300` (secondi) | Tempo di scadenza automatico degli inviti (richiede task schedulato).                      |

### 1.2 Check di Sicurezza e Logici Aggiuntivi (Critici)

* **A. Combat Logging:**
    * **Stato:** Placeholder commentato nel codice.
    * **Necessità:** Implementare `isInCombat(player)` prima di comandi come `/group home` o `/group accept`. Richiede
      `CombatManager` esterno.
* **B. Economia Reale:**
    * **Stato:** `deposit` e `withdraw` gestiscono solo il bilancio interno (`group.bankBalance`).
    * **Necessità:** Integrazione con `EconomyService` per prelevare/depositare dal wallet reale del giocatore.
* **C. Chunk Loading:**
    * **Rischio:** Operazioni su territori con chunk scaricati.
    * **Consiglio:** Assicurare che le operazioni siano Thread-Safe rispetto al caricamento del mondo Hytale.
* **D. Offline Players:**
    * **Problema:** I comandi (es. `kick`) usano `PlayerRef` che spesso implica giocatori online.
    * **Soluzione:** Modificare le firme per accettare `String targetName` e risolvere l'UUID dal database offline.

---

## 2. Specifiche di Validazione Comandi (Logica e Codice)

Questa sezione dettaglia ogni controllo che **deve** essere superato affinché un comando venga eseguito. Include sia la
logica funzionale che i riferimenti all'implementazione (`GroupService.java`).

### 2.1 Controlli Generali (Pre-requisiti)

Applicabili alla maggior parte dei comandi prima della logica specifica.

1. **Sender is Player:** Il comando non può essere eseguito da console.
2. **Player in Group:** Verifica esistenza e appartenenza (`playerGroupMap.containsKey(uuid)`).
3. **Data Integrity:** Il gruppo recuperato non è nullo (`getGroup(uuid) != null`).
4. **Permissions:** Verifica nodo permessi base o ruolo (`checkPerm(perm)`).

---

### 2.2 Gestione del Gruppo (Management)

#### `/group create <name> <tag> [color] [desc]`

* **Stato Giocatore:**
    * Deve essere **senza** gruppo (`!inGroup`).
* **Validazione Argomenti:**
    * **Nome/Tag:** Presenti, alfanumerici, lunghezza entro limiti config (`validateName/Tag`).
    * **Unicità:** Nome e Tag non devono esistere nella Cache Globale.
    * **Colore:** Se fornito, deve essere un codice Hex valido (regex check).
    * **Descrizione:** Non vuota e lunghezza entro limiti config.

#### `/group info [name]`

* **Se nome fornito:** Verifica se esiste un gruppo con quel nome.
* **Se omesso:** Verifica se il giocatore è in un gruppo.

#### `/group update [name/tag/color/desc]`

* **Permessi:** Il ruolo deve avere `CAN_UPDATE_GROUP`.
* **Validazione:** Stessi controlli di `create` (unicità, regex, lunghezza) per il campo specifico che si sta
  modificando.

#### `/group delete`

* **Permessi:** Solo il **Leader** può eseguire (`isLeader`).
* **Sicurezza:** Richiede conferma esplicita (`confirmed` flag / comando confirm).

#### `/group leave`

* **Logica Leader:**
    * Se il leader è l'ultimo membro (`members == 1`), il gruppo viene cancellato.
    * Se ci sono altri membri, l'azione è bloccata: il leader deve prima fare `/group transfer`.

---

### 2.3 Gestione dei Membri

#### `/group invite <name>`

* **Permessi:** Ruolo con `CAN_INVITE`.
* **Stato Target:**
    * Target deve essere online (o risolvibile offline vedi gap 1.2.D).
    * Target **non** deve essere in un altro gruppo (`!targetInGroup`).
* **Stato Gruppo:** Numero membri attuale < `maxSize` (Config).

#### `/group invitations` / `/listinvitations`

* **Stato:** Verifica se il giocatore ha inviti pendenti.

#### `/group accept <name>`

* **Stato Giocatore:** Non deve essere in un gruppo (`!inGroup`).
* **Validazione Invito:**
    * Deve esistere un invito pendente per quel gruppo (`hasInvitation`).
    * Verifica scadenza invito (se implementata).
* **Stato Gruppo:** Verifica finale che il gruppo non sia pieno (`size < maxSize`).

#### `/group kick <name>`

* **Permessi:** Ruolo con `CAN_KICK`.
* **Validazione Target:**
    * Target deve essere nel gruppo (`targetInGroup`).
    * Non può essere se stessi (`!self`).
* **Gerarchia:** Priorità ruolo sender > Priorità ruolo target (Impossibile espellere pari grado o superiori).

#### `/group transfer <name>`

* **Permessi:** Solo il **Leader** attuale (`isLeader`).
* **Validazione Target:** Il nuovo leader deve essere già membro del gruppo.
* **Sicurezza:** Richiede conferma.

---

### 2.4 Gestione dei Ruoli

#### `/group role create <name> [grants]`

* **Permessi:** Ruolo con `CAN_MANAGE_ROLE`.
* **Limiti:** Numero ruoli personalizzati < 10 (Hardcoded/Config `maxCustomRoles`).
* **Validazione:** Nome ruolo univoco nel gruppo (`nameUnique`).
* **Grants:** Se forniti, devono essere stringhe di permesso valide.

#### `/group role update <name> [grants]`

* **Permessi:** Ruolo con `CAN_MANAGE_ROLE`.
* **Validazione:** Il ruolo deve esistere e non essere protetto (se logica applicabile).

#### `/group role delete <name>`

* **Permessi:** Ruolo con `CAN_MANAGE_ROLE`.
* **Protezione:**
    * Non si possono cancellare ruoli di default (`!isDefault`).
    * Nessun membro deve avere quel ruolo assegnato al momento (`!isInUse`).

#### `/group set_role` / `promote` / `demote`

* **Permessi:** Ruolo con `CAN_CHANGE_ROLE`.
* **Gerarchia:**
    * Sender deve essere superiore al target.
    * Non si può promuovere un target a un grado >= al proprio (salvo Leader).

---

### 2.5 Gestione Territori e Home

#### `/group sethome <name>`

* **Permessi:** Ruolo con `CAN_MANAGE_HOME`.
* **Posizione:** Il giocatore deve trovarsi in un chunk rivendicato dal gruppo (`inClaimedChunk`).
* **Limiti:** Numero homes < `maxHomes` (Config).
* **Validazione:** Nome home univoco (o sovrascrittura se specificato).

#### `/group home [name]`

* **Validazione:** La home deve esistere.
* **Combat:** Check `isInCombat` (vedi Gap 1.2.A).

#### `/group claim`

* **Permessi:** Ruolo con `CAN_MANAGE_CLAIM`.
* **Disponibilità:** Il chunk non deve essere rivendicato da **nessun** altro gruppo (`!globalClaimed`).
* **Requisiti:**
    * *Faction:* Potere sufficiente (`ClaimRatio`).
    * *Guild:* Limite numerico claim non raggiunto.
    * *Economia:* Fondi sufficienti in banca (se costo attivo).

#### `/group unclaim`

* **Permessi:** Ruolo con `CAN_MANAGE_CLAIM`.
* **Proprietà:** Il chunk deve appartenere al gruppo del sender.

---

### 2.6 Gestione Economia

#### `/group deposit <quantity>`

* **Input:** Quantità numerica positiva valida.
* **Economia Player:** Il giocatore deve avere fondi sufficienti nel proprio wallet (Vedi Gap 1.2.B).

#### `/group withdraw <quantity>`

* **Permessi:** Ruolo con `CAN_MANAGE_BANK`.
* **Input:** Quantità numerica positiva valida (`amount > 0`).
* **Economia Gruppo:** Saldo banca del gruppo sufficiente (`balance >= amount`).

#### `/group upgrade`

* **Permessi:** Permesso specifico upgrade.
* **Validazione:**
    * Solo modalità Guild (`isGuild`).
    * Livello attuale < Max Level (`!maxLevel`).
* **Costo:** Saldo banca sufficiente per il costo calcolato.

---

### 2.7 Gestione Diplomazia

#### `/group diplomacy <target_group> <status>`

* **Permessi:** Ruolo con gestione diplomazia.
* **Validazione:**
    * Target group esiste e non è il proprio gruppo.
    * Status valido (`ALLY`, `NEUTRAL`, `ENEMY`).
* **Logica:**
    * Se `ALLY`/`NEUTRAL`, invia richiesta (l'altro deve accettare).
    * Verifica se lo stato è già attivo.

---

## 3. Piano di Test (Test Plan)

Casi di test ordinati per area funzionale e complessità.

### 3.1 Test Funzionali di Base ("Happy Path")

#### Gestione del Gruppo

| Giocatori   | Comando               | Azione da Testare                                          |
|:------------|:----------------------|:-----------------------------------------------------------|
| 1 (Online)  | `/creategroup <nome>` | Creare un nuovo gruppo.                                    |
| 1 (Online)  | `/update`             | Modificare le impostazioni (descrizione, tag).             |
| 1 (Online)  | `/upgrade`            | Eseguire l'upgrade del gruppo.                             |
| 1 (Online)  | `/info <gruppo>`      | Visualizzare le info del proprio gruppo.                   |
| 1 (Online)  | `/delete`             | Cancellare il proprio gruppo.                              |
| 1 (Online)  | `/leave`              | Lasciare un gruppo come unico membro (cancella il gruppo). |
| 2+ (Online) | `/leave`              | Membro non-proprietario lascia il gruppo.                  |
| 2+ (Online) | `/info <gruppo>`      | Esterno visualizza info di un gruppo esistente.            |

#### Gestione dei Membri

| Giocatori  | Comando               | Azione da Testare                     |
|:-----------|:----------------------|:--------------------------------------|
| 1 (Online) | `/listinvitations`    | Lista vuota.                          |
| 2+         | `/invite <giocatore>` | P1 invita P2 (online).                |
| 2+         | `/invite <giocatore>` | P1 invita P2 (offline).               |
| 2+         | `/accept <gruppo>`    | P2 accetta invito.                    |
| 2+         | `/kick <membro>`      | Owner espelle membro online.          |
| 2+         | `/kick <membro>`      | Owner espelle membro offline.         |
| 2+         | `/transfer <membro>`  | Owner trasferisce proprietà a membro. |
| 2+         | `/listinvitations`    | P2 visualizza invito ricevuto.        |

#### Gestione dei Ruoli

| Giocatori   | Comando                      | Azione da Testare             |
|:------------|:-----------------------------|:------------------------------|
| 1 (Online)  | `/create_role <nome>`        | Creare nuovo ruolo.           |
| 1 (Online)  | `/list_roles`                | Visualizzare lista ruoli.     |
| 1 (Online)  | `/delete_role <ruolo>`       | Cancellare ruolo esistente.   |
| 2+ (Online) | `/set_role <membro> <ruolo>` | Owner assegna ruolo a membro. |

#### Gestione Territori e Home

| Giocatori  | Comando    | Azione da Testare                 |
|:-----------|:-----------|:----------------------------------|
| 1 (Online) | `/claim`   | Rivendicare territorio.           |
| 1 (Online) | `/unclaim` | Rimuovere rivendicazione.         |
| 1 (Online) | `/sethome` | Impostare home posizione attuale. |
| 1 (Online) | `/home`    | Teletrasporto alla home.          |
| 1 (Online) | `/delhome` | Cancellare home.                  |

#### Gestione Economia

| Giocatori   | Comando                | Azione da Testare                             |
|:------------|:-----------------------|:----------------------------------------------|
| 1 (Online)  | `/getbalance`          | Check saldo personale.                        |
| 1 (Online)  | `/deposit <quantità>`  | Deposito in banca gruppo.                     |
| 1 (Online)  | `/getgroupbalance`     | Check saldo gruppo.                           |
| 1 (Online)  | `/withdraw <quantità>` | Prelievo (Owner).                             |
| 2+ (Online) | `/withdraw <quantità>` | Tentativo prelievo membro (testare permessi). |

#### Gestione Diplomazia

| Giocatori | Comando                           | Azione da Testare                 |
|:----------|:----------------------------------|:----------------------------------|
| 2 Gruppi  | `/diplomacy <gruppo> <relazione>` | Impostare relazione (es. `ALLY`). |
| 2 Gruppi  | `/listdiplomacy`                  | Visualizzare relazioni.           |

---

### 3.2 Test Avanzati (Negativi e Casi Limite)

#### Gestione del Gruppo

| Priorità | Comando        | Scenario Negativo/Limite                         |
|:---------|:---------------|:-------------------------------------------------|
| **Alta** | `/creategroup` | Nome già esistente.                              |
| **Alta** | `/creategroup` | Giocatore già in un gruppo.                      |
| Media    | `/creategroup` | Caratteri speciali, spazi, lunghezza non valida. |
| Media    | `/info`        | Gruppo inesistente.                              |

#### Gestione dei Membri

| Priorità | Comando     | Scenario Negativo/Limite                       |
|:---------|:------------|:-----------------------------------------------|
| **Alta** | `/invite`   | Membro senza permessi invita qualcuno.         |
| **Alta** | `/invite`   | Gruppo pieno (max capienza).                   |
| **Alta** | `/invite`   | Target già nel gruppo.                         |
| **Alta** | `/kick`     | Membro senza permessi espelle Owner.           |
| **Alta** | `/transfer` | Membro senza permessi prova a diventare Owner. |
| Media    | `/accept`   | Gruppo inesistente o invito ritirato.          |
| Media    | `/invite`   | Target inesistente.                            |

#### Gestione dei Ruoli

| Priorità | Comando        | Scenario Negativo/Limite                           |
|:---------|:---------------|:---------------------------------------------------|
| **Alta** | `/create_role` | Membro senza permessi crea ruolo.                  |
| **Alta** | `/create_role` | Nome ruolo già esistente.                          |
| **Alta** | `/set_role`    | Membro senza permessi assegna ruolo a Owner.       |
| Media    | `/delete_role` | Cancellare ruolo predefinito o assegnato a membri. |

#### Gestione Territori ed Economia

| Priorità | Comando                | Scenario Negativo/Limite                 |
|:---------|:-----------------------|:-----------------------------------------|
| **Alta** | `/claim`               | Territorio già occupato da altro gruppo. |
| **Alta** | `/claim`               | Limite territori superato.               |
| **Alta** | `/unclaim`             | Membro senza permessi annulla claim.     |
| **Alta** | `/deposit`/`/withdraw` | Importi negativi, zero, non numerici.    |
| **Alta** | `/withdraw`            | Prelievo superiore al saldo disponibile. |
| **Alta** | `/withdraw`            | Membro senza permessi preleva.           |

---

### 3.3 Test di Regressione

Da eseguire dopo ogni modifica significativa al codice per garantire la stabilità.

1. **Ciclo Vita Gruppo:** Eseguire `/creategroup` seguito da `/delete`.
2. **Ciclo Vita Membri:** Eseguire `/invite`, far accettare con `/accept`, e poi `/kick`.
3. **Ciclo Vita Territorio:** Eseguire `/claim` seguito da `/unclaim`.
4. **Ciclo Vita Economia:** Eseguire `/deposit` e `/withdraw` (come owner).
5. **Permessi:** Verifica fallimento comandi critici (`kick`, `unclaim`, `withdraw`) da parte di membro senza permessi.