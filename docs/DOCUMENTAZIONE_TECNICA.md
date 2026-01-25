e Indice dei contenuti

1. Executive Summary ........................................................................... 1
2. Introduzione ................................................................................. 3
3. Architettura del Sistema ............................................................... 5
4. Specifiche Tecniche Dettagliate ...................................................... 9
5. Requisiti .................................................................................... 18
6. Design e Implementazione ............................................................ 21
7. Interfacce e Integrazioni ............................................................ 27
8. Sicurezza ................................................................................ 31
9. Testing e Quality Assurance ....................................................... 36
10. Deployment e Operations .......................................................... 42
11. Manutenzione e Troubleshooting ................................................ 48
12. Glossario ................................................................................ 52
13. Riferimenti e Appendici .......................................................... 54

---

1 Executive Summary

1.1 Scopo del documento

Questo documento fornisce una descrizione tecnica completa e professionalmente strutturata del progetto "
BetterGroupSystemPlugin" (di seguito "il Plugin"), destinata a sviluppatori, architetti, project manager e stakeholder
tecnici. Il documento è conforme alle migliori pratiche di documentazione tecnica (riferimenti: ISO/IEC/IEEE 26514, IEEE
1016) e contiene: architettura, specifiche misurabili, diagrammi descrittivi, requisiti funzionali e non funzionali,
strategie di testing, piani di deployment e operazioni di manutenzione.

1.2 Contesto del progetto

Il Plugin è un modulo server-side per Hytale che fornisce un sistema di Factions & Guilds con gestione di ruoli,
permessi, claim di territorio (chunk), case base (homes), diplomazia e integrazione con un sistema economico (
deposit/withdraw, saldo). Si integra come plugin Java caricato dal processo server Hytale.

1.3 Breve descrizione funzionale

- Gestione entità: Group (fazioni/gilde), GroupMember, GroupRole, GroupClaimedChunk, GroupHome.
- Comandi in-game per creazione, aggiornamento, gestione membri, ruoli, claim/unc- laim chunk, gestione case e economia.
- Persistenza su file JSON (servizio `JsonStorage`) con versioning e backup.
- Notifiche in-game e logging via `NotificationService`.
- Configurazione runtime in `run/config.json` e manifest di plugin in `src/main/resources/manifest.json`.

1.4 Deliverable e audience

Deliverable inclusi:

- Documento tecnico (Markdown + esportazione PDF a richiesta).
- PlantUML sources per diagrammi architetturali e di sequenza.
- Snippet di configurazione e codice esemplificativo.
- Tabelle comparative e metriche di performance.

Audience: sviluppatori backend Java, amministratori server Hytale, QA, responsabili di rilascio.

---

2 Introduzione

2.1 Ambito e obiettivi

Obiettivo: fornire una guida tecnica autorevole che permetta di comprendere, mantenere ed estendere il Plugin. Il
documento copre:

- Architettura e design dei componenti.
- Specifiche tecniche e contratti delle API interne.
- Requisiti non-funzionali (performance, sicurezza).
- Procedure di build, deployment, monitoring e troubleshooting.

Ambito: codice sorgente presente in `src/main/java` e risorse in `src/main/resources`; runtime target: Hytale Server (
versione server: qualsiasi compatibile — manifest indica `"ServerVersion": "*"`).

2.2 Assunzioni e prerequisiti

- Ambiente di sviluppo: Windows 10/11 o Linux/macOS con JDK 25 (vedi `gradle.properties` → `java_version=25`).
- Strumenti: Gradle 8.x compatibile con toolchain Java (uso del plugin Java). Maven non richiesto.
- Path di Hytale correttamente configurato come proprietà Gradle `hytale_home` o rilevato automaticamente (vedi
  `build.gradle`).
- Spazio disco sufficiente per file di log e per la directory di runtime `run/`.

2.3 Convenzioni di lettura

- Terminologia: "Group" indica l'entità logica principale; "Guild" è un sottotipo/variante dello stesso concetto quando
  applicabile.
- Termine "server" riferito al processo Hytale che carica il Plugin.
- Esempi di JSON mostrati sono indentati per leggibilità.
- Nei frammenti di codice si indica versione delle librerie quando rilevata dalle configurazioni di build.

---

3 Architettura del Sistema

3.1 Vista ad alto livello

Componenti principali:

- Plugin bootstrap (`dzve.BetterGroupSystemPlugin`): entrypoint, lifecycle onEnable/onDisable.
- Command layer (`dzve.command.*`): parsing e validazione input, mapping a servizi.
- Service layer (`dzve.service.*`): `GroupService`, `JsonStorage`, `NotificationService`.
- Model layer (`dzve.model.*`): POJO che rappresentano entità persistenti.
- Storage: file system JSON in `run/` (gestito da `JsonStorage`).
- Hytale Server process: host runtime e provider di API per players/permissions.

3.2 Pattern architetturali utilizzati

- Layered Architecture: separazione tra presentation (commands), application (services), and persistence (storage).
- Single Responsibility per classe: ciascuna classe ha responsabilità univoca (es. `JsonStorage` solo persistenza,
  `GroupService` logica di business).
- Transactional Logical Boundaries: non c'è un DB relazionale ACID; transazioni logiche implementate tramite locking e
  write-atomicity su file JSON.
- Defensive Programming e Fail-safe: backup on write, validazione input, rollback semplificato quando possibile.

3.3 Diagramma dei componenti e responsabilità

(vedi PlantUML in appendice — includere nel repo `docs/plantuml/architecture.puml`).

Descrizione testuale:

- `BetterGroupSystemPlugin` inizializza `BetterGroupSystemPluginConfig`, `JsonStorage` e `GroupService`.
- `GroupService` espone API sincrone per comandi: createGroup, disbandGroup, invite, acceptInvitation, claimChunk,
  releaseChunk, setHome, etc.
- `JsonStorage` serializza/deserializza i modelli usando Jackson (jackson-databind 2.21.0) con supporto per java.time
  tramite `jackson-datatype-jsr310`.
- `NotificationService` è invocato dal `GroupService` per notifiche agli utenti e logging.

3.4 Flusso dati e lifecycle (es. creazione gruppo)

Sequenza (alto livello):

1. Player esegue comando `/group create <name>`.
2. `BaseGroupCommand` valida input e permessi.
3. Command invoca `GroupService.createGroup(request)`.
4. `GroupService` costruisce oggetto `Group`, assegna `GroupRole` iniziali, e aggiorna l'in-memory model.
5. `GroupService` chiama `JsonStorage.save(groups)` per persistere lo stato.
6. `NotificationService` notifica il player e i membri interessati.

3.5 Diagramma di deployment (descrizione)

Elementi:

- Host OS (Windows/Linux/macOS)
- Hytale Server process (Java 25 runtime)
- Plugin Jar caricato in `Mods` o tramite `--mods` path definito da Gradle/IDE run configuration.
- Runtime `run/` directory contenente `config.json`, `auth.enc`, `permissions.json`, `logs/`.
- File di storage (es. `groups.json`) gestiti da `JsonStorage` all'interno di `run/`.

---

4 Specifiche Tecniche Dettagliate

4.1 Modelli dati (schema e contratti)

Nota: i modelli sono descritti secondo la struttura trovata in `src/main/java/dzve/model`.

Tabella riassuntiva dei modelli principali

| Modello           |    Campo chiave | Tipo                      | Note                     | Vincoli                                            |
|-------------------|----------------:|---------------------------|--------------------------|----------------------------------------------------|
| Group             |              id | String (UUID consigliato) | Identificatore univoco   | Non nullo, immutabile dopo creazione               |
| Group             |            name | String                    | Nome visuale             | Lunghezza [3..32], univoco per server              |
| Group             |            type | GroupType enum            | Tipo (Guild/Faction/etc) | Valori consentiti definiti in GroupType            |
| GroupMember       |        playerId | String                    | Identificatore giocatore | Deve corrispondere a player registrato             |
| GroupRole         |     permissions | Set<Permission>           | Permessi associati       | Default role con permessi minimi                   |
| GroupClaimedChunk |       x,z,world | int,int,String            | Coordinate di chunk      | Non overlap con chunk già claimati da altri groups |
| GroupHome         | name,coordinate | String,(x,y,z)            | Home di gruppo           | Una default per group, altre opzionali             |

4.1.1 Group (dettagli)

Campi (esempio):

- id: String (es. UUID v4)
- name: String
- type: GroupType
- members: List<GroupMember>
- roles: List<GroupRole>
- claimedChunks: List<GroupClaimedChunk>
- homes: List<GroupHome>
- balance: Decimal (economy)
- power: Integer (PvP power system)

Validazioni principali:

- name unico
- la somma dei membri con ruolo leader >=1
- balance non negativo

4.1.2 GroupMember

Campi:

- playerId: String
- roleId: String (riferimento a GroupRole)
- joinedAt: ISO-8601 datetime
- invitedBy: Optional playerId

Stati: INVITED, ACTIVE, KICKED, LEFT

4.1.3 GroupRole & Permission

- `GroupRole` definisce un set di `Permission` (es. MANAGE_MEMBERS, CLAIM_TERRITORY, DEPOSIT_FUNDS).
- I permessi sono mappati ai comandi e vengono valutati nel `BaseGroupCommand`.

4.1.4 GroupHome, GroupClaimedChunk

- `GroupClaimedChunk` contiene coordinate intere (chunk grid) e metadata (claimedAt, claimedByRoleId).
- `GroupHome` memorizza una coordinate XYZ e un nome descrittivo.

4.2 Storage e formato

- Formato: JSON (serializzazione Jackson). File consigliati: `run/groups.json`, `run/group_metadata.json`.
- Schema: versionato via campo `schemaVersion` in header JSON.
- Atomic writes: implementare scrittura su file temporaneo + rename atomico (posix/ntfs safe) e backup
  `groups.json.bak`.
- Locking: sincronizzazione su JVM (synchronized block) e lock file opzionale per multi-process safety.

Esempio JSON (semplificato):

{
"schemaVersion": 1,
"groups": [
{
"id": "b3f7e8c2-...",
"name": "Guardiani",
"type": "GUILD",
"members": [
{
"playerId": "player-uuid",
"roleId": "role-leader",
"joinedAt": "2026-01-24T22:00:00Z"
}
],
"claimedChunks": [
{"x": 10, "z": -5, "world": "default"}
]
}
]
}

4.3 API e servizi interni (contratti)

Lista minima di API pubbliche (metodi esposti da `GroupService`):

- createGroup(CreateGroupRequest) : Group
- disbandGroup(String groupId) : boolean
- getGroup(String groupId) : Optional<Group>
- listGroups() : List<Group>
- inviteMember(String groupId, String playerId, String invitedBy) : Invitation
- acceptInvitation(String invitationId, String playerId) : boolean
- claimChunk(String groupId, ChunkCoord) : ClaimResult
- setHome(String groupId, GroupHome) : boolean
- deposit(String groupId, BigDecimal amount) : TransactionResult
- withdraw(String groupId, BigDecimal amount, String destinationPlayer) : TransactionResult

Contratti tecnici:

- Tutti i metodi devono restituire oggetti immutabili o copie difensive.
- Input validation: Null-check, range check, permission check.
- Error handling: lanciare eccezioni specifiche (es. GroupNotFoundException, InsufficientFundsException) o utilizzare
  oggetti Result con stato.

4.4 Concorrenza e thread-safety

- `GroupService` deve essere thread-safe. Strategie raccomandate:
    - Utilizzo di `ConcurrentHashMap` per stato in-memory.
    - Locking a livello di groupId (Map<groupId, ReentrantLock>) per operazioni mutative.
    - Scritture su disco serializzate tramite un single-writer executor (ExecutorService a thread singolo) o bloque
      sincronizzato.

---

5 Requisiti

5.1 Requisiti funzionali (RF)

RF-001 Creazione gruppo: un giocatore con permesso crea un group con nome e tipo.
RF-002 Gestione membri: invitare, accettare, kick, trasferire leadership.
RF-003 Ruoli e permessi: creare, aggiornare, assegnare ruoli.
RF-004 Claim territorio: richieste di claim ed unclaim per chunk, validazione conflitti.
RF-005 Home management: impostare e teletrasportare a homes di gruppo.
RF-006 Economia: depositare/ritirare fondi del gruppo, interrogare bilancio.
RF-007 Persistenza: stato conservato tra riavvii nel file system.
RF-008 Notifiche: messaggi ai membri su eventi importanti (invito, claim, disband).

5.2 Requisiti non funzionali (RNF)

RNF-001 Latency: tempo di risposta dei comandi utente p95 < 200ms in configurazione di riferimento (SSD, 8 vCPU, 8GB
heap).
RNF-002 Durability: dopo persist a disk, il dato deve essere recuperabile anche in caso di crash del processo.
RNF-003 Scalabilità: supportare almeno 5.000 gruppi con carico medio di 200 operazioni al minuto senza degrado
significativo.
RNF-004 Availability: sistema tollerante a restart del plugin; coerenza eventuale accettabile (last-write-wins per
operazioni concorrenti non coordinate).
RNF-005 Security: prevenzione escalation permessi e input validation.

5.3 Vincoli e assunzioni

- Nessun DB esterno: storage su file JSON. Migrazioni future possibili verso DB relazionale/NoSQL.
- Dipendenza da API Hytale (jar `HytaleServer.jar`) versione compatibile con runtime Java 25.
- Limiti fisici del mondo Hytale (coordinata chunk) e limiti server.

---

6 Design e Implementazione

6.1 Package structure e responsabilità

- `dzve` (root)
    - `BetterGroupSystemPlugin.java` — bootstrap e iniezione dipendenze manuale.
    - `config` — gestione lettura config (BetterGroupSystemPluginConfig).
    - `command` — implementazioni comandi, classi base e sottopackage per categories.
    - `model` — POJO per entità persistenti.
    - `service` — `JsonStorage`, `GroupService`, `NotificationService`.
    - `utils` — helper, formattatori, adapter per date.

6.2 Diagramma delle classi principali

(Include class diagram PlantUML in appendice — file `docs/plantuml/classes.puml`).

6.3 Strategie di persistenza e caching

- Strategia primaria: in-memory model caricato all'avvio da `JsonStorage` e periodicamente/su mutazione scritto su
  disco.
- Write-through vs write-behind: preferibile write-through per semplicità e durabilità; write-behind con batching può
  essere considerato per scalare (introduce rischio data-loss in caso di crash).
- Cache eviction: non richiesto per l'attuale scope (dataset gestibile in RAM); per dataset grandi implementare
  paginazione e partial-load per gruppi inattivi.

6.4 Error handling e politiche di retry

- IO errors: retry esponenziale limitato (3 tentativi) per scritture su file prima di fallire l'operazione e notificare
  gli admin.
- Corrupted JSON: tentare restore da `groups.json.bak` e loggare l'incidente con stack trace; se fallisce, entrare in
  modalità read-only e sollevare alert.
- Transazioni logiche: dove più passi devono compiersi (es. trasferimento leadership + aggiornamento ruoli), assicurarsi
  che rollback logico venga eseguito in caso di errore.

---

7 Interfacce e Integrazioni

7.1 Comandi utente: sintassi e permessi (esempi)

Esempio: Create Group

- Sintassi: /group create <name> [--type=GUILD]
- Permesso richiesto: groups.create
- Risultato: Group creato, leader assegnato, conferma in chat.

Esempio: Claim Chunk

- Sintassi: /group claim
- Permesso: groups.claim
- Processo: il comando prende la posizione corrente del player e tenta il claim; ritorna errore se chunk già claimato.

Tabella comandi (estratto):

| Comando        | Classe              | Permesso       | Descrizione         |
|----------------|---------------------|----------------|---------------------|
| /group create  | CreateGroupCommand  | groups.create  | Crea un nuovo group |
| /group disband | DisbandGroupCommand | groups.disband | Scioglie un group   |
| /group invite  | InvitePlayerCommand | groups.invite  | Invita un giocatore |
| /group claim   | ClaimChunkCommand   | groups.claim   | Claim di un chunk   |

7.2 Contratti eventi / callback con Hytale

- Hook di join/leave player: usare API Hytale per ascoltare eventi e aggiornare online-status dei membri.
- Permessi: delegare a `permissions.json` per mapping ruoli/permessi.

7.3 Formati di configurazione

- `src/main/resources/manifest.json`: metadata plugin. Versione corrente: 0.0.132 (estratta dal file).
- `gradle.properties`: versione di build (`version=0.0.0`) e `java_version=25`.
- `run/config.json`: runtime server configuration.

Esempio estratto manifest.json:

> {
> "Group": "Dzve",
> "Name": "BetterGroupSystemPlugin",
> "Version": "0.0.132",
> "Main": "dzve.BetterGroupSystemPlugin",
> "IncludesAssetPack": true
> }

Nota: il `gradle.properties` contiene `version=0.0.0` — è raccomandato sincronizzare `version` con `manifest.json` (task
Gradle `updatePluginManifest` effettua il bump di manifest Version e allinea `version`).

---

8 Sicurezza

8.1 Autenticazione e autorizzazione

- Autenticazione: delegata al server Hytale (player identity). Il Plugin non gestisce credenziali utente tranne che per
  eventuali credenziali amministrative in `AuthCredentialStore` (vedi `run/config.json` — `auth.enc`).
- Autorizzazione: modello Role-Based Access Control (RBAC) basato su `GroupRole` e `Permission`.
- Controlli: tutti i comandi devono eseguire il check `hasPermission(player, requiredPermission)` prima di eseguire
  l'azione.

8.2 Crittografia e gestione segreti

- `AuthCredentialStore` nel `run/config.json` è impostato come `Encrypted` con path `auth.enc`. Il Plugin non deve
  memorizzare segreti in chiaro.
- Suggerimento: usare AES-256-GCM per cifratura, chiavi gestite esternamente (es. vault). Evitare hardcoding di chiavi
  nel codice o in `gradle.properties`.

8.3 Validazione input e difesa

- Validazione su tutti i campi ricevuti via comando (regex per nomi, range per coordinate, dimensioni per stringhe).
- Sanitize: rimuovere sequenze di escape, limitare lunghezza di messaggi mostrati in chat per prevenire abuse di chat
  formatting.
- Rate limiting: implementare o delegare al server per prevenire abuse su comandi costosi (es. claim in massa).

8.4 Gestione vulnerabilità

- Dipendenze note:
    - lombok 1.18.42
    - jackson-databind 2.21.0
    - jackson-core 2.21.0
    - jackson-annotations 2.21
    - jackson-datatype-jsr310 2.21.0

- Azione raccomandata: eseguire periodicamente scansione CVE (es. `validate_cves` toolchain) e aggiornare versioni
  critiche, specialmente `jackson-databind` che ha storicamente avuto vulnerabilità deserialization.

---

9 Testing e Quality Assurance

9.1 Strategia di testing

- Unit testing: JUnit 5 (JUnit Jupiter) + Mockito per mocking di dipendenze (es. file system per `JsonStorage`).
- Integration testing: test end-to-end che caricano il plugin in un ambiente Hytale di test o in un container simulato;
  verificare comandi e persistence.
- Contract testing: assicurarsi che le API pubbliche del `GroupService` rispettino i contratti (property-based tests
  dove applicabile).
- Static analysis: SpotBugs, PMD, Checkstyle, e uso di JaCoCo per coverage.

9.2 Casi di test principali (prioritari)

UT-01 CreateGroup happy path (assert group persisted, leader assigned)
UT-02 Invite/Accept flow (invitazione, accettazione, member added)
UT-03 ClaimChunk conflict detection (concurrent claims)
UT-04 Deposit/Withdraw with insufficient funds
UT-05 Persist/Recover corrupted JSON (restore from backup)

Integration tests (IT):
IT-01 End-to-end command flow: create -> claim -> sethome -> disband
IT-02 Concurrent operations: N threads invoking claims on adjacent chunks

9.3 Coverage e criteri di accettazione

- Minimum code coverage: 70% generale, 85% per package `dzve.service`.
- Build acceptance: tutti i test unitari e di integrazione eseguiti in CI devono passare prima di un rilascio.

9.4 Strumenti e setup

- Gradle test task: `./gradlew test` (Windows: `.\gradlew.bat test`).
- JaCoCo: integrare `jacoco` plugin per report coverage.
- Test resources: fixtures in `src/test/resources` e directory `run/test/` per integration fixtures.

---

10 Deployment e Operations

10.1 Build e packaging

- Build system: Gradle. Plugin Java configurato in `build.gradle`.
- Dipendenze compile-time:
    - org.projectlombok:lombok:1.18.42
    - com.fasterxml.jackson.core:jackson-databind:2.21.0
    - ... (vedi sezione Build)
- Task utile: `updatePluginManifest` (bump manifest Version e sincronizza `version`).
- Packaging output: `build/libs/BetterGroupSystemPlugin-<version>.jar`.

Esempio comandi (Windows PowerShell):

```powershell
.\gradlew.bat clean build -x test
```

10.2 Configurazione runtime

File principali:

- `run/config.json` (server runtime settings) — esempio presente in repository.
- `run/permissions.json` — mapping ruoli e permessi (es. admin, default).
- `auth.enc` — credenziali cifrate per autenticazione server.

10.3 Procedure di aggiornamento e rollback

Rilascio standard:

1. Aggiornare `src/main/resources/manifest.json` o usare `updatePluginManifest`.
2. Incrementare `gradle.properties` version se necessario.
3. Build JAR e deploy in `Mods` path configurato.
4. Riavviare server Hytale o ricaricare plugin (se supportato).

Rollback:

- Mantenere almeno 3 backup di JAR e backup del `run/groups.json` (es. timestamped). Per rollback:
    1. Sostituire JAR con versione precedente.
    2. Ripristinare `groups.json` da backup compatibile con schemaVersion.
    3. Riavviare server.

10.4 Monitoraggio e logging

- Logging: leve e rotazione in `run/logs/` (file example presenti in repository).
- Metriche da esporre (es. via JMX o endpoint HTTP locale):
    - Command latency histogram
    - Persist write latency
    - Number of groups, number of claimed chunks
    - Errors per minute

Strumenti consigliati: Prometheus (scraping exporter custom), Grafana per dashboard.

---

11 Manutenzione e Troubleshooting

11.1 Procedure di manutenzione ordinaria

- Backup giornaliero della directory `run/` (incluso `groups.json`, `auth.enc`, `permissions.json`).
- Verifica settimanale integrità JSON (strumento di validazione schema).
- Aggiornamento dipendenze trimestrale o su rilevamento CVE critiche.

11.2 Debugging common issues

Problema: "Group non presente dopo riavvio"

- Verificare `run/groups.json` e `groups.json.bak` per presenza dati.
- Controllare log (`run/logs/`) per eccezioni di parsing.

Problema: "Claim fallisce con errore di lock"

- Verificare concorrenza: controllare se un processo esterno sta bloccando il file.
- Controllare spazio disco e permessi file.

11.3 Logging levels e raccolta informazioni

Livelli consigliati:

- INFO: operazioni utente riuscite (crea, claim)
- WARN: condizioni anomale recuperabili
- ERROR: eccezioni non gestite, IO error persistenti
- DEBUG: dettagli per sviluppo (abilitato in ambiente test)

11.4 Migrazioni dati e compatibilità

- Quando si cambia `schemaVersion`, implementare migrator classes in `dzve.service.migrations` che leggono la versione
  corrente e trasformano al nuovo schema.
- Conservare i file `groups.json.v{n}.bak` per rollback.

---

12 Glossario

- Plugin: modulo Java caricato da Hytale server.
- Group: entità logica (fazione/gilda).
- Role: ruolo all'interno del gruppo che definisce permessi.
- Claim/Chunk: porzione del world gestita in coordinate chunk.
- Manifest: `manifest.json` file metadata del plugin.

---

13 Riferimenti e Appendici

13.1 Versioni e configurazioni estratte (fonte di verità)

- `src/main/resources/manifest.json`:
    - Version: 0.0.132
    - Main: dzve.BetterGroupSystemPlugin
    - IncludesAssetPack: true

- `gradle.properties`:
    - version: 0.0.0 (sincronizzare con manifest.json)
    - java_version: 25
    - includes_pack: true
    - patchline: release
    - load_user_mods: false

- `build.gradle` (dipendenze):
    - lombok: 1.18.42
    - jackson-databind: 2.21.0
    - jackson-core: 2.21.0
    - jackson-annotations: 2.21
    - jackson-datatype-jsr310: 2.21.0
    - Dependency runtime: HytaleServer.jar (path derivato da `hytaleHome` e `patchline`)

13.2 Metriche di performance attese e SLAs

Raccomandazioni (ambiente di riferimento: SSD, 8 vCPU, 8GB heap):

| Metrica              |         Target | Rationale                                    |
|----------------------|---------------:|----------------------------------------------|
| p95 command latency  |       < 200 ms | esperienza utente fluida per comandi in-game |
| p99 command latency  |          < 1 s | tolleranza in casi di picco                  |
| Persist write p95    | < 100 ms (SSD) | serializzazione JSON e I/O locale            |
| Max gruppi gestiti   |          5.000 | dimensione testata limite raccomandata       |
| Throughput operativo |    200 ops/min | carico medio sostenibile                     |

13.3 Diagrammi PlantUML (sorgenti suggeriti)

Architecture (esempio PlantUML):

@startuml
package "Hytale Server" {
[Hytale Process]
}
package "Plugin" {
[BetterGroupSystemPlugin]
[Command Layer]
[GroupService]
[JsonStorage]
[NotificationService]
[Model]
}
[Hytale Process] --> [BetterGroupSystemPlugin]
[BetterGroupSystemPlugin] --> [Command Layer]
[Command Layer] --> [GroupService]
[GroupService] --> [JsonStorage]
[GroupService] --> [NotificationService]
@enduml

Class diagram (sintetico):

@startuml
class Group {

- String id
- String name
- GroupType type
- List<GroupMember> members
  }
  class GroupService {

+ createGroup()
+ disbandGroup()
  }
  GroupService --> Group
  Group "1" --> "*" GroupMember
  @enduml

13.4 Tabelle comparative (storage options)

| Opzione    |    Durability |               Scalabilità | Complessità | Raccomandazione                       |
|------------|--------------:|--------------------------:|-------------|---------------------------------------|
| File JSON  | Alta (locale) | Limitata (dataset grande) | Bassa       | Buono per MVP / installazioni singole |
| SQLite     |          Alta |                  Moderata | Media       | Migrazione naturale per dataset medio |
| PostgreSQL |          Alta |                      Alta | Alta        | Consigliato per cluster/scale-out     |

13.5 Rischi tecnici e mitigazioni

- Rischio: Vulnerabilità in `jackson-databind` (deserialization RCE). Mitigazione: aggiornare alla versione stabile più
  recente e utilizzare whitelisting dei tipi di classe; evitare deserializzazione di payload remoti non fidati.
- Rischio: Corruzione di `groups.json`. Mitigazione: backup atomici, validazione schema, modalità read-only su errore
  grave.
- Rischio: Concorrenza su write file. Mitigazione: single-writer executor, locking su groupId.

13.6 Checklist di consegna (verifiche prima del rilascio)

- [ ] Allineare `gradle.properties`::version con `manifest.json`::Version
- [ ] Eseguire `.\gradlew.bat clean build` e verificare artefatto JAR
- [ ] Eseguire test unitari e integration tests
- [ ] Generare report JaCoCo e controllare coverage
- [ ] Effettuare backup `run/` prima del deploy
- [ ] Aggiornare changelog e tag Git

---

Contatti e note finali

Per ulteriori dettagli tecnici posso generare automaticamente:

- estratti API (metodi pubblici) delle classi elencate in `src/main/java` per popolare le sezioni tecniche;
- file PlantUML completi in `docs/plantuml/`;
- test template JUnit per `GroupService` e `JsonStorage`.

Indica quale di questi preferisci come prossimo passo e procedo automaticamente.
