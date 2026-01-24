# Analisi Configurazione e Controlli Mancanti

Analisi basata sul file `BetterGroupSystemPluginConfig.java` e `COMMAND_CHECKS.md`.

## 1. Proprietà di Configurazione Mancanti o Consigliate

Le seguenti proprietà sono utilizzate nel codice (hardcoded) o logiche necessarie ma non presenti nel file config
fornito:

| Proprietà Suggerita           | Valore Default  | Utilizzo                                                                                         |
|:------------------------------|:----------------|:-------------------------------------------------------------------------------------------------|
| `maxCustomRoles`              | `10`            | Limite massimo di ruoli personalizzati per evitare abusi di memoria. Attualmente hardcoded a 10. |
| `teleportDelay`               | `5` (secondi)   | Tempo di attesa prima del teletrasporto home (anti-combat escape).                               |
| `allowCrossDimensionTeleport` | `false`         | Se permettere TP alla home se il player è in un'altra dimensione (Nether/End).                   |
| `homeLimitScaling`            | `false`         | Se `true`, il limite home aumenta col livello della Gilda (attualmente fisso a `maxHome`).       |
| `inviteExpiration`            | `300` (secondi) | Tempo dopo il quale un invito scade automaticamente (necessita task schedulato).                 |

## 2. Check di Sicurezza e Logici Aggiuntivi

### A. Combat Logging (Critico)

Nel codice attuale c'è un placeholder commentato. È **fondamentale** implementare:

- **Check:** `isInCombat(player)` prima di comandi come `/group home` o `/group accept`.
- **Implementazione:** Richiede un `CombatManager` esterno che traccia l'ultimo danno ricevuto.

### B. Economia Reale

Attualmente `deposit` e `withdraw` gestiscono solo il bilancio interno del gruppo (`group.bankBalance`).

- **Manca:** Integrazione con l'economia del Player (es. togliere soldi dall'inventario/wallet del player su `deposit`).
- **Azione:** Iniettare un servizio `EconomyService` in `GroupService`.

### C. Chunk Loading

I controlli sui territori (`claim`, `sethome`) usano le coordinate.

- **Rischio:** Se un chunk è scaricato, `player.getLocation()` funziona, ma operazioni più complesse potrebbero fallire
  o causare lag.
- **Consiglio:** Assicurarsi che le operazioni siano Thread-Safe rispetto al caricamento del mondo Hytale.

### D. Offline Players

Molti comandi (`invite`, `kick`) assumono `PlayerRef`.

- **Problema:** `kick` dovrebbe funzionare anche se il target è offline. Attualmente `PlayerRef` spesso implica un
  giocatore online o una cache recente.
- **Soluzione:** Modificare le firme per accettare `String targetName`, risolvere l'UUID dal database offline, e
  procedere.