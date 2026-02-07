# VoxPopuli

Plugin Hytale per server community, con funzionalità social avanzate e gestione reputazione, log e dashboard admin.

## Funzionalità principali

- **Missive**: messaggi privati tra utenti, con controllo su destinatari esistenti.
- **Bacheca Gilda**: post, like, gestione membri e permessi.
- **Reputazione e Fama**: sistema di flag e tracciamento eventi oggettivi.
- **Configurazione log**: parametri di logging (dimensione file, numero file) configurabili da dashboard.
- **Dashboard Admin**: gestione avanzata di config, permessi, log e parametri social.

## Requisiti
- Hytale server compatibile
- Java 25
- Database SQLite (default: `config/voxpopuli/voxpopuli.db`)

## Installazione
1. Copia la cartella del plugin nella directory delle mod del server Hytale.
2. Avvia il server: il plugin crea e aggiorna il database e i file di configurazione necessari.
3. Accedi alla dashboard admin tramite comando `/voxadmin` (permessi richiesti).

## Configurazione
- Tutte le configurazioni sono centralizzate nel database SQLite.
- I parametri di log sono modificabili dalla dashboard admin.
- Nessuna modifica manuale ai file di config necessaria.

## Sicurezza
- Le azioni sociali (missive, inviti, promozioni) sono consentite solo verso utenti realmente esistenti.
- La creazione di utenti avviene solo al primo accesso reale di un player.
