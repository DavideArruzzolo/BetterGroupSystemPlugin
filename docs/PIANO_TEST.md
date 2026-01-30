# Piano di Test - BetterGroupSystemPlugin

## 1. Test Solitari (1 Persona)

Questi test possono essere eseguiti da un singolo giocatore (preferibilmente Leader/Admin) per verificare le
funzionalità di base.

### Gestione Base

- [ ] **Creazione Gruppo**: `/faction create <nome>`
    - *Verifica*: Creazione avvenuta, soldi scalati, messaggio conferma.
    - *Argomenti*: `nome` (Obbligatorio).
- [ ] **Info Gruppo**: `/faction info [nomeGruppo]`
    - *Verifica*: Mostra nome, membri, power, banca.
    - *Argomenti*: `nomeGruppo` (Opzionale: se omesso mostra il proprio).
- [ ] **Disband**: `/faction disband`
    - *Verifica*: Il gruppo viene cancellato, tutti i membri rimossi.
    - *Argomenti*: Nessuno.
- [ ] **Abbandono**: `/faction leave`
    - *Verifica*: Funziona per membri non-leader; se Leader, verificare se blocca o passa lead.
    - *Argomenti*: Nessuno.
- [ ] **Upgrade**: `/faction upgrade`
    - *Verifica*: Aumenta il livello della gilda (se configurato) o mostra info upgrade.
    - *Argomenti*: Nessuno.

### Territorio & Home

- [ ] **Claim**: `/faction claim`
    - *Verifica*: Il chunk diventa della fazione.
    - *Argomenti*: Nessuno (Claima chunk attuale).
- [ ] **Unclaim**: `/faction unclaim`
    - *Verifica*: Il chunk viene liberato.
    - *Argomenti*: Nessuno (Unclaima chunk attuale).
- [ ] **Set Home**: `/faction setHome <nome>`
    - *Verifica*: Home creata alle coordinate correnti.
    - *Argomenti*: `nome` (Obbligatorio).
- [ ] **Home Teleport**: `/faction home <nome>`
    - *Verifica*: Teletrasporto corretto.
    - *Argomenti*: `nome` (Obbligatorio).
- [ ] **Remove Home**: `/faction deleteHome <nome>`
    - *Verifica*: Home rimossa correttamente.
    - *Argomenti*: `nome` (Obbligatorio).
- [ ] **Set Default Home**: `/faction setDefaultHome <nome>`
    - *Verifica*: Imposta la home predefinita per il teletrasporto rapido.
    - *Argomenti*: `nome` (Obbligatorio).
- [ ] **List Homes**: `/faction listHomes`
    - *Verifica*: Elenco corretto delle home.
    - *Argomenti*: Nessuno.
- [ ] **Claim Map**: `/faction claimMap`
    - *Verifica*: Mostra una mappa visuale dei claim circostanti nella chat.
    - *Argomenti*: Nessuno.

### Economia & Power

- [ ] **Deposit**: `/faction deposit <amount>`
    - *Verifica*: Soldi rimossi dal giocatore e aggiunti alla banca fazione.
    - *Argomenti*: `amount` (Obbligatorio: numero intero/decimale).
- [ ] **Withdraw**: `/faction withdraw <amount>`
    - *Verifica*: Soldi rimossi dalla banca e aggiunti al giocatore.
    - *Argomenti*: `amount` (Obbligatorio).
- [ ] **Balance**: `/faction balance`
    - *Verifica*: Mostra il saldo corretto della fazione.
    - *Argomenti*: Nessuno.
- [ ] **Power**: `/faction power [player]`
    - *Verifica*: Mostra il power corrente della fazione/giocatore.
    - *Argomenti*: `player` (Opzionale).

### Ruoli

- [ ] **Crea Ruolo**: `/faction createRole <nome> [permessi]`
    - *Verifica*: Ruolo creato con i permessi specificati.
    - *Argomenti*: `nome` (Obbligatorio), `permessi` (Opzionale: lista separata da spazi).
- [ ] **Lista Ruoli**: `/faction listRoles`
    - *Verifica*: Il ruolo appare nella lista.
    - *Argomenti*: Nessuno.
- [ ] **Imposta Ruolo**: `/faction setRole <player> <ruolo>`
    - *Verifica*: Il giocatore riceve il ruolo specificato.
    - *Argomenti*: `player` (Obbligatorio), `ruolo` (Obbligatorio).
- [ ] **Aggiorna Ruolo**: `/faction updateRole <nome> <nuovi_permessi>`
    - *Verifica*: I permessi del ruolo vengono aggiornati.
    - *Argomenti*: `nome` (Obbligatorio), `nuovi_permessi` (Obbligatorio).
- [ ] **Cancella Ruolo**: `/faction deleteRole <nome>`
    - *Verifica*: Ruolo rimosso.
    - *Argomenti*: `nome` (Obbligatorio).

---

## 2. Test di Coppia (2 Persone: Leader + Membro)

### Gestione Membri

- [ ] **Invito**: `/faction invite <player>`
    - *Verifica*: Il giocatore riceve la notifica.
    - *Argomenti*: `player` (Obbligatorio).
- [ ] **Accetta Invito**: `/faction acceptInvite <nomeGruppo>`
    - *Verifica*: Il giocatore entra nel gruppo.
    - *Argomenti*: `nomeGruppo` (Obbligatorio).
- [ ] **Lista Inviti**: `/faction invitations`
    - *Verifica*: Mostra gli inviti pendenti.
    - *Argomenti*: Nessuno.
- [ ] **Lista Membri**: `/faction members`
    - *Verifica*: Mostra lista membri e ruoli.
    - *Argomenti*: Nessuno.
- [ ] **Kick**: `/faction kick <player>`
    - *Verifica*: Il giocatore viene rimosso dal gruppo.
    - *Argomenti*: `player` (Obbligatorio).
- [ ] **Transfer Leadership**: `/faction transfer <player>`
    - *Verifica*: B diventa Leader, A diventa membro/ufficiale.
    - *Argomenti*: `player` (Obbligatorio).

---

## 3. Test Multi-Gruppo / PvP

### Diplomazia

- [ ] **Richiesta Alleanza**: `/faction diplomacy <gruppo> ALLY`
    - *Verifica*: Messaggio di richiesta inviato.
    - *Argomenti*: `gruppo` (Obbligatorio), `status` (Obbligatorio: ALLY).
- [ ] **Lista Richieste**: `/faction allyRequests`
    - *Verifica*: Vede la richiesta.
    - *Argomenti*: Nessuno.
- [ ] **Accetta Alleanza**: `/faction acceptAlly <gruppo>`
    - *Verifica*: Fazioni diventano Alleate.
    - *Argomenti*: `gruppo` (Obbligatorio).
- [ ] **Rifiuta Alleanza**: `/faction denyAlly <gruppo>`
    - *Verifica*: Alleanza rifiutata o sciolta.
    - *Argomenti*: `gruppo` (Obbligatorio).
- [ ] **Lista Diplomazia**: `/faction listDiplomacy`
    - *Verifica*: Mostra alleati e nemici.
    - *Argomenti*: Nessuno.
- [ ] **Stato Nemico**: `/faction diplomacy <gruppo> ENEMY`
    - *Verifica*: Relazione ostile impostata.
    - *Argomenti*: `gruppo` (Obbligatorio), `status` (Obbligatorio: ENEMY).

---

## 4. Test Amministrativi (Admin)

Comandi riservati agli operatori.

- [ ] **Admin Disband**: `/faction admin disband <gruppo>`
- [ ] **Admin Kick**: `/faction admin kick <gruppo> <player>`
- [ ] **Admin Set Leader**: `/faction admin setLeader <gruppo> <player>`
- [ ] **Admin Set Diplomacy**: `/faction admin setDiplomacy <gruppo1> <gruppo2> <relation>`
- [ ] **Admin Set Money**: `/faction admin setMoney <gruppo> <amount>`
- [ ] **Admin Set Player Money**: `/faction admin setPlayerMoney <player> <amount>`
- [ ] **Admin Grant Perm**: `/faction admin grantPerm <gruppo> <permesso>`
- [ ] **Admin Revoke Perm**: `/faction admin revokePerm <gruppo> <permesso>`
- [ ] **Admin Info**: `/faction admin info <gruppo>`

---

## 5. Test Non-Comandi & Funzionali

- [ ] **Join Event**: Entrare nel server (caricamento cache, chat format).
- [ ] **Quit Event**: Uscire dal server (salvataggio dati).
- [ ] **Chat Formatting**: Prefisso fazione in chat.
- [ ] **Riavvio Server**: Persistenza dati.
- [ ] **Reload Config**: `/faction reload` (ricarica configurazione).
