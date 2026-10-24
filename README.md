# GPS Trigger

<img src="art/gps-trigger-icon.svg" alt="" width="96">

🇮🇹 [Leggi in italiano](README.it.md)

An Android app that changes your phone's settings when you enter a place and restores them when you leave. It targets Android 15 and later and also runs on Android 8 and later.

For each zone you can set:

- ringtone volume
- notification volume
- in-call volume
- sound mode (sound, vibrate or silent)
- Wi‑Fi on or off

For each zone you also choose:

- **Days of the week it is active.** Entering it on other days is ignored.
- **Location check frequency:** 1, 10, 20, 30 or 60 minutes.

You pick the location on an OpenStreetMap map, by tapping it or by using your current GPS position. It needs no API key and doesn't use Google Play Services.

The app is in English, and in Italian on phones set to Italian.

The light or dark theme follows the system. You can also choose it from the settings icon at the top of the zone list. In dark theme the map turns dark too.

The original icon is `art/gps-trigger-icon.svg`. The Android versions are in `app/src/main/res/drawable/ic_launcher_*.xml`:

- background
- foreground
- monochrome version for themed icons

## Building from source

Anyone can build the app from this repository and install the APK by hand, on Windows, macOS or Linux.

### Requirements

- **JDK 17** or newer.
- **Android SDK** with Android 15 (platform 35) and build-tools 35.0.0.
- Gradle doesn't need to be installed: the `gradlew` / `gradlew.bat` wrapper downloads it.

### The easy way: Android Studio

1. Install [Android Studio](https://developer.android.com/studio). It includes the JDK and the SDK.
2. **File → Open** and select the project folder. Wait for the Gradle sync to finish.
3. **Build → Generate Signed App Bundle or APK… → APK.** Create your own key when asked, then choose the `release` variant.

### From the command line

**1. Install JDK 17**

| System | Command |
|---|---|
| Windows | `winget install EclipseAdoptium.Temurin.17.JDK` |
| macOS | `brew install openjdk@17` |
| Debian / Ubuntu | `sudo apt install openjdk-17-jdk` |
| Fedora | `sudo dnf install java-17-openjdk-devel` |
| Arch | `sudo pacman -S jdk17-openjdk` |

**2. Install the Android command-line tools**

1. Download "Command line tools only" from [developer.android.com/studio](https://developer.android.com/studio#command-line-tools-only). On macOS you can use `brew install --cask android-commandlinetools` instead.
2. Unzip it so that it ends up in `<SDK>/cmdline-tools/latest/`. `<SDK>` is the SDK folder. The usual choices:
   - Windows: `%LOCALAPPDATA%\Android\Sdk`
   - macOS: `~/Library/Android/sdk`
   - Linux: `~/Android/Sdk`

**3. Set the environment variables**

macOS / Linux (bash or zsh):

```sh
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=~/Android/Sdk
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/build-tools/35.0.0:$ANDROID_HOME/platform-tools:$PATH"
```

Windows (PowerShell):

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot"   # adjust to your version
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:Path = "$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:ANDROID_HOME\build-tools\35.0.0;$env:ANDROID_HOME\platform-tools;$env:Path"
```

These only last for the current terminal. To make them permanent, add them to `~/.bashrc` / `~/.zshrc`, or on Windows to System Properties → Environment Variables.

**4. Install the SDK packages**

```sh
sdkmanager --licenses
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

On Windows the command is `sdkmanager.bat`.

**5. Build**

```sh
./gradlew assembleRelease        # Windows: gradlew.bat assembleRelease
```

This creates `app/build/outputs/apk/release/app-release-unsigned.apk`. Android won't install an unsigned APK, so it needs signing with your own key.

**6. Sign with your own key**

Create the key once and keep it safe, because every future update must be signed with it:

```sh
keytool -genkeypair -keystore my-key.jks -alias mykey -keyalg RSA -keysize 4096 -validity 10000
```

Then sign each build:

```sh
zipalign -f -P 16 4 app/build/outputs/apk/release/app-release-unsigned.apk gps-trigger.apk
apksigner sign --ks my-key.jks --ks-key-alias mykey gps-trigger.apk
```

On Windows the tools are `zipalign.exe` and `apksigner.bat`. Both are in `<SDK>\build-tools\35.0.0`.

**Quick alternative for testing:** `./gradlew assembleDebug` creates `app/build/outputs/apk/debug/app-debug.apk`. Gradle already signs it with an automatic debug key, but it runs slower than the release build.

### What to know about a self-built APK

- **It is signed with your key, not the official one.** Android won't install it over the official version, or the other way round, unless you uninstall the app first, which deletes its zones.
- **Your key's fingerprint** is what the **About** screen shows.
- **Official releases** are built with `scripts/release.sh`, which needs the author's key.

## Installing on the phone

1. Copy the APK to the phone and open it. Android asks you to allow installs from that source.
2. Or, over USB with USB debugging on: `adb install -r gps-trigger.apk`.
3. If Play Protect flags the app as unknown, choose "Install anyway".

### Installing outside the Play Store

From 2027 Google will require apps installed from an APK on phones with Google services to come from developers registered with Google. Brazil, Indonesia, Singapore and Thailand started in September 2026. The planned alternatives:

- A free "hobbyist" account, for up to 20 devices.
- Installing over USB with `adb install`, which stays exempt.
- An "advanced flow" in Developer options.

## First launch

The main screen shows what still needs setting up:

1. **Location**: allow access, then in Settings → Permissions → Location choose **"Allow all the time"**. Without it, zones only work while the app is open.
2. **Do Not Disturb access**: needed for silent mode and to change volumes while Do Not Disturb is on.
3. **Notifications** (optional): get a notice when you enter or leave a zone.

Recommended: in Settings → Apps → GPS Trigger → Battery choose **"Unrestricted"**.

## How it works

- **Zone detection.** The app reads the location **from satellite GPS only**.
  - At each check a short-lived service turns on GPS, reads the position and works out the distance to each zone.
  - A check lasts at most 60 seconds and shows a "Checking location…" notification.
  - The "network" provider (Wi‑Fi and cell towers) isn't used, because on nearly all phones it belongs to Google.
- **Radius.**
  - From 25 m to 2000 m.
  - At 25 or 50 m the app shows a warning, because GPS accuracy is often 20–50 m. A radius of at least 100 m is more reliable.
  - To leave a zone, the position must be outside it even allowing for the GPS error. This way an imprecise reading near the edge doesn't keep entering and leaving.
- **Entering.** The app saves the phone's current state and applies the zone's settings.
- **Leaving.** The app restores only the settings the zone changed.
  - With overlapping zones, the last zone entered wins.
- **Check frequency.**
  - The location is checked at the highest frequency among the zones active today.
  - Longer intervals save battery, but entering and leaving are noticed later.
  - If no zone is active today, no checks run.
  - With the phone still and the screen off (Doze mode), Android runs the checks at most about every 9 minutes, even if you chose 1 minute.
- **Days of the week.**
  - Just after midnight the app checks the zones again.
  - If you are inside a zone and the new day isn't active, it restores the previous settings.
  - If the new day is active and you are inside, it applies the zone's settings.
- **Extra checks.** Besides the scheduled ones, a check also runs:
  - when the app is opened
  - when a zone is saved, enabled or disabled
  - after a reboot or an app update
- **Testing without moving.** The zone editor has "Simulate entering" and "Simulate leaving" buttons. "Simulate entering" also works on days the zone is off.

## Privacy

The app collects no usage data and contains no analytics, crash reporting, advertising or Google Play Services libraries. Zones and settings stay on the phone, and Android cloud backup is disabled.

The only network traffic is **map tiles**, downloaded from `tile.openstreetmap.org` only while you choose a location. The OpenStreetMap servers receive your IP address and the map area you're looking at. The tiles are then cached on the phone.

Outside the app's control:

- **A‑GPS.** The operating system may download GPS assistance data by itself, for example from your carrier or Google, to find satellites faster.
- **Google Location Accuracy.** If it is on in Android settings, the phone sends Wi‑Fi and cell scans to Google for all apps. You can turn it off in Settings → Location → Location services → Google Location Accuracy. The app works without it, since it only uses GPS.

## Signing and author

The app is by **Stefano Sisti** and is released under the MIT license (see [LICENSE](LICENSE)).

Official APKs are signed with this certificate:

```
CN=Stefano Sisti, C=IT
SHA-256: 88:CA:16:F7:2B:24:B5:FE:B6:90:B9:C1:84:1D:D7:40:DD:1E:4A:67:0B:4A:BD:A0:D0:A1:3F:DC:4E:8D:00:2B
```

To check that an APK is genuine:

- **On a computer:**
  ```sh
  apksigner verify --print-certs gps-trigger-<version>.apk
  ```
- **On the phone:** the fingerprint is shown in the app under About (ⓘ icon at the top of the zone list).

If the fingerprint is different, the APK doesn't come from this project.

### For the maintainer: official releases

```sh
./scripts/release.sh
```

The script builds the app and signs it. The APK is created in `app/build/outputs/apk/release/gps-trigger-<version>.apk`.

- **Where the signing files are.** They are read from `~/.android/gps-trigger/`, outside the repository:
  - `release.jks`: the key (RSA 4096, valid 30 years)
  - `signing.properties`: the password
  - `lineage.bin`: the rotation lineage
- **Keep a backup of that folder.** Without it no update can be installed over existing installs. Never commit it.
- **Key rotation.** Versions up to 0.1.4 were signed with a debug key. From 0.1.5 the signature carries a rotation lineage from that key to the personal key, so updates install over older versions and keep the zones. APKs signed with the old key alone are no longer accepted.

## Why targetSdk 28

Since Android 10 apps can no longer turn Wi‑Fi on or off. The only exception is apps declared for Android 9 or earlier, that is with targetSdk 28. That's why the app uses targetSdk 28.

On Android 15 the app installs and works normally. There are two consequences:

- It can't be published on the Play Store.
- Android may show a "this app was built for an older version of Android" warning.

## Known limitations

- **Linked ringtone and notification volumes.** On some phones the notification volume is tied to the ringtone volume, and changing one also changes the other. This is the manufacturer's behaviour.
- **Wi‑Fi in airplane mode.** With airplane mode on, Android may ignore a request to turn Wi‑Fi on.
- **GPS indoors.** Inside buildings GPS may not find the satellites. In that case the check is skipped and the zones stay as they were until the next check.
- **Battery.** Each check turns GPS on for a few seconds, up to 60. With a 1-minute interval GPS is on almost all the time. From 10 minutes upwards the drain is small.
- **Force stop.** If you stop the app from Settings → Apps → "Force stop", Android cancels the scheduled checks. They resume the next time the app is opened, or after a reboot.
