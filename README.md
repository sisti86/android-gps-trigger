# GPS Trigger

<img src="art/gps-trigger-icon.svg" alt="" width="96">

App Android (dalla 15 in poi, compatibile dalla 8) che cambia le impostazioni del telefono quando entri in una zona e le ripristina quando esci.

Per ogni zona puoi impostare:

- volume della suoneria
- volume delle notifiche
- volume in chiamata
- modalità audio (suono, vibrazione o silenzioso)
- Wi‑Fi acceso o spento

Per ogni zona scegli anche:

- **In quali giorni della settimana è attiva.** Negli altri giorni l'ingresso viene ignorato.
- **La frequenza di controllo della posizione:** 1, 10, 20, 30 o 60 minuti.

La posizione si sceglie su una mappa OpenStreetMap, toccando la mappa o usando la posizione GPS attuale. Non serve nessuna API key e l'app non usa Google Play Services.

L'interfaccia è in inglese, e in italiano se il telefono è impostato in italiano.

Il tema chiaro o scuro segue il sistema. Puoi anche sceglierlo dall'icona impostazioni in alto nella lista zone. In tema scuro anche la mappa diventa scura.

L'icona originale è in `art/gps-trigger-icon.svg`. Le versioni Android sono in `app/src/main/res/drawable/ic_launcher_*.xml`:

- sfondo
- primo piano
- versione monocromatica per le icone a tema

## Compilare

Serve JDK 17 e l'Android SDK (platform 35, build-tools 35). Su questo Mac sono installati con Homebrew.

```sh
./scripts/release.sh
```

Lo script compila l'app e la firma con la chiave personale. L'APK viene creato in `app/build/outputs/apk/release/gps-trigger-<versione>.apk`.

`./gradlew assembleRelease` da solo produce un APK non firmato, che Android non installa. La firma la fa `apksigner` dentro lo script, perché Gradle non supporta la rotazione della chiave.

## Firma e autore

L'app è di **Stefano Sisti** ed è distribuita con licenza MIT (vedi [LICENSE](LICENSE)).

Gli APK ufficiali sono firmati con questo certificato:

```
CN=Stefano Sisti, C=IT
SHA-256: 88:CA:16:F7:2B:24:B5:FE:B6:90:B9:C1:84:1D:D7:40:DD:1E:4A:67:0B:4A:BD:A0:D0:A1:3F:DC:4E:8D:00:2B
```

Per verificare che un APK sia autentico:

- **Dal Mac:**
  ```sh
  apksigner verify --print-certs gps-trigger-<versione>.apk
  ```
- **Dal telefono:** l'impronta è visibile nell'app, in Informazioni (icona ⓘ in alto nella lista zone).

Se l'impronta è diversa, l'APK non viene da questo progetto. In ogni caso Android non lo installerebbe sopra la versione ufficiale.

### Chiave di firma

- **Dove si trova.** Sul Mac di sviluppo, fuori dal repository, in `~/.android/gps-trigger/`:
  - `release.jks`: la chiave (RSA 4096, valida 30 anni)
  - `signing.properties`: la password
  - `lineage.bin`: la catena di rotazione
- **Fai una copia di sicurezza di quella cartella.** Senza, nessun aggiornamento potrà più essere installato sopra l'app esistente: bisognerebbe disinstallarla e rifare le zone. Non metterla mai su GitHub.
- **Rotazione.** Le versioni fino alla 0.1.4 erano firmate con la chiave di debug del Mac (`~/.android/debug.keystore`). Dalla 0.1.5 la firma include una "catena di rotazione" che dichiara il passaggio dalla vecchia chiave a quella personale, così l'aggiornamento si installa sopra la versione esistente e le zone restano. Un APK firmato solo con la vecchia chiave invece non viene più accettato.

### Installazione fuori dal Play Store

Dal 2027 Google richiederà che anche le app installate da APK sui telefoni con servizi Google provengano da sviluppatori registrati presso Google. Brasile, Indonesia, Singapore e Thailandia sono partiti a settembre 2026. Le alternative previste:

- Un account gratuito "hobbista", fino a 20 dispositivi.
- L'installazione via cavo con `adb install`, che resta esente.
- Un percorso "avanzato" dalle Opzioni sviluppatore.

## Installare sul telefono

1. Copia l'APK sul telefono e aprilo. Android chiederà di consentire l'installazione da quella sorgente.
2. Puoi anche usare il cavo USB con il debug USB attivo: `adb install -r gps-trigger-<versione>.apk`.
3. Se Play Protect segnala l'app come sconosciuta, scegli "Installa comunque".

## Primo avvio

La schermata principale mostra cosa manca da configurare:

1. **Posizione**: concedi l'accesso, poi in Impostazioni → Autorizzazioni → Posizione scegli **"Consenti sempre"**. Senza questa opzione le zone funzionano solo con l'app aperta.
2. **Accesso a Non disturbare**: serve per la modalità silenziosa e per cambiare i volumi quando Non disturbare è attivo.
3. **Notifiche** (facoltative): ricevi un avviso quando entri o esci da una zona.

Consigliato: in Impostazioni → App → GPS Trigger → Batteria scegli **"Senza restrizioni"**.

## Come funziona

- **Rilevamento delle zone.** L'app legge la posizione **solo dal GPS satellitare**.
  - A ogni controllo un breve servizio accende il GPS, legge la posizione e calcola la distanza da ogni zona.
  - Dura al massimo 60 secondi e mostra la notifica "Controllo posizione…".
  - Il provider "rete" (Wi‑Fi e celle) non viene usato, perché su quasi tutti i telefoni è di Google.
- **Raggio.**
  - Va da 25 m a 2000 m.
  - Con 25 o 50 m l'app mostra un avviso, perché la precisione del GPS è spesso di 20–50 m. Un raggio di almeno 100 m dà risultati più affidabili.
  - Per uscire da una zona la posizione deve risultare fuori anche tenendo conto dell'errore del GPS, così una lettura imprecisa vicino al bordo non fa entrare e uscire di continuo.
- **Ingresso.** Quando entri in una zona, l'app salva lo stato attuale del telefono e applica le impostazioni della zona.
- **Uscita.** Quando esci, ripristina solo le impostazioni che la zona aveva cambiato.
  - Con zone sovrapposte, prevale l'ultima zona in cui sei entrato.
- **Frequenza di controllo.**
  - La posizione viene controllata con la frequenza più alta tra le zone attive oggi.
  - Intervalli più lunghi fanno risparmiare batteria, ma ingresso e uscita vengono rilevati più tardi.
  - Se nessuna zona è attiva oggi, non viene fatto nessun controllo.
  - Con il telefono fermo e lo schermo spento (modalità Doze), Android lascia partire i controlli al massimo circa ogni 9 minuti, anche se hai scelto 1 minuto.
- **Giorni della settimana.**
  - Poco dopo mezzanotte l'app ricontrolla le zone.
  - Se sei dentro una zona e il nuovo giorno non è attivo, ripristina le impostazioni precedenti.
  - Se invece il nuovo giorno è attivo e sei dentro, applica le impostazioni della zona.
- **Quando parte un controllo, oltre agli orari pianificati.**
  - All'apertura dell'app.
  - Quando salvi, attivi o disattivi una zona.
  - Dopo un riavvio del telefono o un aggiornamento dell'app.
- **Prova senza muoverti.** Nella schermata di modifica di una zona ci sono i pulsanti "Simula ingresso" e "Simula uscita". "Simula ingresso" funziona anche nei giorni in cui la zona non è attiva.

## Privacy

L'app non raccoglie dati di utilizzo e non contiene librerie di analytics, crash reporting, pubblicità o Google Play Services. Zone e impostazioni restano solo sul telefono, e il backup Android su cloud è disattivato.

L'unico traffico di rete sono i **tile della mappa**, scaricati da `tile.openstreetmap.org` solo mentre scegli una posizione. I server di OpenStreetMap ricevono il tuo indirizzo IP e l'area della mappa che stai guardando. I tile restano poi in cache sul telefono.

Al di fuori del controllo dell'app:

- **A‑GPS.** Il sistema operativo può scaricare da solo dati di assistenza GPS, per esempio dal tuo operatore o da Google, per trovare prima i satelliti.
- **Precisione della posizione di Google.** Se nelle impostazioni Android è attiva, il telefono invia a Google scansioni Wi‑Fi e celle per tutte le app. Puoi disattivarla in Impostazioni → Posizione → Servizi di geolocalizzazione → Precisione della posizione di Google. L'app funziona anche senza, perché usa solo il GPS.

## Perché targetSdk 28

Da Android 10 le app non possono più accendere o spegnere il Wi‑Fi. L'eccezione vale solo per le app dichiarate per Android 9 o precedenti, cioè con targetSdk 28. Per questo l'app usa targetSdk 28.

Su Android 15 l'app si installa e funziona normalmente. Le conseguenze sono due:

- Non può essere pubblicata sul Play Store.
- Android potrebbe mostrare l'avviso "app creata per una versione precedente".

## Limiti noti

- **Suoneria e notifiche collegate.** Su alcuni telefoni il volume delle notifiche è legato a quello della suoneria, e cambiarne uno cambia anche l'altro. È un comportamento del produttore.
- **Wi‑Fi con modalità aereo.** Se la modalità aereo è attiva, Android può ignorare la richiesta di accendere il Wi‑Fi.
- **GPS al chiuso.** Dentro gli edifici il GPS può non trovare i satelliti. In quel caso il controllo viene saltato e lo stato delle zone resta com'era fino al controllo successivo.
- **Batteria.** Ogni controllo accende il GPS per qualche secondo, fino a 60. Con un intervallo di 1 minuto il GPS resta acceso quasi sempre. Da 10 minuti in su il consumo è contenuto.
- **Arresto forzato.** Se fermi l'app da Impostazioni → App → "Forza interruzione", Android cancella i controlli pianificati. Riprendono alla successiva apertura dell'app o dopo un riavvio.
