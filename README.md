# 🏰 BetterGroupSystemPlugin

> Un plugin completo per server Hytale sviluppato dal **DZVE Development Team** per la gestione avanzata di Fazioni, Gilde, territori, economia virtuale e diplomazia.

## 📋 Informazioni Generali
* **Linguaggio:** Java (toolchain Java 17+)
* **Database:** SQLite JDBC 3.45.1.0 (Modalità WAL per atomicità)
* **Build System:** Gradle con ProGuard per obfuscation

---

## ⚙️ Modalità di Gioco
Il plugin è altamente versatile e può operare in due modalità distinte, configurabili tramite il parametro `PluginMode`:

* **Modalità FACTION (Default):** Focalizzata sul PvP e sul Raiding. Utilizza un sistema di "Power" dinamico. Se il potere totale di una fazione scende al di sotto del numero di territori reclamati, la fazione diventa vulnerabile (Raidable).
* **Modalità GUILD:** Focalizzata sulla cooperazione pacifica. Non usa il sistema di potere, ma un sistema di progressione a "Livelli" acquistabili con la valuta in-game, i quali sbloccano slot aggiuntivi per i membri.

**Visibilità Mappa:** Tramite l'opzione `HidePlayers`, è possibile limitare la visibilità della mappa globale, mostrando ai giocatori solo i membri del proprio gruppo e i propri alleati.

---

## ⚔️ Meccaniche Principali

### Sistema Potere (Solo FACTION)
* **Raid:** Una fazione vulnerabile (`TotalPower < NumberOfClaims`) può subire il furto istantaneo dei propri territori tramite il comando `/faction claim` usato dai nemici.
* **Variazioni Power:** Si perde `-1.0` per ogni morte e si guadagna `+1.0` uccidendo membri di fazioni nemiche.
* **Rigenerazione:** Il potere si rigenera passivamente di `+0.001` al minuto se online, e `+0.0001` se offline (con limiti massimi/minimi di +/- 100).

### Territori ed Economia
* **Protezione:** I territori reclamati bloccano qualsiasi azione di rottura, piazzamento, uso e danno da parte dei non membri (`CAN_INTERACT_IN_CLAIM`).
* **Mappa Interattiva:** Digitando `/faction map` viene generata in chat una griglia 21x6 che mostra i territori propri (O), alleati (A), nemici (E) o selvaggi (-).
* **Banca Virtuale Chiusa:** L'economia è interna al plugin. Gli amministratori generano la valuta, mentre i giocatori possiedono un saldo personale che possono depositare o prelevare dalla tesoreria del gruppo.

---

## 🛠️ Configurazione (Parametri Principali)

| Parametro | Valore Default | Descrizione |
| :--- | :--- | :--- |
| `PluginMode` | "FACTION" | Modalità del server (FACTION o GUILD) |
| `AllCommandsPrefix` | "faction" | Prefisso per l'ecosistema dei comandi |
| `PlayerInitialPower` | 5.0 | Potere assegnato a un nuovo giocatore |
| `InitialPrice` | 2500.0 | Costo base per il primo livello di una Gilda |
| `LevelPriceMultiplier`| 1.2 | Moltiplicatore matematico per i livelli successivi |
| `SlotQuantityGainForLevel`| 10 | Numero di slot membri aggiunti per ogni livello |
| `MaxSize` | 10 | Dimensione massima consentita per un gruppo |

---

## 🔐 Permessi e Ruoli

Il sistema include gerarchie predefinite (**LEADER**, **OFFICER**, **MEMBER**) con bypass totale per il leader. È possibile creare ruoli **CUSTOM** assegnando permessi granulari:

| Nodo Permesso | Azione Consentita |
| :--- | :--- |
| `CAN_MANAGE_BANK` | Prelevare fondi dalla banca del gruppo |
| `CAN_UPDATE_GROUP` | Modificare nome, tag, descrizione e colore esadecimale |
| `CAN_INVITE` | Invitare nuovi giocatori |
| `CAN_KICK` | Espellere membri di grado inferiore |
| `CAN_MANAGE_ROLE` | Creare, modificare o eliminare ruoli |
| `CAN_MANAGE_HOME` | Creare o cancellare i punti di teletrasporto del gruppo |
| `CAN_MANAGE_CLAIM` | Reclamare chunk o liberare territori |
| `CAN_MANAGE_DIPLOMACY`| Accettare alleanze o dichiarare lo stato di nemico |

---

## 💻 API per Sviluppatori

Il plugin espone `BetterGroupEconomyAPI` per l'integrazione con altri sistemi. L'API è **Thread-Safe**, include l'**Auto-Persistence** (salvataggio automatico su DB) ed è strutturata per non essere intaccata dall'obfuscation di ProGuard.

```java
BetterGroupEconomyAPI api = BetterGroupEconomyAPI.getInstance();

// Gestione dell'economia del singolo giocatore
double balance = api.getMemberBalance(playerId);
api.depositMemberBalance(playerId, 50.0);

// Gestione dell'economia del gruppo
UUID groupId = api.getPlayerGroup(playerId);
api.withdrawGroupBalance(groupId, 200.0);
```
---

##📦 Build e Deployment
* **Costruzione**: Esegui ./gradlew build per compilare il progetto includendo l'obfuscation di ProGuard.
* **Test Locale**: Usa ./gradlew runServer.
* **Installazione**: Sposta il .jar compilato nella cartella Hytale corrispondente al tuo OS (es. %APPDATA%/Roaming/Hytale/UserData/Mods/ su Windows). È raccomandato schedulare backup periodici del file groups.db.
