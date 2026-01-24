# Piano di Test per i Comandi del Plugin

Questo documento descrive i casi di test per i comandi del plugin, ordinati per area funzionale e numero di giocatori
richiesti.

---

## 1. Test Funzionali di Base ("Happy Path")

### 1.1. Gestione del Gruppo

**1 Giocatore (Online):**
| Comando | Azione da Testare |
| :--- | :--- |
| `/creategroup <nome>` | Creare un nuovo gruppo. |
| `/update` | Modificare le impostazioni del proprio gruppo (es. descrizione, tag). |
| `/upgrade` | Eseguire l'upgrade del proprio gruppo. |
| `/info <gruppo>` | Visualizzare le informazioni del proprio gruppo. |
| `/delete` | Cancellare il proprio gruppo. |
| `/leave` | Lasciare un gruppo di cui si è l'unico membro (dovrebbe cancellare il gruppo). |

**2+ Giocatori (Online):**
| Comando | Azione da Testare |
| :--- | :--- |
| `/leave` | Un membro (non proprietario) lascia il gruppo. |
| `/info <gruppo>` | Un giocatore esterno visualizza le informazioni di un gruppo esistente. |

### 1.2. Gestione dei Membri

**1 Giocatore (Online):**
| Comando | Azione da Testare |
| :--- | :--- |
| `/listinvitations` | Controllare la lista di inviti (dovrebbe essere vuota). |

**2+ Giocatori:**
| Comando | Azione da Testare |
| :--- | :--- |
| `/invite <giocatore>` | **Player 1** invita **Player 2** (online) nel gruppo. |
| `/invite <giocatore>` | **Player 1** invita **Player 2** (offline) nel gruppo. |
| `/accept <gruppo>` | **Player 2** accetta l'invito e si unisce al gruppo. |
| `/kick <membro>` | **Player 1** (owner) espelle **Player 2** (membro online). |
| `/kick <membro>` | **Player 1** (owner) espelle **Player 2** (membro offline). |
| `/transfer <membro>` | **Player 1** (owner) trasferisce la proprietà del gruppo a **Player 2**. |
| `/listinvitations` | **Player 2** visualizza l'invito ricevuto da **Player 1**. |

### 1.3. Gestione dei Ruoli

**1 Giocatore (Online):**
| Comando | Azione da Testare |
| :--- | :--- |
| `/create_role <nome>` | Creare un nuovo ruolo nel proprio gruppo. |
| `/list_roles` | Visualizzare la lista dei ruoli del proprio gruppo. |
| `/delete_role <ruolo>` | Cancellare un ruolo esistente nel proprio gruppo. |

**2+ Giocatori (Online):**
| Comando | Azione da Testare |
| :--- | :--- |
| `/set_role <membro> <ruolo>` | **Player 1** (owner) assegna un ruolo a **Player 2**. |

### 1.4. Gestione Territori e Home

**1 Giocatore (Online):**
| Comando | Azione da Testare |
| :--- | :--- |
| `/claim` | Rivendicare un territorio per il proprio gruppo. |
| `/unclaim` | Rimuovere la rivendicazione da un territorio del proprio gruppo. |
| `/sethome` | Impostare la home del gruppo nella posizione attuale. |
| `/home` | Teletrasportarsi alla home del gruppo. |
| `/delhome` | Cancellare la home del gruppo. |

### 1.5. Gestione Economia

**1 Giocatore (Online):**
| Comando | Azione da Testare |
| :--- | :--- |
| `/getbalance` | Controllare il proprio saldo personale. |
| `/deposit <quantità>` | Depositare denaro nella banca del gruppo. |
| `/getgroupbalance` | Controllare il saldo della banca del gruppo. |
| `/withdraw <quantità>` | Prelevare denaro (come owner con pieni permessi). |

**2+ Giocatori (Online):**
| Comando | Azione da Testare |
| :--- | :--- |
| `/withdraw <quantità>` | **Player 2** (membro con permessi limitati) tenta di prelevare (testare successo e fallimento
in base ai permessi del ruolo). |

### 1.6. Gestione Diplomazia

**2+ Giocatori (Online, in 2 gruppi separati):**
| Comando | Azione da Testare |
| :--- | :--- |
| `/diplomacy <gruppo> <relazione>` | **Player 1** (owner Gruppo A) imposta una relazione (es. `ALLY`) con il **Gruppo B
**. |
| `/listdiplomacy` | **Player 1** e **Player 2** visualizzano le relazioni diplomatiche dei rispettivi gruppi. |

---

## 2. Test Avanzati (Negativi e Casi Limite)

### 2.1. Gestione del Gruppo

| Priorità | Giocatori | Comando               | Azione da Testare (Scenario Negativo/Limite)                     |
|:---------|:----------|:----------------------|:-----------------------------------------------------------------|
| **Alta** | 1         | `/creategroup <nome>` | Tentare di creare un gruppo con un nome già esistente.           |
| **Alta** | 1         | `/creategroup <nome>` | Tentare di creare un gruppo quando si è già in un altro gruppo.  |
| Media    | 1         | `/creategroup <nome>` | Usare nomi con caratteri speciali, spazi, o troppo lunghi/corti. |
| Media    | 1         | `/info <gruppo>`      | Richiedere info per un gruppo che non esiste.                    |

### 2.2. Gestione dei Membri

| Priorità | Giocatori | Comando               | Azione da Testare (Scenario Negativo/Limite)                                                       |
|:---------|:----------|:----------------------|:---------------------------------------------------------------------------------------------------|
| **Alta** | 2+        | `/invite <giocatore>` | **Player 2** (membro senza permessi) tenta di invitare **Player 3**.                               |
| **Alta** | 2         | `/invite <giocatore>` | **Player 1** tenta di invitare **Player 2** quando il gruppo è al massimo della capienza.          |
| **Alta** | 2         | `/invite <giocatore>` | **Player 1** tenta di invitare **Player 2** che è già nel gruppo.                                  |
| **Alta** | 2         | `/kick <membro>`      | **Player 2** (membro senza permessi) tenta di espellere **Player 1** (owner).                      |
| **Alta** | 2         | `/transfer <membro>`  | **Player 2** (membro senza permessi) tenta di trasferire la proprietà a **Player 1**.              |
| Media    | 2         | `/accept <gruppo>`    | **Player 2** tenta di accettare un invito per un gruppo che non esiste o che ha ritirato l'invito. |
| Media    | 1         | `/invite <giocatore>` | Tentare di invitare un giocatore inesistente.                                                      |
| Media    | 1         | `/kick <membro>`      | Tentare di espellere un membro inesistente.                                                        |

### 2.3. Gestione dei Ruoli

| Priorità | Giocatori | Comando                      | Azione da Testare (Scenario Negativo/Limite)                                          |
|:---------|:----------|:-----------------------------|:--------------------------------------------------------------------------------------|
| **Alta** | 2         | `/create_role <nome>`        | **Player 2** (membro senza permessi) tenta di creare un ruolo.                        |
| **Alta** | 1         | `/create_role <nome>`        | Tentare di creare un ruolo con un nome già esistente.                                 |
| **Alta** | 2         | `/set_role <membro> <ruolo>` | **Player 2** (membro senza permessi) tenta di assegnare un ruolo a **Player 1**.      |
| Media    | 1         | `/delete_role <ruolo>`       | Tentare di cancellare un ruolo predefinito (es. 'owner', 'member') se non consentito. |
| Media    | 1         | `/delete_role <ruolo>`       | Tentare di cancellare un ruolo a cui sono assegnati dei membri.                       |
| Media    | 1         | `/set_role <membro> <ruolo>` | Assegnare un ruolo a un membro inesistente o usare un ruolo inesistente.              |

### 2.4. Gestione Territori e Home

| Priorità | Giocatori | Comando    | Azione da Testare (Scenario Negativo/Limite)                                                |
|:---------|:----------|:-----------|:--------------------------------------------------------------------------------------------|
| **Alta** | 1         | `/claim`   | Tentare di rivendicare un territorio già occupato da un altro gruppo.                       |
| **Alta** | 1         | `/claim`   | Tentare di rivendicare più territori del limite consentito dall'upgrade del gruppo.         |
| **Alta** | 2         | `/unclaim` | **Player 2** (membro senza permessi) tenta di annullare la rivendicazione di un territorio. |
| Media    | 1         | `/home`    | Usare `/home` quando non è stata impostata.                                                 |
| Media    | 1         | `/delhome` | Usare `/delhome` quando non è stata impostata.                                              |

### 2.5. Gestione Economia

| Priorità | Giocatori | Comando                | Azione da Testare (Scenario Negativo/Limite)                    |
|:---------|:----------|:-----------------------|:----------------------------------------------------------------|
| **Alta** | 1         | `/deposit <quantità>`  | Depositare un importo negativo, non numerico o zero.            |
| **Alta** | 1         | `/deposit <quantità>`  | Depositare più denaro di quanto si possiede.                    |
| **Alta** | 1         | `/withdraw <quantità>` | Prelevare un importo negativo, non numerico o zero.             |
| **Alta** | 1         | `/withdraw <quantità>` | Prelevare più denaro di quanto presente nella banca del gruppo. |
| **Alta** | 2         | `/withdraw <quantità>` | **Player 2** (membro senza permessi) tenta di prelevare denaro. |

### 2.6. Gestione Diplomazia

| Priorità | Giocatori | Comando          | Azione da Testare (Scenario Negativo/Limite)                                                         |
|:---------|:----------|:-----------------|:-----------------------------------------------------------------------------------------------------|
| **Alta** | 2+        | `/diplomacy ...` | **Player 2** (membro senza permessi in Gruppo A) tenta di modificare la diplomazia con **Gruppo B**. |
| Media    | 2         | `/diplomacy ...` | Impostare una relazione con un gruppo inesistente.                                                   |
| Media    | 2         | `/diplomacy ...` | Impostare una relazione diplomatica non valida (es. `/diplomacy GruppoB AMICI`).                     |

---

## 3. Test di Regressione

I test di regressione non sono un elenco di nuovi casi, ma una **strategia**. Dopo ogni modifica significativa al
codice, è fondamentale rieseguire una selezione dei test più critici (quelli con priorità **Alta**) per assicurarsi che
le nuove modifiche non abbiano introdotto bug in funzionalità che prima erano stabili.

**Strategia di Regressione (da eseguire dopo ogni modifica):**

1. **Creazione e cancellazione gruppo:** Eseguire `/creategroup` e `/delete`.
2. **Invito e accettazione:** Eseguire `/invite` e `/accept`.
3. **Kick membro:** Eseguire `/kick`.
4. **Claim territorio:** Eseguire `/claim` e `/unclaim`.
5. **Deposito e prelievo:** Eseguire `/deposit` e `/withdraw` (come owner).
6. **Verifica Permessi Critici:** Verificare che un membro senza permessi **non possa** eseguire comandi come `/kick`,
   `/unclaim`, `/withdraw`.

Questa suite di regressione va eseguita dopo ogni aggiornamento del plugin per garantire la stabilità delle funzioni
principali.
