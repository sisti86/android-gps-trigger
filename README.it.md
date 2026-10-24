# GPS Trigger

<img src="art/gps-trigger-icon.svg" alt="" width="96">

🇬🇧 [Read in English](README.md)

App Android che cambia le impostazioni del telefono quando entri in un luogo e le ripristina quando esci. È pensata per Android 15 e successivi, e funziona anche da Android 8.

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

## Compilare dal codice sorgente

Chiunque può compilare l'app da questo repository e installare l'APK a mano, su Windows, macOS o Linux.

### Requisiti

- **JDK 17** o successivo.
- **Android SDK** con Android 15 (platform 35) e build-tools 35.0.0.
- Gradle non va installato: lo scarica da solo il wrapper `gradlew` / `gradlew.bat`.

### Il modo più semplice: Android Studio

1. Installa [Android Studio](https://developer.android.com/studio), che include già JDK e SDK.
2. **File → Open** e scegli la cartella del progetto. Aspetta che finisca la sincronizzazione Gradle.
3. **Build → Generate Signed App Bundle or APK… → APK.** Crea una tua chiave quando te lo chiede, poi scegli la variante `release`.

### Da riga di comando

**1. Installa JDK 17**

| Sistema | Comando |
|---|---|
| Windows | `winget install EclipseAdoptium.Temurin.17.JDK` |
| macOS | `brew install openjdk@17` |
| Debian / Ubuntu | `sudo apt install openjdk-17-jdk` |
| Fedora | `sudo dnf install java-17-openjdk-devel` |
| Arch | `sudo pacman -S jdk17-openjdk` |

**2. Installa gli strumenti da riga di comando di Android**

1. Scarica "Command line tools only" da [developer.android.com/studio](https://developer.android.com/studio#command-line-tools-only). Su macOS puoi usare invece `brew install --cask android-commandlinetools`.
2. Estrai lo zip in modo che finisca in `<SDK>/cmdline-tools/latest/`. `<SDK>` è la cartella dell'SDK. Le scelte abituali:
   - Windows: `%LOCALAPPDATA%\Android\Sdk`
   - macOS: `~/Library/Android/sdk`
   - Linux: `~/Android/Sdk`

**3. Imposta le variabili d'ambiente**

macOS / Linux (bash o zsh):

```sh
export JAVA_HOME=/percorso/del/jdk-17
export ANDROID_HOME=~/Android/Sdk
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/build-tools/35.0.0:$ANDROID_HOME/platform-tools:$PATH"
```

Windows (PowerShell):

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot"   # adatta alla tua versione
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:Path = "$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:ANDROID_HOME\build-tools\35.0.0;$env:ANDROID_HOME\platform-tools;$env:Path"
```

Valgono solo per il terminale aperto. Per renderle permanenti aggiungile a `~/.bashrc` / `~/.zshrc`, oppure su Windows in Proprietà del sistema → Variabili d'ambiente.

**4. Installa i pacchetti dell'SDK**

```sh
sdkmanager --licenses
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

Su Windows il comando è `sdkmanager.bat`.

**5. Compila**

```sh
./gradlew assembleRelease        # Windows: gradlew.bat assembleRelease
```

Il risultato è `app/build/outputs/apk/release/app-release-unsigned.apk`. Android non installa un APK non firmato, quindi va firmato con una tua chiave.

**6. Firma con una tua chiave**

Crea la chiave una volta sola e conservala, perché ogni aggiornamento futuro andrà firmato con la stessa:

```sh
keytool -genkeypair -keystore mia-chiave.jks -alias miachiave -keyalg RSA -keysize 4096 -validity 10000
```

Poi firma ogni build:

```sh
zipalign -f -P 16 4 app/build/outputs/apk/release/app-release-unsigned.apk gps-trigger.apk
apksigner sign --ks mia-chiave.jks --ks-key-alias miachiave gps-trigger.apk
```

Su Windows gli strumenti sono `zipalign.exe` e `apksigner.bat`, entrambi in `<SDK>\build-tools\35.0.0`.

**Alternativa veloce per provare:** `./gradlew assembleDebug` crea `app/build/outputs/apk/debug/app-debug.apk`. Gradle lo firma già con una chiave di debug automatica, ma è più lento della versione release.

### Cosa sapere di un APK compilato da te

- **È firmato con la tua chiave, non con quella ufficiale.** Android non lo installa sopra la versione ufficiale, né viceversa, a meno di disinstallare prima l'app, perdendo le zone.
- **L'impronta della tua chiave** è quella che compare nella schermata **Informazioni**.
- **Le release ufficiali** si creano con `scripts/release.sh`, che richiede la chiave dell'autore.

## Installare sul telefono

1. Copia l'APK sul telefono e aprilo. Android chiederà di consentire l'installazione da quella sorgente.
2. Oppure via cavo USB, con il debug USB attivo: `adb install -r gps-trigger.apk`.
3. Se Play Protect segnala l'app come sconosciuta, scegli "Installa comunque".

### Installazione fuori dal Play Store

Dal 2027 Google richiederà che anche le app installate da APK sui telefoni con servizi Google provengano da sviluppatori registrati presso Google. Brasile, Indonesia, Singapore e Thailandia sono partiti a settembre 2026. Le alternative previste:

- Un account gratuito "hobbista", fino a 20 dispositivi.
- L'installazione via cavo con `adb install`, che resta esente.
- Un percorso "avanzato" dalle Opzioni sviluppatore.

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
- **Controlli aggiuntivi.** Oltre a quelli pianificati, un controllo parte anche:
  - all'apertura dell'app
  - quando salvi, attivi o disattivi una zona
  - dopo un riavvio del telefono o un aggiornamento dell'app
- **Prova senza muoverti.** Nella schermata di modifica di una zona ci sono i pulsanti "Simula ingresso" e "Simula uscita". "Simula ingresso" funziona anche nei giorni in cui la zona non è attiva.

## Privacy

L'app non raccoglie dati di utilizzo e non contiene librerie di analytics, crash reporting, pubblicità o Google Play Services. Zone e impostazioni restano solo sul telefono, e il backup Android su cloud è disattivato.

L'unico traffico di rete sono i **tile della mappa**, scaricati da `tile.openstreetmap.org` solo mentre scegli una posizione. I server di OpenStreetMap ricevono il tuo indirizzo IP e l'area della mappa che stai guardando. I tile restano poi in cache sul telefono.

Al di fuori del controllo dell'app:

- **A‑GPS.** Il sistema operativo può scaricare da solo dati di assistenza GPS, per esempio dal tuo operatore o da Google, per trovare prima i satelliti.
- **Precisione della posizione di Google.** Se nelle impostazioni Android è attiva, il telefono invia a Google scansioni Wi‑Fi e celle per tutte le app. Puoi disattivarla in Impostazioni → Posizione → Servizi di geolocalizzazione → Precisione della posizione di Google. L'app funziona anche senza, perché usa solo il GPS.

## Firma e autore

L'app è di **Stefano Sisti** ed è distribuita con licenza MIT (vedi [LICENSE](LICENSE)).

Gli APK ufficiali sono firmati con questo certificato:

```
CN=Stefano Sisti, C=IT
SHA-256: 88:CA:16:F7:2B:24:B5:FE:B6:90:B9:C1:84:1D:D7:40:DD:1E:4A:67:0B:4A:BD:A0:D0:A1:3F:DC:4E:8D:00:2B
```

Per verificare che un APK sia autentico:

- **Dal computer:**
  ```sh
  apksigner verify --print-certs gps-trigger-<versione>.apk
  ```
- **Dal telefono:** l'impronta è visibile nell'app, in Informazioni (icona ⓘ in alto nella lista zone).

Se l'impronta è diversa, l'APK non viene da questo progetto.

### Per il manutentore: release ufficiali

```sh
./scripts/release.sh
```

Lo script compila l'app e la firma. L'APK viene creato in `app/build/outputs/apk/release/gps-trigger-<versione>.apk`.

- **Dove sono i file di firma.** Vengono letti da `~/.android/gps-trigger/`, fuori dal repository:
  - `release.jks`: la chiave (RSA 4096, valida 30 anni)
  - `signing.properties`: la password
  - `lineage.bin`: la catena di rotazione
- **Fai una copia di sicurezza di quella cartella.** Senza, nessun aggiornamento potrà più essere installato sopra l'app esistente. Non metterla mai nel repository.
- **Rotazione della chiave.** Le versioni fino alla 0.1.4 erano firmate con una chiave di debug. Dalla 0.1.5 la firma include una catena di rotazione da quella chiave alla chiave personale, così l'aggiornamento si installa sopra le versioni precedenti e le zone restano. Un APK firmato solo con la vecchia chiave non viene più accettato.

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
