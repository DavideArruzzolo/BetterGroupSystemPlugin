# Documentazione Check Gruppi

Elenco dettagliato di tutti i controlli eseguiti da `GroupService.java` prima dell'esecuzione dei comandi.

## 1. Comandi Generali

| Comando | Check Eseguiti | Descrizione |
| :--- | :--- | :--- |
| **Tutti (Pre-requisiti)** | `playerGroupMap.containsKey(uuid)` | Verifica se il giocatore è in un gruppo (O(1) lookup). |
| | `getGroup(uuid) != null` | Verifica l'integrità dei dati (il gruppo esiste in memoria). |
| | `checkPerm(perm)` | Se non è leader, verifica se il ruolo ha il permesso specifico (Enum Permission). |

## 2. Gestione Gruppo

| Comando | Check | Descrizione |
| :--- | :--- | :--- |
| `create` | `!inGroup` | Il giocatore non deve essere già in un gruppo. |
| | `validateName/Tag` | Lunghezza (Min/Max config), Caratteri (Alfanumerici), Unicità (Cache globale). |
| | `color` | Formato Hex (regex) se fornito. |
| | `desc` | Lunghezza massima (config). |
| `delete` | `isLeader` | Solo il leader può cancellare il gruppo. |
| | `confirmed` | Richiede conferma esplicita (bool flag). |
| `update` | `CAN_UPDATE_GROUP` | Permesso del ruolo. |
| | `validate[Field]` | Stessi controlli di `create` (unicità, lunghezza) per il campo specifico. |
| `leave` | `!isLeader || members == 1` | Il leader non può uscire se ci sono altri membri (deve fare transfer). |

## 3. Gestione Membri

| Comando | Check | Descrizione |
| :--- | :--- | :--- |
| `invite` | `CAN_INVITE` | Permesso. |
| | `!targetInGroup` | Il target non deve essere in un altro gruppo. |
| | `size < maxSize` | Il gruppo non ha raggiunto il limite membri (Config). |
| `accept` | `!inGroup` | Chi accetta non deve essere in un gruppo. |
| | `hasInvitation` | Deve esistere un invito pendente nella mappa inviti. |
| | `size < maxSize` | Ricontrolla se il gruppo si è riempito nel frattempo. |
| `kick` | `CAN_KICK` | Permesso. |
| | `targetInGroup` | Il target deve essere nel gruppo. |
| | `!self` | Non ci si può autokickare. |
| | `priority(sender) > priority(target)` | Non puoi kickare pari grado o superiori. |
| `transfer` | `isLeader` | Solo il leader attuale può trasferire. |
| | `targetInGroup` | Il nuovo leader deve essere membro. |
| | `confirmed` | Richiede conferma. |

## 4. Ruoli

| Comando | Check | Descrizione |
| :--- | :--- | :--- |
| `role create` | `CAN_MANAGE_ROLE` | Permesso. |
| | `count < 10` | Hardcap per prevenire spam di ruoli (ottimizzazione). |
| | `nameUnique` | Nome ruolo univoco nel gruppo. |
| `role delete` | `CAN_MANAGE_ROLE` | Permesso. |
| | `!isDefault` | I ruoli default non si cancellano. |
| | `!isInUse` | Nessun membro deve avere quel ruolo al momento. |
| `set role` | `CAN_CHANGE_ROLE` | Permesso. |
| | `hierarchy` | Non puoi promuovere qualcuno a un grado >= al tuo (tranne se sei Leader). |

## 5. Territorio e Economia

| Comando | Check | Descrizione |
| :--- | :--- | :--- |
| `sethome` | `CAN_MANAGE_HOME` | Permesso. |
| | `count < maxHomes` | Limite config. |
| | `inClaimedChunk` | La location deve essere dentro un chunk del gruppo. |
| `claim` | `CAN_MANAGE_CLAIM` | Permesso. |
| | `!globalClaimed` | Il chunk non deve essere di NESSUN altro gruppo. |
| | `power/limit` | Faction: Potere sufficiente (`ClaimRatio`). Guild: Limite numerico fisso. |
| `withdraw` | `CAN_MANAGE_BANK` | Permesso. |
| | `amount > 0` | Valore positivo. |
| | `balance >= amount` | Fondi sufficienti. |
| `upgrade` | `isGuild` | Solo per modalità Guild. |
| | `!maxLevel` | Livello < Config max level. |
| | `balance >= cost` | Costo calcolato esponenzialmente (Config). |