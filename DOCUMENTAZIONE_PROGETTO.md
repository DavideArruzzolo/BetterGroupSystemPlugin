# Documentazione Progetto: BetterGroupSystemPlugin

## 1. Panoramica

**BetterGroupSystemPlugin** è un plugin completo per server Hytale che implementa un sistema avanzato di Fazioni e
Gilde. Il progetto è progettato per gestire gruppi di giocatori, territori, economia e diplomazia, offrendo
un'esperienza di gioco strategica e competitiva.

### Funzionalità Principali

* **Gestione Gruppi**: Creazione, gestione membri, ruoli personalizzabili.
* **Territorio (Claim)**: Protezione dei chunk, sistema di "Home", e meccaniche di raid basate sul potere.
* **Economia**: Banca di fazione, tasse (*Work in Progress*), livelli di gilda acquistabili.
* **Potere (Power)**: Sistema dinamico di potere che determina la vulnerabilità delle fazioni.
* **Diplomazia**: Gestione di relazioni (Alleati, Neutrali, Nemici).
* **Integrazione Globale**: Chat formattata e marcatori sulla mappa.
* **Notifiche**: Sistema di notifiche in tempo reale.

---

## 2. Installazione e Configurazione

### Installazione

Posizionare il file `.jar` generato nella cartella `mods` del server Hytale.

### Configurazione (`groups.json`)

Il file si trova in `mods/Dzve_BetterGroupSystemPlugin/data/groups.json`.

* **Generali**:
    * `PluginMode`: `FACTION` (con Power) o `GUILD` (con Livelli).
    * `AllowedWorlds`: Mondi abilitati (default: `*`).
    * `MaxSize`: Membri massimi base (default: 10).
* **Economia**:
    * `InitialPrice`: Costo creazione.
    * `EnableTax`: *Nota: Il parametro esiste nel config ma la logica di prelievo automatico non sembra essere attiva
      nel codice corrente.*
* **Potere (Power)**:
    * `PlayerInitialPower`: (default: 5)
    * `PlayerPowerMax`: (default: 100)
    * `PowerLooseByDeath`: (default: 1)
* **Admin**:
    * Non ci sono comandi di bypass admin espliciti, eccetto `reload` che richiede modalità Creativa.

---

## 3. Sistema di Gruppi e Ruoli

### Gestione Membri

* **Inviti**: I leader possono invitare giocatori con `/faction invite`.
* **Ruoli**: Sistema flessibile. I ruoli personalizzati possono avere i seguenti permessi:

### Nodi Permessi (Case Sensitive)

* `CAN_MANAGE_BANK`: Depositare/Prelevare.
* `CAN_UPDATE_GROUP`: Modificare informazioni gruppo.
* `CAN_INVITE`: Invitare nuovi membri.
* `CAN_KICK`: Espellere membri.
* `CAN_CHANGE_ROLE`: Modificare ruolo membri.
* `CAN_MANAGE_ROLE`: Creare/Eliminare ruoli.
* `CAN_MANAGE_HOME`: Gestire le Home.
* `CAN_TELEPORT_HOME`: Usare le Home.
* `CAN_MANAGE_CLAIM`: Reclamare territori.
* `CAN_INTERACT_IN_CLAIM`: Costruire in territorio protetto.
* `CAN_MANAGE_DIPLOMACY`: Gestire relazioni diplomatiche.
* `CAN_CHAT_INTERNAL`: Usare chat gruppo.
* `CAN_CHAT_ALLY`: Usare chat alleanza.
* `CAN_UPGRADE_GUILD`: Livellare la gilda (Solo modalità GUILD).

---

## 4. Intestazioni Gameplay

### Chat Formatting

Formato: `[TAG] NomeGiocatore: Messaggio`
Il colore del tag e delle parentesi dipende dal colore scelto dalla fazione.

### Mappa (WorldMap)

I membri del gruppo vedono i marker dei compagni sulla mappa in tempo reale (`WorldMapTracker`).

---

## 5. Territorio e Protezione

### Claims

I claim proteggono i chunk (16x16 blocchi) da modifiche esterne.

* **Eventi Protetti**: `BreakBlock`, `PlaceBlock`, `UseBlock`, `DamageBlock`, `PvP` (Friendly Fire disabilitato).

### Raiding (Solo modalità FACTION)

Se `Potere Totale < Numero Claims`, la fazione diventa **Raidable**.
In questo stato, le protezioni sui blocchi vengono disattivate per permettere attacchi.

* **Notifica Raid**: Se un non-membro entra in un territorio raidable, i membri ricevono un alert ("Danger").

---

## 6. Economia e Progressione

### Banca

Ogni gruppo ha un conto comune.

* Deposito: Libero.
* Prelievo: Richiede permesso `CAN_MANAGE_BANK`.

### Livelli Gilda (Solo modalità GUILD)

Formula costo upgrade:
`Costo = InitialPrice * (LevelPriceMultiplier ^ (Livello + 1))`
Ogni livello aggiunge slot membri extra (`SlotQuantityGainForLevel`).

---

## 7. Diplomazia

Due stati principali implementati:

1. **Neutrale**: Default. Danni PvP attivi.
2. **Alleato (Ally)**: Danni PvP disabilitati. Accesso alla chat alleanza.
    * *Nota: Attualmente la richiesta di alleanza è asimmetrica o parzialmente implementata nel comando ("Alliance
      request sent (Not implemented fully)"). Impostare lo stato forza la relazione.*

---

## 8. Comandi Completi

Prefisso base: `/faction`

| Comando        | Argomenti          | Descrizione                                    |
|:---------------|:-------------------|:-----------------------------------------------|
| **Generale**   |                    |                                                |
| `create`       | `<nome>`           | Crea un nuovo gruppo.                          |
| `disband`      |                    | Scioglie il gruppo (Solo Leader).              |
| `leave`        |                    | Abbandona il gruppo.                           |
| `info`         | `[nome]`           | Info su gruppo, banca, power, ecc.             |
| `reload`       |                    | Ricarica config (Richiede Creative).           |
| **Membri**     |                    |                                                |
| `invite`       | `<player>`         | Invita un giocatore.                           |
| `kick`         | `<player>`         | Espelle un membro.                             |
| **Ruoli**      |                    |                                                |
| `role create`  | `<nome> [perms]`   | Crea ruolo (es. `role create Mod CAN_INVITE`). |
| `role set`     | `<player> <ruolo>` | Assegna ruolo.                                 |
| `role delete`  | `<nome>`           | Cancella ruolo.                                |
| `role list`    |                    | Lista ruoli e permessi.                        |
| **Territorio** |                    |                                                |
| `claim`        |                    | Rivendica chunk corrente.                      |
| `unclaim`      |                    | Rimuove claim corrente.                        |
| `home`         | `<nome>`           | Teletrasporto.                                 |
| `sethome`      | `<nome>`           | Crea punto home.                               |
| `listhomes`    |                    | Lista homes.                                   |
| `map`          |                    | Toggle mappa visuale claims.                   |
| **Economia**   |                    |                                                |
| `deposit`      | `<amount>`         | Deposita soldi.                                |
| `withdraw`     | `<amount>`         | Preleva soldi.                                 |
| `balance`      |                    | Mostra saldo.                                  |
| `upgrade`      |                    | (GUILD) Livella la gilda.                      |
| **Diplomazia** |                    |                                                |
| `diplomacy`    | `<gruppo> <stato>` | Imposta relazione (ALLY, NEUTRAL, ENEMY).      |
| **Chat**       |                    |                                                |
| `chat group`   | `<msg>`            | Invia messaggio ai membri.                     |
| `chat ally`    | `<msg>`            | Invia messaggio agli alleati.                  |

---

## 9. Struttura Tecnica (Sviluppatori)

### Architettura

* **Service Singleton**: `GroupService` gestisce tutta la logica.
* **Persistenza**: `JsonStorage` salva su file JSON locale.
* **Eventi ECS**:
    * `PvPProtectionSystem`: Blocca danni tra membri/alleati.
    * `ClaimProtectionSystems`: Blocca interazioni nei chunk protetti.

### Note Importanti

* **Tasse**: Il parametro `EnableTax` nel config non ha effetto nel codice attuale (manca il task schedulato).
* **Admin**: Non esiste un sistema di permessi "operator" (op) integrato. I comandi amministrativi si basano sulla
  Gamemode o non sono presenti.
