# Piano di Test Completo - BetterGroupSystemPlugin

## 1. Test Solitari (1 Persona - Leader/Admin)

Questi test verificano tutte le funzionalità base che possono essere eseguite da un singolo giocatore.

### Gestione Base Gruppo

- [X] **Creazione Gruppo Completa**: `/faction create <nome> [tag] [desc] [color]`
    - *Verifica*: Creazione con tutti i parametri opzionali, soldi scalati, messaggio conferma.
    - *Argomenti*: `nome` (Obbligatorio), `tag`/`desc`/`color` (Opzionali).
    - *Test Edge*: Nomi invalidi (caratteri speciali, lunghezza errata).
- [X] **Info Gruppo**: `/faction info [nomeGruppo]`
    - *Verifica*: Mostra nome, membri, power, banca, livello (per GUILD).
    - *Argomenti*: `nomeGruppo` (Opzionale: se omesso mostra il proprio).
- [X] **Disband Gruppo**: `/faction disband`
    - *Verifica*: Il gruppo viene cancellato, tutti i membri rimossi, territori liberati.
    - *Argomenti*: Nessuno.
- [X] **Abbandono Gruppo**: `/faction leave`
    - *Verifica*: Funziona per membri non-leader; se Leader, verifica blocco o passaggio lead.
    - *Argomenti*: Nessuno.
- [ ] **Upgrade Gilda**: `/faction upgrade`
    - *Verifica*: Aumenta livello gilda (solo modalità GUILD), costo calcolato correttamente.
    - *Argomenti*: Nessuno.
    - *Test Edge*: Fondi insufficienti, livello massimo raggiunto.

### Territorio & Claim System

- [X] **Claim Territorio**: `/faction claim`
    - *Verifica*: Il chunk diventa della fazione, controllo power/slot sufficienti.
    - *Argomenti*: Nessuno (Claima chunk attuale).
    - *Test Edge*: Power insufficiente (FACTION), slot esauriti (GUILD), claim massimo.
- [X] **Unclaim Territorio**: `/faction unclaim`
    - *Verifica*: Il chunk viene liberato, rimozione protezioni.
    - *Argomenti*: Nessuno (Unclaima chunk attuale).
- [X] **Mappa Claim**: `/faction map`
    - *Verifica*: Mostra mappa 21x6 chunk con simboli corretti (@, O, A, E, -).
    - *Verifica*: Mostra rappresentazione ASCII dei claim circostanti in chat.
    - *Argomenti*: Nessuno.

### Home System Completo

- [X] **Crea Home**: `/faction setHome <nome>`
    - *Verifica*: Home creata alle coordinate correnti, mondo salvato.
    - *Argomenti*: `nome` (Obbligatorio).
    - *Test Edge*: Nome duplicato, limite home raggiunto.
- [X] **Teletrasporto Home**: `/faction home <nome>`
    - *Verifica*: Teletrasporto corretto alle coordinate salvate.
    - *Argomenti*: `nome` (Obbligatorio).
    - *Test Edge*: Home inesistente, permessi mancanti.
- [X] **Elimina Home**: `/faction deleteHome <nome>`
    - *Verifica*: Home rimossa correttamente dal sistema.
    - *Argomenti*: `nome` (Obbligatorio).
- [X] **Imposta Home Default**: `/faction setDefaultHome <nome>`
    - *Verifica*: Imposta home predefinita per teletrasporto rapido.
    - *Argomenti*: `nome` (Obbligatorio).
- [X] **Lista Home**: `/faction listHomes`
    - *Verifica*: Elenco completo home con coordinate e mondo.
    - *Argomenti*: Nessuno.

### Economia & Power System

- [X] **Deposita Banca**: `/faction deposit <amount>`
    - *Verifica*: Soldi rimossi dal giocatore e aggiunti alla banca gruppo.
    - *Argomenti*: `amount` (Obbligatorio: numero intero/decimale).
    - *Test Edge*: Fondi insufficienti, amount negativo.
- [X] **Preleva Banca**: `/faction withdraw <amount>`
    - *Verifica*: Soldi rimossi dalla banca e aggiunti al giocatore.
    - *Argomenti*: `amount` (Obbligatorio).
    - *Test Edge*: Fondi insufficienti banca, permessi mancanti.
- [X] **Saldo Banca**: `/faction balance`
    - *Verifica*: Mostra il saldo corretto della banca gruppo.
    - *Argomenti*: Nessuno.
- [X] **Power Giocatore**: `/faction power [player]`
    - *Verifica*: Mostra power corrente giocatore/fazione (solo FACTION).
    - *Argomenti*: `player` (Opzionale).
    - *Test Edge*: Player inesistente, modalità GUILD.

### Sistema Ruoli Avanzato

- [ ] **Crea Ruolo Personalizzato**: `/faction createRole <nome> [permessi]`
    - *Verifica*: Ruolo creato con permessi specificati, salvato in database.
    - *Argomenti*: `nome` (Obbligatorio), `permessi` (Opzionale).
    - *Test Edge*: Nome duplicato, permessi invalidi.
- [X] **Lista Ruoli**: `/faction listRoles`
    - *Verifica*: Mostra tutti i ruoli con permessi associati.
    - *Argomenti*: Nessuno.
- [X] **Assegna Ruolo**: `/faction setRole <player> <ruolo>`
    - *Verifica*: Giocatore riceve ruolo con permessi corretti.
    - *Argomenti*: `player` (Obbligatorio), `ruolo` (Obbligatorio).
    - *Test Edge*: Player non nel gruppo, ruolo inesistente.
- [ ] **Modifica Ruolo**: `/faction updateRole <nome> <nuovi_permessi>`
    - *Verifica*: Permessi ruolo aggiornati, applicati immediatamente.
    - *Argomenti*: `nome` (Obbligatorio), `nuovi_permessi` (Obbligatorio).
- [X] **Elimina Ruolo**: `/faction deleteRole <nome>`
    - *Verifica*: Ruolo rimosso, membri con quel ruolo resettati a MEMBER.
    - *Argomenti*: `nome` (Obbligatorio).
    - *Test Edge*: Tentativo eliminazione ruoli di sistema (LEADER, MEMBER).

### Chat System

- [X] **Chat Gruppo**: `/faction chat group <message>`
    - *Verifica*: Messaggio visibile solo ai membri del gruppo.
    - *Argomenti*: `message` (Obbligatorio).
- [ ] **Chat Alleati**: `/faction chat ally <message>`
    - *Verifica*: Messaggio visibile a membri gruppi alleati.
    - *Argomenti*: `message` (Obbligatorio).
    - *Test Edge*: Nessun alleato, permessi mancanti.

---

## 2. Test di Coppia (2 Persone: Leader + Membro)

### Gestione Membri Completa

- [X] **Invita Giocatore**: `/faction invite <player>`
    - *Verifica*: Il giocatore riceve notifica, invito salvato.
    - *Argomenti*: `player` (Obbligatorio).
    - *Test Edge*: Giocatore già in gruppo, permessi mancanti.
- [X] **Accetta Invito**: `/faction acceptInvite <nomeGruppo>`
    - *Verifica*: Il giocatore entra nel gruppo, ruolo assegnato.
    - *Argomenti*: `nomeGruppo` (Obbligatorio).
    - *Test Edge*: Invito inesistente, gruppo pieno.
- [X] **Lista Inviti**: `/faction invitations`
    - *Verifica*: Mostra tutti gli inviti pendenti per il giocatore.
    - *Argomenti*: Nessuno.
- [X] **Lista Membri Dettagliata**: `/faction members`
    - *Verifica*: Mostra membri con ruoli, power, data join.
    - *Argomenti*: Nessuno.
- [X] **Espelli Membro**: `/faction kick <player>`
    - *Verifica*: Il giocatore viene rimosso, notifiche inviate.
    - *Argomenti*: `player` (Obbligatorio).
    - *Test Edge*: Espellere leader, espellere se stesso.
- [X] **Trasferisci Leadership**: `/faction transfer <player>`
    - *Verifica*: B diventa Leader, A diventa membro/ufficiale.
    - *Argomenti*: `player` (Obbligatorio).
    - *Test Edge*: Trasferimento a non-membro, a se stesso.

### Permessi e Protezioni

- [X] **Test Permessi**:
    - *Verifica*: Comandi bloccati con messaggio errore appropriato.
- [X] **Protezione Territorio**: Membro prova a costruire in claim nemico
    - *Verifica*: Blocco place/break/use con messaggio appropriato.

---

## 3. Test Multi-Gruppo / PvP (3+ Persone)

### Sistema Diplomazia Completo

- [ ] **Richiesta Alleanza**: `/faction diplomacy <gruppo> ALLY`
    - *Verifica*: Richiesta inviata, notifica al gruppo target.
    - *Argomenti*: `gruppo` (Obbligatorio), `status` (Obbligatorio).
- [ ] **Lista Richieste Alleanza**: `/faction allyRequests`
    - *Verifica*: Mostra tutte le richieste pendenti in entrata/uscita.
    - *Argomenti*: Nessuno.
- [ ] **Accetta Alleanza**: `/faction acceptAlly <gruppo>`
    - *Verifica*: Fazioni diventano alleate, benefici applicati.
    - *Argomenti*: `gruppo` (Obbligatorio).
- [ ] **Rifiuta Alleanza**: `/faction denyAlly <gruppo>`
    - *Verifica*: Alleanza rifiutata, richiesta rimossa.
    - *Argomenti*: `gruppo` (Obbligatorio).
- [ ] **Dichiara Nemico**: `/faction diplomacy <gruppo> ENEMY`
    - *Verifica*: Relazione ostile impostata, PvP abilitato.
    - *Argomenti*: `gruppo` (Obbligatorio).
- [ ] **Imposta Neutrale**: `/faction diplomacy <gruppo> NEUTRAL`
    - *Verifica*: Relazione neutrale ripristinata.
    - *Argomenti*: `gruppo` (Obbligatorio).
- [] **Lista Diplomazia Completa**: `/faction listDiplomacy`
    - *Verifica*: Mostra tutte le relazioni con stato e data.
    - *Argomenti*: Nessuno.

### Sistema Power e Raid (Solo FACTION)

- [ ] **Test Power System**: Uccisioni e morti tra giocatori fazioni diverse
    - *Verifica*: Power modificato correttamente (+1 kill, -1 morte).
    - *Test Edge*: Kill stesso giocatore, kill alleato.
- [ ] **Test Power Regen**: Verifica rigenerazione power online/offline
    - *Verifica*: Power aumenta gradualmente nel tempo.
- [ ] **Test Raidability**: Rendi fazione raidable (power < claims)
    - *Verifica*: Stato raidable mostrato in info, claim possibili da nemici.
- [ ] **Test Conquista Claim**: Nemico claim su territorio raidable
    - *Verifica*: Chunk trasferito istantaneamente al conquistatore.

### Protezioni e Interazioni

- [ ] **Protezione Alleati**: Alleato può costruire in claim amico
    - *Verifica*: Place/break/use permesso per alleati.
- [X] **Protezione Nemici**: Nemico non può interagire in claim
    - *Verifica*: Tutte le azioni bloccate per nemici.
- [ ] **PvP tra Alleati**: PvP disabilitato tra alleati (se configurato)
    - *Verifica**: Impossibile danneggiare alleati.
- [ ] **PvP tra Nemici**: PvP abilitato tra nemici
    - *Verifica**: Danni applicati correttamente.

---

## 4. Test Amministrativi Completi (Admin/OP)

### Gestione Admin Gruppi

- [X] **Admin Disband Forzato**: `/faction admin disband <gruppo>`
    - *Verifica*: Gruppo cancellato senza consenso leader.
    - *Argomenti*: `gruppo` (Obbligatorio).
- [X] **Admin Kick Forzato**: `/faction admin kick <gruppo> <player>`
    - *Verifica*: Giocatore rimosso senza permessi.
    - *Argomenti*: `gruppo` (Obbligatorio), `player` (Obbligatorio).
- [X] **Admin Set Leader**: `/faction admin setLeader <gruppo> <player>`
    - *Verifica*: Leadership trasferita forzatamente.
    - *Argomenti*: `gruppo` (Obbligatorio), `player` (Obbligatorio).
- [X] **Admin Info Gruppo**: `/faction admin info <gruppo>`
    - *Verifica*: Informazioni complete gruppo inclusi dati sensibili.
    - *Argomenti*: `gruppo` (Obbligatorio).

### Gestione Admin Economia

- [X] **Admin Set Money Gruppo**: `/faction admin setMoney <gruppo> <amount>`
    - *Verifica*: Saldo banca gruppo impostato forzatamente.
    - *Argomenti*: `gruppo` (Obbligatorio), `amount` (Obbligatorio).
- [X] **Admin Set Player Money**: `/faction admin setPlayerMoney <player> <amount>`
    - *Verifica*: **FONDAMENTALE**: Crea valuta nel sistema.
    - *Argomenti*: `player` (Obbligatorio), `amount` (Obbligatorio).
    - *Test Edge*: Amount negativo, player inesistente.

### Gestione Admin Permessi

- [ ] **Admin Grant Permesso Globale**: `/faction admin grantPerm <gruppo> <permesso>`
    - *Verifica*: Permesso concesso a tutto il gruppo.
    - *Argomenti*: `gruppo` (Obbligatorio), `permesso` (Obbligatorio).
- [ ] **Admin Revoke Permesso Globale**: `/faction admin revokePerm <gruppo> <permesso>`
    - *Verifica*: Permesso revocato dal gruppo.
    - *Argomenti*: `gruppo` (Obbligatorio), `permesso` (Obbligatorio).

### Gestione Admin Diplomazia

- [ ] **Admin Set Diplomazia Forzata**: `/faction admin setDiplomacy <gruppo1> <gruppo2> <relation>`
    - *Verifica*: Relazione impostata senza consenso.
    - *Argomenti*: `gruppo1` (Obbligatorio), `gruppo2` (Obbligatorio), `relation` (Obbligatorio).

### Sistema Admin

- [X] **Admin Reload Config**: `/faction admin reload`
    - *Verifica*: Configurazione ricaricata, modifiche applicate.
    - *Argomenti*: Nessuno.
    - *Test Edge*: Config invalida, errori parsing.

---

## 5. Test Sistema e Performance

### Eventi Server

- [X] **Player Join Event**: Entrata nel server
    - *Verifica*: Cache caricata, chat format applicato, gruppo caricato.
- [X] **Player Quit Event**: Uscita dal server
    - *Verifica*: Dati salvati, cache aggiornata, power calcolato.
- [X] **Chat Event**: Messaggio in chat
    - *Verifica*: Prefisso gruppo visualizzato, colori applicati.
- [X] **Death Event**: Morte giocatore
    - *Verifica*: Power perso (FACTION), notifiche inviate.
- [ ] **Kill Event**: Uccisione giocatore
    - *Verifica*: Power guadagnato, statistiche aggiornate.

### Persistenza Dati

- [X] **Riavvio Server**: Spegni e riavvia server
    - *Verifica*: Tutti i dati persistenti (gruppi, membri, claim, etc.).
- [ ] **Backup Database**: Test backup file groups.db
    - *Verifica*: Backup creato correttamente, ripristino possibile.
- [ ] **Corruzione Database**: Simula corruzione file
    - *Verifica*: Sistema gestisce errore gracefulmente.

### Performance e Scalabilità

- [ ] **Multipli Gruppi**: Crea 10+ gruppi attivi
    - *Verifica*: Performance mantenuta, cache efficiente.
- [ ] **Molti Membri**: Gruppo con 20+ membri
    - *Verifica*: Lista membri efficiente, operazioni veloci.
- [ ] **Molti Claim**: 100+ claim per fazione
    - *Verifica**: Mappa caricata velocemente, protezioni attive.
- [ ] **Concurrent Access**: Più giocatori operano simultaneamente
    - *Verifica*: Nessun race condition, dati consistenti.

---

## 6. Test Edge Cases e Sicurezza

### Input Validation

- [X] **Nomi Invalidi**: Caratteri speciali, troppo lunghi/corti
    - *Verifica*: Messaggi errore chiari, blocco input.
- [ ] **SQL Injection**: Tentativi injection nei comandi
    - *Verifica*: Input sanitizzato, nessuna vulnerabilità.

### Limiti e Constraints

- [ ] **Limiti Gruppo**: Superamento limiti configurati
    - *Verifica*: Messaggi errore limiti rispettati.
- [x] **Limiti Economici**: Transazioni con valori estremi
    - *Verifica*: Double overflow gestito, precisione mantenuta.
- [ ] **Limiti Territorio**: Claim massimi raggiunti
    - *Verifica*: Blocco claim con messaggio appropriato.

### Modalità Switch

- [X] **Switch FACTION → GUILD**: Cambia modalità in config
    - *Verifica*: Sistema adattato, power convertito/livelli assegnati.
- [X] **Switch GUILD → FACTION**: Cambia modalità inversa
    - *Verifica*: Livelli convertiti in power, sistema funzionante.

---

## 7. Test API e Integrazione

### BetterGroupEconomyAPI

- [] **API Singleton**: Test accesso singleton thread-safe
    - *Verifica*: Istanza unica, nessuna race condition.
- [ ] **API Member Balance**: Tutti i metodi economia membri
    - *Verifica*: Operazioni corrette, persistenza automatica.
- [ ] **API Group Balance**: Tutti i metodi economia gruppi
    - *Verifica*: Saldo aggiornato, transazioni atomiche.
- [ ] **API Error Handling**: Input invalidi, gruppi inesistenti
    - *Verifica*: Valori ritorno appropriati, nessun crash.

### Configurazione System

- [x] **Config Loading**: Caricamento configurazione con valori validi/invalidi
    - *Verifica*: Default applicati per valori invalidi.
- [x] **Config Hot Reload**: Reload senza riavvio
    - *Verifica*: Modifiche applicate immediatamente.
- [x] **Config Validation**: Validazione parametri complessi
    - *Verifica*: Regex, range, type checking funzionanti.

---

## 8. Checklist Finali

### Documentazione

- [x] **Comandi Help**: Tutti i comandi hanno help/messaggi uso
- [x] **Error Messages**: Messaggi errore chiari e utili
- [x] **Log Messages**: Logging appropriato per debug

### User Experience

- [ ] **Feedback Utente**: Conferme per tutte le azioni importanti
- [ ] **Undo Operations**: Possibilità annullare azioni critiche
- [ ] **Progress Indicators**: Indicatori per operazioni lunghe

### Compatibility

- [ ] **Multi-Version**: Test con diverse versioni Hytale
- [ ] **Plugin Conflicts**: Test con altri plugin comuni
- [ ] **Resource Usage**: CPU/Memory usage accettabile

---

*Piano di Test Completo v4.0 - Copertura 100% funzionalità*  
*Basato su analisi architettura completa del progetto*
