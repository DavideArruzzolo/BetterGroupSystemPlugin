# Piano di Test per BetterGroupSystemPlugin

Questo documento descrive i casi di test per i comandi del plugin, ordinati per categoria funzionale e numero di
giocatori richiesti.

---

## 1. Gestione del Gruppo

### 1.1. Test Individuali (1 Giocatore)

| Comando                         | Permesso Richiesto      | Azione da Testare (Happy Path)                                                     | Azioni da Testare (Casi Limite e Negativi)                                                                                                                                          |
|:--------------------------------|:------------------------|:-----------------------------------------------------------------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `/group create <nome> <tag>`    | Nessuno                 | Creare un nuovo gruppo con nome e tag validi.                                      | 1. Tentare di creare un gruppo quando si è già membri di un altro.<br>2. Usare nomi/tag già esistenti.<br>3. Usare nomi/tag con caratteri non validi, troppo lunghi o troppo corti. |
| `/group update <tipo> <valore>` | `CAN_MANAGE_GROUP_INFO` | Modificare descrizione, tag, e colore del proprio gruppo.                          | 1. Tentare di usare un colore non valido (es. non esadecimale).<br>2. Tentare di impostare un nome/tag già in uso.                                                                  |
| `/group info`                   | Nessuno                 | Visualizzare le informazioni del proprio gruppo.                                   | 1. Eseguire il comando senza essere in un gruppo (dovrebbe notificarlo).                                                                                                            |
| `/group leave`                  | Nessuno                 | Lasciare un gruppo di cui si è l'unico membro (il gruppo dovrebbe essere sciolto). | -                                                                                                                                                                                   |
| `/group delete`                 | Leader                  | Cancellare il proprio gruppo.                                                      | -                                                                                                                                                                                   |
| `/group upgrade`                | `CAN_UPGRADE_GUILD`     | Eseguire l'upgrade del proprio gruppo (solo Gilde).                                | 1. Tentare l'upgrade senza fondi sufficienti.<br>2. Tentare l'upgrade al livello massimo.                                                                                           |

### 1.2. Test di Gruppo (2+ Giocatori)

| Comando                    | Permesso Richiesto | Giocatori | Azione da Testare (Happy Path)                                          | Azioni da Testare (Casi Limite e Negativi)                                                                                 |
|:---------------------------|:-------------------|:----------|:------------------------------------------------------------------------|:---------------------------------------------------------------------------------------------------------------------------|
| `/group leave`             | Nessuno            | P2        | Un membro (non leader) lascia il gruppo.                                | 1. **P1 (Leader)** tenta di lasciare un gruppo con altri membri (dovrebbe fallire e chiedere di trasferire la leadership). |
| `/group info <nome>`       | Nessuno            | P2        | Un giocatore esterno visualizza le informazioni di un gruppo esistente. | 1. Richiedere info per un gruppo che non esiste.                                                                           |
| `/group transfer <membro>` | Leader             | P1 -> P2  | **P1 (Leader)** trasferisce la proprietà del gruppo a **P2**.           | 1. **P2 (Membro)** tenta di trasferire la proprietà a **P1**.                                                              |

---

## 2. Gestione dei Membri

### 2.1. Test Individuali (1 Giocatore)

| Comando              | Permesso Richiesto | Azione da Testare (Happy Path)                                                     | Azioni da Testare (Casi Limite e Negativi) |
|:---------------------|:-------------------|:-----------------------------------------------------------------------------------|:-------------------------------------------|
| `/group invitations` | Nessuno            | Controllare la lista di inviti (dovrebbe essere vuota o mostrare inviti pendenti). | -                                          |

### 2.2. Test di Gruppo (2+ Giocatori)

| Comando                     | Permesso Richiesto | Giocatori | Azione da Testare (Happy Path)                      | Azioni da Testare (Casi Limite e Negativi)                                                                                                                                                                            |
|:----------------------------|:-------------------|:----------|:----------------------------------------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `/group invite <giocatore>` | `CAN_INVITE`       | P1 -> P2  | **P1** invita **P2** (online e offline) nel gruppo. | 1. **P2 (senza permesso)** tenta di invitare **P3**.<br>2. **P1** invita **P2** quando il gruppo è pieno.<br>3. **P1** invita **P2** che è già nel gruppo.<br>4. **P1** invita un giocatore inesistente.              |
| `/group accept <gruppo>`    | Nessuno            | P2        | **P2** accetta l'invito e si unisce al gruppo.      | 1. **P2** tenta di accettare un invito per un gruppo inesistente o che ha ritirato l'invito.                                                                                                                          |
| `/group refuse <gruppo>`    | Nessuno            | P2        | **P2** rifiuta un invito.                           | -                                                                                                                                                                                                                     |
| `/group kick <membro>`      | `CAN_KICK`         | P1 -> P2  | **P1** espelle **P2** (membro online e offline).    | 1. **P2 (senza permesso)** tenta di espellere **P1**.<br>2. **P1** tenta di espellere un membro con un ruolo di priorità superiore o uguale (se non è leader).<br>3. **P1** tenta di espellere un membro inesistente. |

---

## 3. Gestione dei Ruoli e Permessi

### 3.1. Test Individuali (1 Giocatore)

| Comando                      | Permesso Richiesto | Azione da Testare (Happy Path)                      | Azioni da Testare (Casi Limite e Negativi)                                                                                 |
|:-----------------------------|:-------------------|:----------------------------------------------------|:---------------------------------------------------------------------------------------------------------------------------|
| `/group role create <nome>`  | `CAN_MANAGE_ROLES` | Creare un nuovo ruolo.                              | 1. Tentare di creare un ruolo con un nome già esistente.                                                                   |
| `/group role delete <ruolo>` | `CAN_MANAGE_ROLES` | Cancellare un ruolo personalizzato.                 | 1. Tentare di cancellare un ruolo di default.<br>2. Tentare di cancellare un ruolo a cui sono ancora assegnati dei membri. |
| `/group role update <ruolo>` | `CAN_MANAGE_ROLES` | Aggiungere/rimuovere permessi da un ruolo.          | 1. Tentare di modificare un ruolo di default (se non permesso).                                                            |
| `/group role list`           | Nessuno            | Visualizzare la lista dei ruoli del proprio gruppo. | -                                                                                                                          |

### 3.2. Test di Gruppo (2+ Giocatori)

| Comando                   | Permesso Richiesto   | Giocatori | Azione da Testare (Happy Path) | Azioni da Testare (Casi Limite e Negativi)                                                                                                           |
|:--------------------------|:---------------------|:----------|:-------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `/group promote <membro>` | `CAN_PROMOTE_DEMOTE` | P1 -> P2  | **P1** promuove **P2**.        | 1. **P2 (senza permesso)** tenta di promuovere **P1**.<br>2. **P1** tenta di promuovere **P2** a un ruolo di priorità uguale o superiore al proprio. |
| `/group demote <membro>`  | `CAN_PROMOTE_DEMOTE` | P1 -> P2  | **P1** retrocede **P2**.       | 1. **P2 (senza permesso)** tenta di retrocedere **P1**.                                                                                              |

---

## 4. Gestione Territori e Home

### 4.1. Test Individuali (1 Giocatore)

| Comando                    | Permesso Richiesto  | Azione da Testare (Happy Path)                              | Azioni da Testare (Casi Limite e Negativi)                                                                |
|:---------------------------|:--------------------|:------------------------------------------------------------|:----------------------------------------------------------------------------------------------------------|
| `/group claim`             | `CAN_MANAGE_CLAIMS` | Rivendicare un chunk libero.                                | 1. Tentare di claimare un chunk già occupato.<br>2. Tentare di superare il limite di claim del gruppo.    |
| `/group unclaim`           | `CAN_MANAGE_CLAIMS` | Rimuovere la rivendicazione da un chunk del proprio gruppo. | 1. Tentare di unclaimare un chunk che non appartiene al proprio gruppo.                                   |
| `/group sethome <nome>`    | `CAN_MANAGE_HOMES`  | Impostare una home in un chunk claimato.                    | 1. Tentare di impostare una home fuori dal territorio claimato.<br>2. Superare il numero massimo di home. |
| `/group home <nome>`       | `CAN_TELEPORT_HOME` | Teletrasportarsi a una home esistente.                      | 1. Usare `/group home` quando nessuna home è impostata.                                                   |
| `/group delhome <nome>`    | `CAN_MANAGE_HOMES`  | Cancellare una home.                                        | 1. Tentare di cancellare una home inesistente.                                                            |
| `/group setdefault <nome>` | Nessuno             | Impostare una home di default per il teletrasporto.         | -                                                                                                         |

### 4.2. Test di Gruppo (2+ Giocatori)

| Comando               | Permesso Richiesto      | Giocatori | Azione da Testare (Happy Path)                                                       | Azioni da Testare (Casi Limite e Negativi)                                                                                                                            |
|:----------------------|:------------------------|:----------|:-------------------------------------------------------------------------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Interazione nel claim | `CAN_INTERACT_IN_CLAIM` | P2        | **P2 (membro con permesso)** interagisce (piazza/rompe blocchi) nel claim di **P1**. | 1. **P3 (esterno)** tenta di interagire nel claim di **P1** (dovrebbe essere bloccato).<br>2. **P2 (membro senza permesso)** tenta di interagire nel claim di **P1**. |

---

## 5. Gestione Economia

### 5.1. Test Individuali (1 Giocatore)

| Comando                      | Permesso Richiesto | Azione da Testare (Happy Path)               | Azioni da Testare (Casi Limite e Negativi)                                                                        |
|:-----------------------------|:-------------------|:---------------------------------------------|:------------------------------------------------------------------------------------------------------------------|
| `/group deposit <quantità>`  | Nessuno            | Depositare denaro nella banca del gruppo.    | 1. Depositare un importo negativo, zero o non numerico.<br>2. Depositare più denaro di quanto si possiede.        |
| `/group withdraw <quantità>` | `CAN_MANAGE_BANK`  | Prelevare denaro dalla banca del gruppo.     | 1. Prelevare un importo negativo, zero o non numerico.<br>2. Prelevare più denaro di quanto presente nella banca. |
| `/group balance`             | Nessuno            | Controllare il saldo della banca del gruppo. | -                                                                                                                 |

### 5.2. Test di Gruppo (2+ Giocatori)

| Comando                      | Permesso Richiesto | Giocatori | Azione da Testare (Happy Path)               | Azioni da Testare (Casi Limite e Negativi)                   |
|:-----------------------------|:-------------------|:----------|:---------------------------------------------|:-------------------------------------------------------------|
| `/group withdraw <quantità>` | `CAN_MANAGE_BANK`  | P2        | **P2 (membro con permesso)** preleva denaro. | 1. **P2 (membro senza permesso)** tenta di prelevare denaro. |

---

## 6. Gestione Diplomazia

### 6.1. Test di Gruppo (2+ Giocatori in 2 gruppi separati)

| Comando                              | Permesso Richiesto     | Giocatori                 | Azione da Testare (Happy Path)                                                | Azioni da Testare (Casi Limite e Negativi)                                                                                                                              |
|:-------------------------------------|:-----------------------|:--------------------------|:------------------------------------------------------------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `/group diplomacy <gruppo> <status>` | `CAN_MANAGE_DIPLOMACY` | P1 (Gruppo A) -> Gruppo B | **P1** imposta una relazione (es. `ENEMY`) con il **Gruppo B**.               | 1. **P2 (membro senza permesso)** tenta di modificare la diplomazia.<br>2. Impostare una relazione con un gruppo inesistente.<br>3. Impostare una relazione non valida. |
| `/group diplolist`                   | Nessuno                | P1, P2                    | **P1** e **P2** visualizzano le relazioni diplomatiche dei rispettivi gruppi. | -                                                                                                                                                                       |
