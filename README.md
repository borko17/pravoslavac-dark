# Pravoslavac Dark (LSPosed modul)

LSPosed (Xposed) modul za Android aplikacije **Pravoslavac** (`com.pravoslavac`) i **Житије светих**
(`com.zitijesvetih`). Za obe aplikacije isti je skup opcija, tema i podešavanja. Dodaje tamnu temu sa
potpuno prilagodljivim bojama, nasumično generisane teme i nekoliko opcija za ponašanje aplikacije pri pokretanju
i prikazu teksta.

> Nezvanični modul. Nije povezan sa autorima aplikacija Pravoslavac i Житије светих.

## Mogućnosti

- **Tamna tema** za celu aplikaciju: trake i tabovi, liste, meni, redovi kalendara, dijalozi, kao i
  sadržaj knjiga, molitvi i žitija (WebView)
- **Uređivač tema**: 19 podesivih boja, birač boje (prstenovi nijanse, zasićenosti i svetline + unos
  hex koda), preimenovanje, brisanje, kopiranje i neograničeno vraćanje izmena (undo).
- **Nasumične teme** (svetle ili tamne) sa zagarantovanim kontrastom slova i pozadine (WCAG omjer
  kontrasta), uključujući teme generisane iz vlastite boje pozadine i boje praznika.
- **Prenos tema** preko klipborda (kopiraj sve / učitaj kopirane).
- **Preskakanje uvodnog ekrana** (splash).
- **Otvaranje kalendara pri pokretanju** (tekući mesec) umesto žitija za tekući dan.
- **Tekst poravnat levo** umesto obostranog (justified) – u WebView sadržaju i u nativnim `TextView` poljima.
- Podešavanja su **unutar same aplikacije** (Православац, odnosno Житије светих); modul nema vlastitu ikonu u launcheru.
  Svaka aplikacija ima svoja nezavisna podešavanja i teme.

## Zahtevi

- Android 5.0+ (`minSdk 21`)
- Instaliran **Pravoslavac** (`com.pravoslavac`) i/ili **Житије светих** (`com.zitijesvetih`, testirano na 1.3.8)
- **LSPosed** (ili drugi okvir kompatibilan sa Xposed API-jem 93)

## Instalacija

1. Preuzmite najnoviji `Pravoslavac_Dark_<verzija>.apk` iz sekcije **Releases** i instalirajte ga.
2. U LSPosed-u otvorite **Moduli**, uključite **Pravoslavac Dark** i u opsegu (scope) označite
   `com.pravoslavac` i/ili `com.zitijesvetih`.
3. Prisilno zaustavite aplikaciju i pokrenite je ponovo.

Modul se pojavljuje u LSPosed-u pod nazivom *Pravoslavac Dark*, a paket je `io.github.borko17.pravoslavac.dark`.

## Podešavanja

U aplikaciji (Pravoslavac ili Житије светих) otvorite bočni meni i izaberite poslednju grupu, **Podešavanja** (ikonica zupčanika).
Ekran ima dva taba: **Opcije** i **Teme**.

### Tab Opcije

Sve četiri opcije su podrazumevano uključene.

| Opcija | Šta radi |
| --- | --- |
| **Preskoči uvodni ekran** | Aplikacija se otvara bez uvodnog ekrana. |
| **Aplikacija otvara kalendar** | Pri pokretanju se otvara kalendar (tekući mesec i godina) umesto žitija za taj dan. |
| **Tekst poravnat levo** | Isključuje obostrano poravnanje teksta. |
| **Teme** | Uključeno: aplikacija koristi boje odabrane teme. Isključeno: originalne boje aplikacije. |

Promena opcije „Teme“ i „Tekst poravnat levo“ primenjuje se kad zatvorite podešavanja. Ako se pri isključivanju
teme neke originalne boje ne mogu vratiti u hodu, aplikacija će zatražiti ponovno pokretanje.

### Tab Teme

Gore je lista tema; dodir bira temu, a dugme **⋮** pored nje otvara meni:

- **Uredi boje** – dodir na kvadrat boje otvara birač boje (ili upišite hex šifru),
- **Preimenuj**,
- **Kopiraj**,
- **Obriši** (mora ostati bar jedna tema).

Traka s ikonicama ispod liste:

| Ikonica | Akcija |
| --- | --- |
| + | **Nova tema** – pravi kopiju odabrane teme i odmah otvara uređivanje boja |
| Kocka sa + | **Nova nasumična tema** – svetla, tamna, ili iz vaše boje (vidi dole) |
| Kružna strelica | **Ponovo generiši** odabranu nasumičnu temu |
| Dva lista | **Kopiraj sve teme** u klipbord (JSON) |
| Lepljenje | **Učitaj kopirane teme** iz klipborda (već postojeće se preskaču) |

U uređivaču boja dugme **Vrati** vraća jednu boju unazad (čuva se do 300 koraka po temi).

## Teme

Ugrađena je jedna tema, **Tamna**, a sve ostale pravite sami.

| Grupa | Boje |
| --- | --- |
| Pozadine | kalendar, meni, knjige i žitija |
| Kalendar | neparni dani, okvir današnjeg dana |
| Tekst | kalendar i podmeniji, naslovi/praznici/postovi, oznake verzija (Srpski, Crkvenoslovenski), liste knjiga i molitvi, slova na tabovima, linkovi, posećeni linkovi |
| Elementi | tabovi, traka meseca i godine, klizač |
| Linije | ispod aktivnog taba, ispod datuma u kalendaru, razdelnici kalendara i menija, liste molitvi i knjiga |

### Nasumične teme

Izbor iz menija „Nasumična tema“:

- **Svetla** / **Tamna** – potpuno nasumična tema,
- **Od moje boje: pozadina** – nijansa, zasićenost i svetlina pozadine dolaze iz vaše boje (ograničene na raspon
  u kojem je čitljivost zagarantovana), ostalo je nasumično,
- **Od moje boje: pozadina i boja praznika** – dodatno zadajete boju naslova i praznika.

Ako je zadatu boju trebalo prilagoditi radi čitljivosti, dobićete obaveštenje. Generisane teme dobijaju
nazive tipa „boja + imenica“ (npr. *rubinska …*), a rod imenice govori kako je tema nastala (muški – nasumična,
ženski – iz boje pozadine, srednji – iz boje pozadine i praznika). Zato **Ponovo generiši** radi samo na
temama koje nisu preimenovane.
