# Piano di Test - BetterGroupSystemPlugin

## 1. Test Solitari (1 Persona)

Questi test possono essere eseguiti da un singolo giocatore (preferibilmente Leader/Admin) per verificare le
funzionalità di base.

### Gestione Base

- [ ] **Creazione Gruppo**: `/faction create <nome>`
    - *Verifica*: Creazione avvenuta, soldi scalati, messaggio conferma.
- [ ] **Info Gruppo**: `/faction info`
    - *Verifica*: Mostra nome, membri, power, banca.
- [ ] **Disband**: `/faction disband`
    - *Verifica*: Il gruppo viene cancellato, tutti i membri rimossi.
- [ ] **Abbandono**: `/faction leave`
    - *Verifica*: Funziona per membri non-leader; se Leader, verificare se blocca o passa lead.

### Territorio & Home

- [ ] **Claim**: `/faction claim` in un chunk libero.
    - *Verifica*: Il chunk diventa della fazione.
- [ ] **Unclaim**: `/faction unclaim` in un chunk posseduto.
    - *Verifica*: Il chunk viene liberato.
- [ ] **Set Home**: `/faction sethome <nome>`
    - *Verifica*: Home creata alle coordinate correnti.
- [ ] **Home Teleport**: Allontanarsi e usare `/faction home <nome>`.
    - *Verifica*: Teletrasporto corretto.
- [ ] **List Homes**: `/faction listhomes`
    - *Verifica*: Elenco corretto delle home.

### Economia

- [ ] **Deposit**: `/faction deposit 10` (o altra cifra).
    - *Verifica*: Soldi rimossi dal giocatore e aggiunti alla banca fazione (`/faction info` o `/faction balance`).
- [ ] **Withdraw**: `/faction withdraw 10`.
    - *Verifica*: Soldi rimossi dalla banca e aggiunti al giocatore.
    - *Test Negativo*: Provare a prelevare più di quanto c'è in banca.

### Ruoli

- [ ] **Crea Ruolo**: `/faction role create TestRole CAN_INVITE`
    - *Verifica*: Ruolo creato.
- [ ] **Lista Ruoli**: `/faction role list`
    - *Verifica*: Il ruolo appare.
- [ ] **Cancella Ruolo**: `/faction role delete TestRole`
    - *Verifica*: Ruolo rimosso.

---

## 2. Test di Coppia (2 Persone: Leader + Membro)

Richiede due giocatori nello stesso server.
**Attori**: Giocatore A (Leader), Giocatore B (Recluta).

### Gestione Membri

- [ ] **Invito**: A invita B (`/faction invite B`).
    - *Verifica*: B riceve la notifica.
- [ ] **Kick**: A espelle B (`/faction kick B`).
    - *Verifica*: B non è più nel gruppo.
- [ ] **Rientro**: Reinviare B nel gruppo per i test successivi.

### Permessi & Gerarchia

- [ ] **Assegnazione Ruolo**: A assegna un ruolo a B (`/faction role set B <ruolo>`).
    - *Verifica*: B ottiene il ruolo.
- [ ] **Test Permesso Negativo**: Togliere permesso `CAN_MANAGE_BANK` a B. B prova a fare `/faction withdraw`.
    - *Verifica*: Accesso negato.
- [ ] **Test Permesso Positivo**: Dare permesso `CAN_MANAGE_BANK` a B. B prova a fare `/faction withdraw`.
    - *Verifica*: Successo.

### Interazione

- [ ] **Chat Gruppo**: `/faction chat group <msg>`
    - *Verifica*: Solo A e B vedono il messaggio.
- [ ] **Friendly Fire**: A colpisce B all'interno e all'esterno del claim.
    - *Verifica*: Nessun danno se nella stessa fazione.
- [ ] **Map Tracker**: A apre la mappa.
    - *Verifica*: A vede la posizione di B (marker).

---

## 3. Test Multi-Gruppo / PvP (3+ Persone o 2 Gruppi)

Richiede almeno 2 fazioni attive.
**Attori**: Fazione Alpha (A), Fazione Beta (B).

### Diplomazia

- [ ] **Stato Neutrale**: Default.
    - *Verifica*: A e B possono farsi danno PvP.
- [ ] **Richiesta Alleanza**: A usa `/faction diplomacy Beta ALLY`.
    - *Verifica*: Messaggio inviato (o stato settato se asimmetrico come da note).
- [ ] **Stato Alleato**: Se entrambi settano ALLY.
    - *Verifica*: PvP disabilitato tra membri di Alpha e Beta.
- [ ] **Chat Alleanza**: `/faction chat ally <msg>`
    - *Verifica*: Membri di Alpha e Beta leggono il messaggio.
- [ ] **Stato Nemico**: Settare ENEMY.
    - *Verifica*: PvP attivo (ed eventuali meccaniche extra se presenti).

### Conflitto & Raiding (Solo modalità FACTION)

- [ ] **Protezione Claim**: B prova a rompere blocchi nel claim di Alpha.
    - *Verifica*: Azione bloccata.
- [ ] **Perdita Power**: Uccidere ripetutamente un membro di Alpha.
    - *Verifica*: Il Power di Alpha scende (`/faction info`).
- [ ] **Condizione Raidable**: Portare Power di Alpha < Numero Claims di Alpha.
    - *Verifica*: Stato "Raidable" attivo.
- [ ] **Esecuzione Raid**: B entra nel claim di Alpha mentre è Raidable.
    - *Verifica*: B può rompere blocchi / interagire (protezione disabilitata).
    - *Verifica*: Alpha riceve notifica "Danger".

---

## 4. Test Amministrativi / Edge Cases

- [ ] **Reload**: `/faction reload` (in Creative).
    - *Verifica*: Configurazione ricaricata senza crash.
- [ ] **Persistenza**: Riavviare il server.
    - *Verifica*: Gruppi, claims e soldi sono ancora presenti al riavvio.
- [ ] **Limiti**: Provare a invitare membri oltre il `MaxSize`.
    - *Verifica*: Errore o blocco invito.
