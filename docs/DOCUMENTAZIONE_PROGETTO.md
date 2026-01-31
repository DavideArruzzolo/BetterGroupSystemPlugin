# Documentazione Progetto: BetterGroupSystemPlugin

## 1. Panoramica

**BetterGroupSystemPlugin** è un plugin completo per server Hytale che implementa Fazioni e Gilde. Il sistema gestisce
gruppi, territori, economia virtuale chiusa e diplomazia avanzata.

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

## 2. Meccaniche di Gioco

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

## 3. Riferimento Tecnico Avanzato

### Validazione Input

Tutti i nomi (Gruppi, Tag, Home) devono rispettare queste regole rigorose:

* **Regex**: `^[\p{L}\p{N}_]+$` (Solo Lettere, Numeri, Underscore).
* **Lunghezza Nome**: 3-15 caratteri.
* **Lunghezza Tag**: 2-5 caratteri.
* **Colore**: Deve essere un HEX valido (es. `#FF0000`).

### Permessi (Nodes)

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

### Persistenza Dati

* **Database**: SQLite JDBC (`org.sqlite.JDBC`).
* **Path**: `mods/Dzve_BetterGroupSystemPlugin/data/groups.db` (NON JSON).
* **Backup**: Consigliato fare backup del file `.db`. Le operazioni sono Atomiche (WAL Mode attivo).
* **Struttura**: Tabelle Relazionali (`groups`, `members`, `claims`, `diplomacy`, `homes`, `roles`).

### Valori Hardcoded (Non Configurabili)

Alcuni elementi sono cablati nel codice e non modificabili da config:

1. **Driver Database**: SQLite (`groups.db`).
2. **Simboli Mappa**: `@` (Tu), `O` (Own), `A` (Ally), `E` (Enemy), `-` (Wild).
3. **Regex Nomi**: `^[\p{L}\p{N}_]+$` (Solo alfanumerici e underscore).
4. **Chiavi Logica**: "FACTION" e "GUILD" sono stringhe interne fisse.

---

## 4. Comandi (Reference Completo)

### Giocatori

* `/faction create <name> [tag] [desc] [color]`
* `/faction join <group>`
* `/faction leave`
* `/faction info [group]`
* `/faction map`
* `/faction deposit <amount>`
* `/faction withdraw <amount>`
* `/faction claim` (Costo: 0, ma richiede Power/Slot)
* `/faction unclaim`
* `/faction home <name>`
* `/faction sethome <name>`
* `/faction diplomacy <group> <ALLY|NEUTRAL|ENEMY>`
* `/faction acceptAlly <group>`
* `/faction chat <group|ally> <msg>`

### Admin (Richiede OP/Creative)

* `/faction admin setplayermoney <player> <amount>` (**Fondamentale**)
* `/faction admin setmoney <group> <amount>`
* `/faction admin disband <group>`
* `/faction admin info <group>`
* `/faction admin reload`

---

## 5. API Sviluppatori (Java)

Il plugin espone un'API pubblica per permettere ad altri plugin di interagire con il sistema (es. economia).

### BetterGroupEconomyAPI

Classe: `dzve.api.BetterGroupEconomyAPI`
Thread-Safe e con persistenza automatica.

#### Esempi di Utilizzo

```java
// Ottieni l'istanza singleton
BetterGroupEconomyAPI api = BetterGroupEconomyAPI.getInstance();

// --- Gestione Player ---
UUID playerId = player.getUuid();

// Ottieni saldo personale nel gruppo
double balance = api.getMemberBalance(playerId);

// Deposita/Preleva
api.depositMemberBalance(playerId, 100.0);
boolean success = api.withdrawMemberBalance(playerId, 50.0);

// --- Gestione Gruppo ---
UUID groupId = api.getPlayerGroup(playerId);
if (groupId != null) {
    // Ottieni saldo banca gruppo
    double groupBank = api.getGroupBalance(groupId);
    
    // Modifica saldo banca
    api.depositGroupBalance(groupId, 500.0);
}
```

**Nota per la compilazione (Proguard)**:
Il pacchetto `dzve.api` è escluso dall'offuscamento, quindi le classi e i metodi pubblici rimangono accessibili.

---
*Revisione 3.0 - Documentazione Tecnica Finale (2026-01-31)*
