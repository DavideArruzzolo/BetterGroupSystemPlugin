# Documentazione Tecnica: BetterGroupSystemPlugin

## 1. Executive Summary

Questo documento fornisce una specifica tecnica completa per il **BetterGroupSystemPlugin**, un sistema modulare per
server Hytale progettato per gestire Fazioni (PvP/potere) e Gilde (progressione/economia). Il plugin è costruito su *
*Java 25**, sfruttando le moderne feature della JVM come il Project Loom per l'elaborazione asincrona ad alte
prestazioni.

L'architettura è basata su un design pattern Singleton per i manager principali, garantendo un unico punto di accesso e
controllo per i sottosistemi. La persistenza dei dati è gestita tramite un sistema basato su file **JSON**, con un
logging strutturato per tutti gli eventi critici. L'interfaccia utente supporta sia comandi testuali tradizionali che
un'interfaccia grafica (GUI) configurabile.

Le funzionalità principali includono:

- **Gestione Gruppi**: Creazione, modifica, scioglimento e gestione dei membri.
- **Sistema di Territori**: Claim di chunk, gestione delle home e mappe territoriali.
- **Economia**: Banche di gruppo, tasse automatiche e upgrade di livello.
- **Diplomazia**: Gestione di alleanze e inimicizie tra gruppi.
- **Ruoli e Permessi**: Un sistema granulare per definire ruoli personalizzati con permessi specifici.

Questo documento è destinato a sviluppatori, architetti e stakeholder tecnici per garantire una comprensione completa
del sistema, facilitare la manutenzione e guidare gli sviluppi futuri.

## 2. Introduzione

### 2.1. Contesto

Il progetto nasce dall'esigenza di fornire ai server Hytale un sistema di gestione gruppi flessibile e performante, in
grado di adattarsi a diverse modalità di gioco (PvP, PvE, Role-playing). I sistemi esistenti spesso mancano di
modularità o presentano limiti di scalabilità.

### 2.2. Obiettivi

- **Modularità**: Creare un sistema dove le modalità Fazione e Gilda possano coesistere o essere attivate singolarmente.
- **Prestazioni**: Garantire tempi di risposta rapidi e un basso impatto sulle performance del server, anche con un
  numero elevato di giocatori e gruppi.
- **Configurabilità**: Offrire un'ampia gamma di parametri di configurazione per personalizzare ogni aspetto del gioco.
- **Usabilità**: Fornire un'interfaccia intuitiva sia per i giocatori (comandi e GUI) che per gli amministratori.
- **Manutenibilità**: Scrivere codice pulito, ben documentato e strutturato per facilitare futuri aggiornamenti.

### 2.3. Scope del Progetto

Lo scope del progetto include:

- Lo sviluppo del core del plugin con i sistemi di Fazione e Gilda.
- La creazione di un sistema di persistenza dati basato su JSON.
- L'implementazione di un sistema completo di comandi e GUI.
- La documentazione di tutte le API interne e dei punti di configurazione.

Non rientrano nello scope:

- Lo sviluppo di un'interfaccia web.
- L'integrazione con database SQL (prevista come possibile estensione futura).

## 3. Architettura del Sistema

Questa sezione descrive l'architettura generale del plugin, i pattern di progettazione utilizzati e i componenti
principali.

### 3.1. Diagramma Architetturale (Alto Livello)

```
+-------------------------+      +-------------------------+      +-------------------------+
|      Command System     |      |        GUI System       |      |      Event Listener     |
+-------------------------+      +-------------------------+      +-------------------------+
             |                              |                              |
             v                              v                              v
+--------------------------------------------------------------------------------------+
|                                     Core Logic                                       |
|                                                                                      |
|  +-----------------+   +-----------------+   +-----------------+   +-----------------+  |
|  |  Group Manager  |   |  Player Manager |   |  Claim Manager  |   |  Config Manager |  |
|  +-----------------+   +-----------------+   +-----------------+   +-----------------+  |
|                                                                                      |
+--------------------------------------------------------------------------------------+
             |                                          ^
             v                                          |
+--------------------------------------------------------------------------------------+
|                                  Data Persistence                                    |
|                                                                                      |
|  +-----------------------------+      +-------------------------------------------+  |
|  |      JSON Data Storage      |      |              Logging System               |  |
|  +-----------------------------+      +-------------------------------------------+  |
|                                                                                      |
+--------------------------------------------------------------------------------------+
```

**Descrizione del Diagramma:**

- **Command System / GUI System / Event Listener**: Punti di ingresso per le interazioni dell'utente e gli eventi del
  server.
- **Core Logic**: Contiene la logica di business principale, orchestrata dai manager.
- **Managers (Singleton)**:
    - `GroupManager`: Gestisce la creazione, modifica e cancellazione dei gruppi.
    - `PlayerManager`: Gestisce i dati dei giocatori e le loro interazioni con i gruppi.
    - `ClaimManager`: Gestisce la logica di claim e protezione dei territori.
    - `ConfigManager`: Carica e fornisce accesso ai parametri di configurazione (`config.yml`).
- **Data Persistence**:
    - `JSON Data Storage`: Memorizza lo stato di gruppi, giocatori e territori su file JSON.
    - `Logging System`: Registra eventi importanti per il debug e il monitoraggio.

### 3.2. Design Pattern Utilizzati

- **Singleton**: I manager principali (`GroupManager`, `PlayerManager`, etc.) sono implementati come Singleton per
  garantire un'unica istanza globale, centralizzando la gestione dello stato.
- **Factory**: Utilizzato per creare oggetti complessi come i gruppi (`Group`), astraendo la logica di creazione a
  seconda del tipo (Fazione o Gilda).
- **Observer**: Il sistema di eventi di Hytale è intrinsecamente basato su un pattern Observer, a cui il plugin si
  aggancia tramite `EventListener` per reagire a eventi di gioco (es. morte del giocatore, interazione con blocchi).
- **Data Transfer Object (DTO)**: Le classi come `GroupMember`, `GroupHome` sono DTO semplici che trasferiscono dati tra
  i layer dell'applicazione senza contenere logica di business.

### 3.3. Componenti Principali

- **Core Plugin Class**: La classe principale che inizializza tutti i manager, registra i comandi e gli eventi all'avvio
  del server.
- **Command Handlers**: Classi dedicate a gestire la logica di esecuzione di ogni comando.
- **GUI Menus**: Classi che costruiscono e gestiscono le interfacce grafiche.
- **Data Models**: Le classi che rappresentano le entità del sistema (es. `Group`, `Player`, `Role`).
- **Storage Engine**: Il componente responsabile della serializzazione e deserializzazione dei dati da/verso i file
  JSON.

## 4. Specifiche Tecniche Dettagliate

### 4.1. Parametri di Configurazione (config.yml)

| Parametro                  | Tipo         | Descrizione                                                     |
|----------------------------|--------------|-----------------------------------------------------------------|
| `pluginMode`               | String       | Modalità attiva: `FACTION`, `GUILD`, `BOTH`.                    |
| `allowedWorlds`            | List<String> | ID dei mondi in cui il claim è permesso.                        |
| `enableGui`                | Boolean      | Abilita/disabilita l'interfaccia grafica.                       |
| `allCommandsPrefix`        | String       | Prefisso globale per tutti i comandi del plugin.                |
| `hidePlayers`              | Boolean      | Nasconde i giocatori esterni sulla mappa.                       |
| `toasterDuration`          | Integer      | Durata (secondi) delle notifiche a schermo.                     |
| `guiThemeColor`            | String       | Colore esadecimale per la GUI.                                  |
| `maxSize`                  | Integer      | Numero massimo di membri in una fazione.                        |
| `minNameLength`            | Integer      | Lunghezza minima per il nome della fazione.                     |
| `maxNameLength`            | Integer      | Lunghezza massima per il nome della fazione.                    |
| `minTagLength`             | Integer      | Lunghezza minima per il tag della fazione.                      |
| `maxTagLength`             | Integer      | Lunghezza massima per il tag della fazione.                     |
| `playerPowerMax`           | Double       | Power massimo per un giocatore.                                 |
| `playerPowerMin`           | Double       | Power minimo per un giocatore.                                  |
| `powerGainByKill`          | Double       | Power guadagnato per uccisione.                                 |
| `powerGainByTime`          | Double       | Power guadagnato passivamente online.                           |
| `powerLooseByDeath`        | Double       | Power perso alla morte.                                         |
| `powerRegenOffline`        | Boolean      | Abilita la rigenerazione del power offline.                     |
| `claimRatio`               | Double       | Moltiplicatore per il limite di chunk basato sul power.         |
| `maxClaimsPerFaction`      | Integer      | Limite massimo di chunk claimabili.                             |
| `unraidableProtectionTime` | Integer      | Minuti di protezione dopo aver perso lo stato "raidable".       |
| `guildLevels`              | List<Object> | Gerarchia dei livelli della gilda.                              |
| `levelPriceMultiplier`     | Double       | Costo per l'upgrade di livello.                                 |
| `slotQuantityForLevel`     | Integer      | Slot per membri sbloccati per livello.                          |
| `enableTax`                | Boolean      | Abilita il sistema di tasse.                                    |
| `taxImport`                | Double       | Ammontare della tassa.                                          |
| `taxInterval`              | String       | Intervallo di riscossione delle tasse (es. "24h").              |
| `maxLatePayment`           | Integer      | Numero massimo di pagamenti mancati prima delle penalità.       |
| `latePayAction`            | String       | Azione per mancato pagamento: `KICK`, `DEMOTE`, `DISABLE_BUFF`. |
| `latePayGracePeriod`       | Integer      | Ore di grazia prima della penalità.                             |
| `maxHome`                  | Integer      | Numero massimo di home per un gruppo.                           |
| `defaultRolesName`         | List<String> | Nomi dei ruoli di default.                                      |
| `defaultGrants`            | List<String> | Permessi iniziali per i ruoli di default.                       |
| `rolePriority`             | Integer      | Gerarchia numerica per la gestione dei permessi.                |

### 4.2. Modelli di Dati (Data Models)

#### 4.2.1. Group.java

| Campo                 | Tipo                       | Descrizione                         |
|-----------------------|----------------------------|-------------------------------------|
| `id`                  | UUID                       | Identificatore unico del gruppo.    |
| `type`                | GroupType (Enum)           | `FACTION` o `GUILD`.                |
| `name`                | String                     | Nome unico, case-insensitive.       |
| `tag`                 | String                     | Tag unico, case-insensitive.        |
| `description`         | String                     | Descrizione opzionale.              |
| `color`               | String                     | Colore esadecimale per chat e GUI.  |
| `leaderId`            | UUID                       | UUID del leader del gruppo.         |
| `level`               | int                        | Livello corrente del gruppo.        |
| `bankBalance`         | double                     | Saldo della banca del gruppo.       |
| `members`             | Set<GroupMember>           | Insieme dei membri del gruppo.      |
| `roles`               | Set<GroupRole>             | Insieme dei ruoli disponibili.      |
| `homes`               | Set<GroupHome>             | Insieme delle home del gruppo.      |
| `claims`              | Set<GroupClaimedChunk>     | Insieme dei chunk claimati.         |
| `diplomaticRelations` | Map<UUID, DiplomacyStatus> | Mappa delle relazioni diplomatiche. |
| `createdAt`           | LocalDateTime              | Timestamp di creazione del gruppo.  |

#### 4.2.2. GroupMember.java

| Campo      | Tipo          | Descrizione                       |
|------------|---------------|-----------------------------------|
| `playerId` | UUID          | ID unico del giocatore.           |
| `roleId`   | UUID          | ID del ruolo assegnato al membro. |
| `joinDate` | LocalDateTime | Timestamp di ingresso nel gruppo. |

#### 4.2.3. GroupRole.java

| Campo      | Tipo        | Descrizione                              |
|------------|-------------|------------------------------------------|
| `roleId`   | UUID        | ID unico del ruolo.                      |
| `roleName` | String      | Nome del ruolo.                          |
| `priority` | int         | Priorità per la gerarchia dei permessi.  |
| `grants`   | Set<String> | Insieme dei permessi (es. `CAN_INVITE`). |

## 5. Requisiti

### 5.1. Requisiti Funzionali

- **RF-01**: Il sistema deve permettere la creazione di un gruppo (Fazione/Gilda) con nome e tag unici.
- **RF-02**: Il sistema deve permettere di invitare, espellere e promuovere/retrocedere membri.
- **RF-03**: Il sistema deve permettere ai giocatori di accettare/rifiutare inviti e di lasciare un gruppo.
- **RF-04**: Il sistema deve permettere di impostare, modificare e cancellare "home" per il teletrasporto.
- **RF-05**: Il sistema deve permettere di claimare e unclaimare chunk di mappa.
- **RF-06**: Il sistema deve fornire una mappa visuale dei territori claimati.
- **RF-07**: Il sistema deve gestire una banca di gruppo con funzioni di deposito e prelievo.
- **RF-08**: Il sistema deve supportare un sistema di diplomazia (Alleato, Neutrale, Nemico).
- **RF-09**: Il sistema deve permettere la creazione e gestione di ruoli con permessi granulari.
- **RF-10**: Il sistema deve avere una modalità GUI per tutte le funzionalità principali.

### 5.2. Requisiti Non Funzionali

- **RNF-01**: Le operazioni di lettura dei dati (es. `/group info`) devono avere un tempo di risposta inferiore a 50ms.
- **RNF-02**: Le operazioni di scrittura dei dati (es. `/group create`) devono essere completate entro 200ms.
- **RNF-03**: Il plugin non deve aumentare l'utilizzo della CPU del server di oltre il 5% in condizioni di carico
  normale.
- **RNF-04**: I dati devono essere persistiti in modo sicuro per prevenire la corruzione in caso di crash del server.
- **RNF-05**: Il sistema deve essere compatibile con Java 25.
- **RNF-06**: Il codice deve essere offuscato (tramite Proguard) per proteggere la proprietà intellettuale.

## 6. Design e Implementazione

### 6.1. Flusso di Inizializzazione

1. **Caricamento del Plugin**: Il server Hytale carica la classe `BetterGroupSystemPlugin`.
2. **Costruttore**: Nel costruttore, viene inizializzata la configurazione (`config.yml`) tramite `withConfig`.
3. **Metodo `setup()`**:
    * Viene inizializzato il `GroupService` come Singleton, passandogli la configurazione caricata.
    * Viene istanziato `BaseGroupCommand`, che a sua volta registra tutti i sottocomandi.
    * Il comando base viene registrato nel `CommandRegistry` di Hytale.
4. **Caricamento Dati**: All'interno del costruttore di `GroupService`, il `JsonStorage` viene inizializzato e il metodo
   `loadGroups()` viene chiamato per caricare tutti i dati dei gruppi dal file JSON in memoria. Vengono popolate le
   cache per nomi, tag e appartenenza dei giocatori.

### 6.2. Logica di Business (`GroupService`)

Il `GroupService` è il componente centrale che orchestra tutta la logica di business. Le sue responsabilità includono:

- **Gestione Dati in Memoria**: Mantiene mappe concorrenti (`ConcurrentHashMap`) per gruppi, appartenenza dei giocatori
  e inviti, garantendo la thread-safety.
- **Operazioni CRUD**: Fornisce metodi per creare, leggere, aggiornare e cancellare gruppi e i loro sottocomponenti (
  membri, ruoli, home).
- **Controllo dei Permessi**: Ogni azione che modifica lo stato è protetta da un controllo dei permessi (`hasPerm`).
  Questo metodo verifica se il ruolo del giocatore possiede il permesso necessario per eseguire l'azione.
- **Validazione dell'Input**: L'input dell'utente (es. nomi, tag) viene normalizzato e validato tramite espressioni
  regolari e controlli di unicità per garantire la consistenza dei dati.
- **Salvataggio Asincrono**: Le operazioni di salvataggio (`saveGroups`) vengono eseguite in modo asincrono tramite
  `JsonStorage` per non bloccare il thread principale del server.

### 6.3. Struttura dei Comandi

- **`BaseGroupCommand`**: Funge da comando radice (es. `/group`). Non contiene logica di business diretta, ma agisce
  come un router.
- **Sottocomandi**: Ogni funzionalità (es. `create`, `invite`, `claim`) è implementata come un sottocomando separato,
  spesso in classi dedicate. Questo design promuove la separazione delle responsabilità e rende il codice più
  manutenibile.
- **Invocazione del Servizio**: I gestori dei comandi estraggono l'input dell'utente e invocano i metodi appropriati sul
  `GroupService` per eseguire la logica di business.

## 7. Interfacce e Integrazioni

### 7.1. Interfaccia a Riga di Comando (CLI)

L'interfaccia principale per l'utente è la CLI. La tabella completa dei comandi è disponibile nella sezione 4 del
`Readme.md`.

### 7.2. Sistema di Permessi (Grants)

Il sistema di permessi è definito dall'enum `Permission`. Ogni permesso è una stringa (es. `CAN_INVITE`) che può essere
assegnata a un `GroupRole`. Questo permette un controllo granulare su chi può eseguire determinate azioni.

| Permesso                | Descrizione                                                                |
|-------------------------|----------------------------------------------------------------------------|
| `CAN_MANAGE_GROUP_INFO` | Permette di usare `/group update`.                                         |
| `CAN_INVITE`            | Permette di usare `/group invite`.                                         |
| `CAN_KICK`              | Permette di usare `/group kick`.                                           |
| `CAN_PROMOTE_DEMOTE`    | Permette di usare `/group promote` e `/group demote`.                      |
| `CAN_MANAGE_ROLES`      | Permette di usare `/group role create/update/delete`.                      |
| `CAN_MANAGE_HOMES`      | Permette di usare `/group sethome/edithome/delhome`.                       |
| `CAN_MANAGE_CLAIMS`     | Permette di usare `/group claim/unclaim`.                                  |
| `CAN_MANAGE_DIPLOMACY`  | Permette di usare `/group diplomacy`.                                      |
| `CAN_MANAGE_BANK`       | Permette di usare `/group withdraw`.                                       |
| `CAN_INTERACT_IN_CLAIM` | Permette di interagire (rompere/piazzare blocchi) nel territorio claimato. |

## 8. Sicurezza

- **Offuscamento del Codice**: Viene utilizzato Proguard per offuscare il codice sorgente compilato, rendendo più
  difficile il reverse engineering.
- **Validazione dell'Input**: Tutta l'input proveniente dai giocatori viene validato per prevenire injection o formati
  di dati imprevisti.
- **Gerarchia dei Ruoli**: Il sistema di priorità dei ruoli impedisce che un membro possa eseguire azioni su un altro
  membro di rango uguale o superiore.

## 9. Testing e Quality Assurance

### 9.1. Strategie di Test

La strategia di testing si basa su test manuali strutturati. I casi di test sono documentati nel file
`docs/TESTING_CASES.md` e coprono tutte le funzionalità principali del plugin.

### 9.2. Casi di Test Esempio

- **Creazione Gruppo**: Verificare che un giocatore possa creare un gruppo e che i vincoli su nome e tag siano
  rispettati.
- **Gestione Membri**: Verificare che un leader possa invitare, espellere e promuovere membri.
- **Claim Territorio**: Verificare che un gruppo possa claimare un chunk e che altri giocatori non possano costruire in
  quel chunk.

## 10. Deployment e Operations

### 10.1. Installazione

1. Compilare il progetto usando Gradle (`./gradlew build`).
2. Copiare il file JAR generato dalla directory `build/libs` alla directory `plugins` del server Hytale.
3. Riavviare il server.

### 10.2. Configurazione

- Il file di configurazione `config.yml` viene generato automaticamente nella directory del plugin.
- Gli amministratori possono modificare questo file per personalizzare il comportamento del plugin.
- Il comando `/group reload` (se implementato o aggiunto) permette di ricaricare la configurazione senza riavviare il
  server.

## 11. Manutenzione e Troubleshooting

- **Logging**: Il plugin utilizza `HytaleLogger` per registrare informazioni, avvisi ed errori. I log sono lo strumento
  principale per il debug.
- **Salvataggi**: I dati vengono salvati in formato JSON leggibile, il che facilita l'ispezione e la correzione manuale
  dei dati in caso di corruzione.
- **Comandi Admin**: Sono previsti comandi di amministrazione per bypassare le restrizioni e gestire i dati del plugin.

## 12. Glossario e Riferimenti

- **Claim**: Atto di rivendicare un chunk di mappa per il proprio gruppo.
- **Grant**: Un permesso specifico assegnato a un ruolo.
- **GUI**: Graphical User Interface, l'interfaccia grafica del plugin.
- **Hytale**: Il gioco per cui questo plugin è sviluppato.
- **JSON**: JavaScript Object Notation, il formato usato per la persistenza dei dati.
- **Singleton**: Un design pattern che restringe l'istanziazione di una classe a un singolo oggetto.
- **UUID**: Universally Unique Identifier, usato per identificare univocamente giocatori e gruppi.
