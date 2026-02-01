# Documentazione Progetto: BetterGroupSystemPlugin

## 1. Panoramica

**BetterGroupSystemPlugin** è un plugin completo per server Hytale che implementa Fazioni e Gilde. Il sistema gestisce
gruppi, territori, economia virtuale chiusa e diplomazia avanzata.

### Informazioni Plugin

- **Versione**: 0.0.519
- **Sviluppatori**: DZVE Development Team
- **Repository**: https://github.com/dzve/hytale-factions-guilds
- **Linguaggio**: Java (con toolchain Java 17+)
- **Database**: SQLite JDBC 3.45.1.0
- **Build System**: Gradle con ProGuard per obfuscation

### Modalità di Gioco

Il plugin opera in due modalità switchabili (Config: `PluginMode`):

1. **FACTION** (Default):
    * Focalizzata sul **PvP e Raiding**.
    * Basata sul **Power**. Se `Power < Claims`, la fazione è **Raidable** (vulnerabile).
2. **GUILD**:
    * Focalizzata sulla **Cooperazione**.
    * Basata su **Livelli**. I livelli sbloccano slot membri extra.

### Visibilità Mappa

* **HidePlayers** (Config):
    * `true`: I giocatori vedono sulla mappa **solo** i membri del proprio gruppo e gli alleati.
    * `false`: Tutti i giocatori sono visibili a tutti sulla mappa globale.

---

## 2. Architettura del Sistema

### Struttura del Progetto

```
src/main/java/dzve/
├── BetterGroupSystemPlugin.java     # Main plugin class
├── api/
│   └── BetterGroupEconomyAPI.java   # Public API per economia
├── command/                         # Sistema comandi (48 classi)
│   ├── BaseGroupCommand.java        # Radice comandi
│   ├── admin/                       # Comandi admin (11)
│   ├── chat/                        # Sistema chat (2)
│   ├── diplomacy/                   # Gestione diplomazia (5)
│   ├── economy/                     # Gestione economia (4)
│   ├── management/                  # Gestione gruppi (6)
│   ├── member/                      # Gestione membri (6)
│   ├── role/                        # Gestione ruoli (5)
│   └── territory/                   # Gestione territori (8)
├── config/
│   └── BetterGroupSystemPluginConfig.java
├── database/
│   ├── DatabaseManager.java         # Gestione DB SQLite
│   └── dao/                         # Data Access Objects
├── listener/                        # Event handlers
├── model/                           # Data models (11 classi)
├── service/                         # Business logic
├── systems/                         # Game systems
│   ├── DamageTrackerSystem.java     # Tracking danni PvP
│   ├── PowerDeathSystem.java        # Gestione power/morti
│   ├── PvPProtectionSystem.java     # Protezioni PvP
│   └── claim/                       # Sistemi claim (2)
└── utils/                           # Utilità varie
```

### Componenti Principali

#### 1. **Core Plugin** (`BetterGroupSystemPlugin`)
- Estende `JavaPlugin` di Hytale
- Gestisce lifecycle: setup() → start() → shutdown()
- Inizializza tutti i sistemi e registrazioni

#### 2. **GroupService** (Singleton)
- **Core Business Logic**: Gestione completa gruppi
- **Database Integration**: SQLite con WAL mode per atomicità
- **Cache Management**: ConcurrentHashMap per performance
- **Config Validation**: Sistema di validazione configurazione
- **Recent Fix**: Aggiunto `getConfig()` method con null-check per prevenire NullPointerException

#### 3. **Sistema Comandi**
- **BaseGroupCommand**: Radice unificata con prefix configurabile
- **48 sottocomandi** organizzati per categoria
- **Validazione input**: Regex `^[\p{L}\p{N}_]+$` per nomi
- **Permission System**: 15+ nodi permesso granulari

#### 4. **Sistemi di Gioco**
- **ClaimProtectionSystems**: Protezione territori (Break/Place/Use/Damage)
- **PowerDeathSystem**: Gestione power per PvP
- **DamageTrackerSystem**: Tracking danni per calcoli power
- **PvPProtectionSystem**: Protezioni PvP configurabili
- **ClaimAlertSystem**: Notifiche territoriali

#### 5. **Public API** (`BetterGroupEconomyAPI`)
- **Thread-Safe**: Accesso concorrente sicuro
- **Auto-Persistence**: Salvataggio automatico modifiche
- **ProGuard Safe**: Escluso da obfuscation
- **Metodi**: 10+ metodi per gestione economica

---

## 3. Meccaniche di Gioco

### Sistema Potere (Power) - Solo FACTION

* **Formula Raidability**: Una fazione è Raidable se `TotalPower < NumberOfClaims`.
* **Conquista (Raid)**: Se una fazione è Raidable, un nemico può usare `/faction claim` sul loro territorio per *rubarlo
  istantaneamente*.
* **Gestione Power**:
    * **Morte**: `-1.0` (Config: `PowerLooseByDeath`).
    * **Kill**: `+1.0` (Config: `PowerGainByKill`) solo se uccidi un nemico di un'altra fazione.
    * **Regen Online**: `+0.001` ogni minuto.
    * **Regen Offline**: `+0.0001` (molto lento).
    * **Cap**: Min `-100`, Max `+100`.

### Sistema Livelli (Levels) - Solo GUILD

Le gilde non hanno potere, ma livelli acquistabili.

* **Costo Upgrade**: `InitialPrice * (Multiplier ^ (CurrentLevel + 1))`
    * Esempio (Default): `2500 * (1.2 ^ (Level + 1))`
* **Benefici**: Ogni livello aggiunge `SlotQuantityGainForLevel` (default 10) slot membri.

### Territorio e Mappa

* **Protezione**: Blocca `Break`, `Place`, `Use`, `Damage` a chi non è membro.
* **Mappa Claim**: Visualizzabile con `/faction map` (Area 21x6 chunk).
    * `@`: Posizione corrente.
    * `O`: Own (Tuo).
    * `A`: Ally.
    * `E`: Enemy.
    * `-`: Wild.

### Economia (Virtuale Chiusa)

Il sistema economico è interno e non comunica con item fisici.

1. **Admin**: Unico creatore di valuta (`/faction admin setplayermoney`).
2. **Personale**: Saldo del giocatore (virtuale).
3. **Banca Gruppo**: Saldo del gruppo (accumulato tramite depositi).
    * Usa `/faction deposit` per spostare "Personale -> Banca".
    * Usa `/faction withdraw` per spostare "Banca -> Personale".

---

## 4. Configurazione Avanzata

### Parametri Principali (Config)

| Categoria | Parametro | Default | Descrizione |
|-----------|-----------|---------|-------------|
| **Generale** | `PluginMode` | `"FACTION"` | Modalità: FACTION o GUILD |
| | `AllCommandsPrefix` | `"faction"` | Prefisso comandi |
| | `HidePlayers` | `true` | Visibilità giocatori mappa |
| **Power System** | `PlayerInitialPower` | `5.0` | Power iniziale giocatore |
| | `PlayerPowerMax` | `100.0` | Power massimo |
| | `PowerGainByKill` | `1.0` | Power per kill nemico |
| | `PowerLooseByDeath` | `1.0` | Power perso per morte |
| | `ClaimRatio` | `1.0` | Ratio power/claim |
| **Guild System** | `InitialPrice` | `2500.0` | Costo primo livello |
| | `LevelPriceMultiplier` | `1.2` | Moltiplicatore costo |
| | `SlotQuantityGainForLevel` | `10` | Slot extra per livello |
| **Economy** | `EnableTax` | `false` | Sistema tasse |
| | `TaxImport` | `0.0` | Percentuale tassa |
| | `TaxInterval` | `86400` | Intervallo tasse (sec) |
| **Limits** | `MaxSize` | `10` | Dimensione massima gruppo |
| | `MaxClaimsPerFaction` | `100` | Claim massimi |
| | `MaxHome` | `5` | Home massime per gruppo |

### Validazione Nomi

* **Regex**: `^[\p{L}\p{N}_]+$` (Solo Lettere, Numeri, Underscore)
* **Lunghezza Nome**: 3-15 caratteri (`MinNameLength`, `MaxNameLength`)
* **Lunghezza Tag**: 2-5 caratteri (`MinTagLength`, `MaxTagLength`)
* **Colore**: HEX valido `^#[0-9a-fA-F]{6}$`

---

## 5. Architettura Database

### Struttura SQLite

**Path**: `mods/Dzve_BetterGroupSystemPlugin/data/groups.db`
**Mode**: WAL (Write-Ahead Logging) per atomicità

### Tabelle Principali

```sql
-- groups: Informazioni base gruppi
CREATE TABLE groups (
    id UUID PRIMARY KEY,
    name VARCHAR(15) UNIQUE NOT NULL,
    tag VARCHAR(5) UNIQUE NOT NULL,
    description VARCHAR(255),
    color VARCHAR(7),
    type VARCHAR(10), -- 'FACTION' or 'GUILD'
    bank_balance DOUBLE DEFAULT 0.0,
    created_at TIMESTAMP,
    level INTEGER DEFAULT 1 -- Solo per GUILD
);

-- members: Gestione membri e ruoli
CREATE TABLE members (
    group_id UUID REFERENCES groups(id),
    player_uuid UUID PRIMARY KEY,
    role VARCHAR(20) DEFAULT 'MEMBER',
    bank_balance DOUBLE DEFAULT 0.0,
    joined_at TIMESTAMP
);

-- claims: Territori reclamati
CREATE TABLE claims (
    group_id UUID REFERENCES groups(id),
    chunk_x INTEGER,
    chunk_z INTEGER,
    claimed_at TIMESTAMP,
    PRIMARY KEY (group_id, chunk_x, chunk_z)
);

-- diplomacy: Relazioni tra gruppi
CREATE TABLE diplomacy (
    group1_id UUID REFERENCES groups(id),
    group2_id UUID REFERENCES groups(id),
    status VARCHAR(10), -- 'ALLY', 'ENEMY', 'NEUTRAL'
    requested_by UUID,
    created_at TIMESTAMP,
    PRIMARY KEY (group1_id, group2_id)
);

-- homes: Teleport home
CREATE TABLE homes (
    group_id UUID REFERENCES groups(id),
    name VARCHAR(20),
    world_uuid UUID,
    x DOUBLE, y DOUBLE, z DOUBLE,
    is_default BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (group_id, name)
);

-- roles: Ruoli personalizzati
CREATE TABLE roles (
    group_id UUID REFERENCES groups(id),
    name VARCHAR(20) PRIMARY KEY,
    permissions TEXT, -- JSON array permessi
    created_at TIMESTAMP
);
```

### Performance & Cache

* **ConcurrentHashMap**: Cache in-memory per gruppi attivi
* **Lazy Loading**: Caricamento dati on-demand
* **Auto-Save**: Persistenza automatica modifiche
* **Backup Strategy**: Backup periodico del file `.db` raccomandato

---

## 6. Sistema Permessi

### Nodi Permesso

Ogni azione sensibile richiede un permesso specifico nel Ruolo del giocatore.

| Azione | Nodo Permesso |
| :--- | :--- |
| Prelevare soldi | `CAN_MANAGE_BANK` |
| Modificare nome/desc/tag/color | `CAN_UPDATE_GROUP` |
| Invitare giocatori | `CAN_INVITE` |
| Espellere membri | `CAN_KICK` |
| Gestire Ruoli (Creare/Assegnare) | `CAN_MANAGE_ROLE` / `CAN_CHANGE_ROLE` |
| Settare/Eliminare Home | `CAN_MANAGE_HOME` |
| Teletrasportarsi alle Home | `CAN_TELEPORT_HOME` |
| Reclamare/Unclaimare terre | `CAN_MANAGE_CLAIM` |
| Costruire in territorio protetto | `CAN_INTERACT_IN_CLAIM` |
| Dichiarare guerra/alleanza | `CAN_MANAGE_DIPLOMACY` |
| Livellare la Gilda | `CAN_UPGRADE_GUILD` |
| Chat Gruppo/Alleata | `CAN_CHAT_INTERNAL` / `CAN_CHAT_ALLY` |

*Il Leader ha tutti i permessi (bypass).*

### Ruoli Predefiniti

- **LEADER**: Tutti i permessi, bypass automatico
- **OFFICER**: Permessi gestione membri e territori
- **MEMBER**: Permessi base di interazione
- **CUSTOM**: Ruoli personalizzati con permessi configurabili

---

## 7. Riferimento Comandi Completo

### Comandi Giocatore

#### Gestione Gruppo
- `/faction create <name> [tag] [desc] [color]` - Crea nuovo gruppo
- `/faction join <group>` - Unisciti a gruppo (se aperto)
- `/faction leave` - Abbandona gruppo
- `/faction info [group]` - Informazioni gruppo
- `/faction upgrade` - Aumenta livello gilda

#### Territorio
- `/faction claim` - Reclama chunk corrente
- `/faction unclaim` - Libera chunk corrente
- `/faction map` - Mostra mappa claim (21x6)
- `/faction claimMap` - Mappa visuale in chat

#### Home System
- `/faction setHome <name>` - Crea home
- `/faction home <name>` - Teletrasporta home
- `/faction deleteHome <name>` - Elimina home
- `/faction setDefaultHome <name>` - Home predefinita
- `/faction listHomes` - Elenca home

#### Economia
- `/faction deposit <amount>` - Deposita in banca
- `/faction withdraw <amount>` - Preleva da banca
- `/faction balance` - Saldo banca gruppo
- `/faction power [player]` - Power giocatore/fazione

#### Membri
- `/faction invite <player>` - Invita giocatore
- `/faction acceptInvite <group>` - Accetta invito
- `/faction kick <player>` - Espelli membro
- `/faction members` - Lista membri
- `/faction transfer <player>` - Trasferisci leadership
- `/faction invitations` - Inviti pendenti

#### Ruoli
- `/faction createRole <name> [permissions]` - Crea ruolo
- `/faction setRole <player> <role>` - Assegna ruolo
- `/faction updateRole <name> <permissions>` - Modifica ruolo
- `/faction deleteRole <name>` - Elimina ruolo
- `/faction listRoles` - Lista ruoli

#### Diplomazia
- `/faction diplomacy <group> <ALLY|NEUTRAL|ENEMY>` - Imposta relazione
- `/faction acceptAlly <group>` - Accetta alleanza
- `/faction denyAlly <group>` - Rifiuta alleanza
- `/faction allyRequests` - Richieste alleanza
- `/faction listDiplomacy` - Relazioni attuali

#### Chat
- `/faction chat <group|ally> <message>` - Chat gruppo/alleata

### Comandi Admin (Richiede OP)

#### Gestione Gruppi
- `/faction admin disband <group>` - Scioglie gruppo
- `/faction admin info <group>` - Info gruppo admin
- `/faction admin setLeader <group> <player>` - Cambia leader
- `/faction admin kick <group> <player>` - Espelli forzato

#### Economia
- `/faction admin setMoney <group> <amount>` - Imposta saldo banca
- `/faction admin setPlayerMoney <player> <amount>` - **FONDAMENTALE**: Crea valuta

#### Permessi
- `/faction admin grantPerm <group> <permission>` - Concedi permesso globale
- `/faction admin revokePerm <group> <permission>` - Revoca permesso globale

#### Diplomazia
- `/faction admin setDiplomacy <group1> <group2> <relation>` - Forza relazione

#### Sistema
- `/faction admin reload` - Ricarica configurazione

---

## 8. API Sviluppatori

### BetterGroupEconomyAPI

Classe: `dzve.api.BetterGroupEconomyAPI`
Thread-Safe e con persistenza automatica.

#### Metodi Principali

```java
// Singleton
BetterGroupEconomyAPI api = BetterGroupEconomyAPI.getInstance();

// --- Player Finance ---
double balance = api.getMemberBalance(playerId);
boolean success = api.setMemberBalance(playerId, 100.0);
api.depositMemberBalance(playerId, 50.0);
boolean withdrawn = api.withdrawMemberBalance(playerId, 25.0);

// --- Group Finance ---
UUID groupId = api.getPlayerGroup(playerId);
double groupBank = api.getGroupBalance(groupId);
api.setGroupBalance(groupId, 1000.0);
api.depositGroupBalance(groupId, 500.0);
boolean withdrawn = api.withdrawGroupBalance(groupId, 200.0);

// --- Utility ---
UUID playerGroup = api.getPlayerGroup(playerId);
```

#### Note Tecniche

- **Thread-Safe**: Tutti i metodi sono sincronizzati
- **Auto-Persistence**: Le modifiche vengono salvate automaticamente
- **ProGuard Safe**: Il package `dzve.api` è escluso dall'obfuscation
- **Error Handling**: Restituisce `false` per operazioni fallite, lancia eccezioni per errori critici

---

## 9. Build & Deployment

### Gradle Build System

```bash
# Build completo con obfuscation
./gradlew build

# Deploy automatico in Hytale Mods
./gradlew build  # Include copyObfuscatedJarToHytale

# Run server di test
./gradlew runServer
```

### ProGuard Configuration

- **Obfuscation**: Attiva per protezione codice
- **API Preservation**: Package `dzve.api` escluso
- **Dependencies**: Jackson, SQLite bundled nel JAR
- **Output**: `build/libs/${project.name}-${version}.jar`

### Deployment Path

**Windows**: `%APPDATA%/Roaming/Hytale/UserData/Mods/`
**Linux**: `~/.local/share/Hytale/UserData/Mods/`
**macOS**: `~/Library/Application Support/Hytale/UserData/Mods/`

---

## 10. Correzioni Recenti & Miglioramenti

### Fix NullPointerException (2026-01-31)

**Problema**: Il campo statico `config` in `GroupService` era null quando `claimChunk()` tentava di accedere a `config.getClaimRatio()`.

**Soluzione Implementata**:
- Aggiunto metodo `getConfig()` con null-check che lancia `IllegalStateException` se config non è inizializzato
- Aggiunto null-check in `getInstance()` per prevenire config null
- Sostituito tutti gli accessi diretti a `config` con chiamate `getConfig()` in tutto `GroupService`
- Reso `getConfig()` public per permettere ad altre classi (Faction, Guild) di accedere safely alla configurazione

**Impatto**: Il fix garantisce che qualsiasi tentativo di accesso alla configurazione prima dell'inizializzazione risulti in un errore chiaro invece di un `NullPointerException`.

### Altri Miglioramenti

- **Performance**: Ottimizzazione cache con `ConcurrentHashMap`
- **Thread Safety**: Sincronizzazione accessi concorrenti
- **Error Handling**: Messaggi di errore dettagliati
- **Logging**: Sistema di logging centralizzato con `LogService`

---

## 11. Troubleshooting & Manutenzione

### Problemi Comuni

1. **NullPointerException in claimChunk()**
   - **Causa**: Configurazione non inizializzata
   - **Fix**: Verificare sequenza startup plugin

2. **Database Corruption**
   - **Prevenzione**: Backup periodico di `groups.db`
   - **Recovery**: Ripristinare da backup

3. **Performance Issues**
   - **Causa**: Troppi gruppi caricati in memoria
   - **Soluzione**: Monitorare dimensione cache

### Manutenzione Programmata

- **Backup Database**: Giornale del file `.db`
- **Monitor Logs**: Errori in console Hytale
- **Update Config**: Revisione parametri bilanciamento
- **Performance Review**: Monitoraggio lag server

---

*Revisione 4.0 - Documentazione Tecnica Completa (2026-01-31)*
*Aggiornata con architettura dettagliata, configurazione avanzata, fix recenti*
