# Pravoslavac kalendar patch (LSPosed modul)

Xposed/LSPosed modul za Android aplikaciju **Православац** (`com.pravoslavac`):

- pocetni ekran je odmah kalendar (preskace splash i otvara tekuci mjesec),
- tamna tema (boje aplikacije, liste, redovi kalendara, WebView sadrzaj preko CSS/JS-a),
- svaki drugi red kalendara (`#fbf7ea`) postaje tamen (`#1f1f1d`),
- naslovi u listi molitvi (`#CC0000`) postaju svijetliji (`#ff7a66`).

Verzija: 1.7-dark (versionCode 8).

## Gradnja

Lokalno (JDK 17, Android SDK 34, Gradle 8.7+):

```
gradle :app:assembleRelease
```

APK je u `app/build/outputs/apk/release/`.

Na GitHub-u: svaki push na `main` pokrece workflow **Build APK** (`.github/workflows/build.yml`),
a gotov APK se skida iz *Actions → run → Artifacts*. Za tag oblika `v1.7` APK se dodaje i u *Releases*.

## Potpisivanje vlastitim kljucem

Bez podesavanja, release APK je potpisan *debug* kljucem. Da koristite svoj kljuc (isti kao do sada,
da se APK moze instalirati preko prethodne verzije), u repozitorijumu otvorite
*Settings → Secrets and variables → Actions* i dodajte:

| Secret              | Vrijednost                                         |
|---------------------|----------------------------------------------------|
| `KEYSTORE_B64`      | `base64 -w0 pravoslavac-dark-keystore.p12`         |
| `KEYSTORE_PASSWORD` | lozinka keystore-a                                 |
| `KEY_ALIAS`         | alias kljuca (npr. `pravoslavac-dark`)             |
| `KEY_PASSWORD`      | lozinka kljuca (kod `.p12` obicno ista kao gore)   |

**Keystore (`.p12`) nikada ne stavljajte u repozitorijum** - `.gitignore` ga vec izbacuje.

## Struktura

- `app/src/main/java/patch/kalendar/Hook.java` - Xposed ulazna tacka (hook-ovi i zamjena resursa)
- `app/src/main/java/patch/kalendar/Dark.java` - pretvaranje boja u tamnu temu
- `app/src/main/java/patch/kalendar/Assets.java` - CSS i JS za WebView
- `xposed-api-stub/` - samo stub klase Xposed API-ja za kompajliranje (nisu u APK-u)

## Napomena

Izvorni kod je rekonstruisan iz gotovog APK-a (originalni izvorni kod nije bio dostupan),
pa se moze razlikovati od originala u stilu, ali je ponasanje isto.
