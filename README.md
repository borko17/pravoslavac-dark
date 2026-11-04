# Pravoslavac Dark (LSPosed modul)

LSPosed (Xposed) modul za Android aplikaciju **Православац** (`com.pravoslavac`). Dodaje tamnu temu sa
potpuno prilagodljivim bojama, nasumično generisane teme i nekoliko opcija za ponašanje aplikacije pri pokretanju
i prikazu teksta.

> Nezvanični modul. Nije povezan sa autorima aplikacije Православац.

## Mogućnosti

- **Tamna tema** za cijelu aplikaciju: trake i tabovi, liste, meni, redovi kalendara, dijalozi, kao i
  sadržaj knjiga, molitvi i žitija (WebView)
- **Uređivač tema**: 19 podesivih boja, birač boje (prstenovi nijanse, zasićenosti i svjetline + unos
  hex koda), preimenovanje, brisanje, kopiranje i neograničeno vraćanje izmjena (undo).
- **Nasumične teme** (svijetle ili tamne) sa zagarantovanim kontrastom slova i pozadine (WCAG omjer
  kontrasta), uključujući teme generisane iz vlastite boje pozadine i boje praznika.
- **Prenos tema** preko klipborda (kopiraj sve / učitaj kopirane).
- **Preskakanje uvodnog ekrana** (splash).
- **Otvaranje kalendara pri pokretanju** (tekući mjesec) umjesto žitija za tekući dan.
- **Tekst poravnat lijevo** umjesto obostranog (justified) – u WebView sadržaju i u nativnim `TextView` poljima.
- Podešavanja su **unutar same aplikacije** Православац; modul nema vlastitu ikonu u launcheru.

## Zahtjevi

- Android 5.0+ (`minSdk 21`)
- Instaliran **Православац** (`com.pravoslavac`)
- **LSPosed** (ili drugi okvir kompatibilan sa Xposed API-jem 93)

## Instalacija

1. Preuzmite najnoviji `Pravoslavac_Dark_<verzija>.apk` iz sekcije **Releases** i instalirajte ga.
2. U LSPosed-u otvorite **Moduli**, uključite **Pravoslavac Dark** i u opsegu (scope) označite
   `com.pravoslavac`.
3. Prisilno zaustavite Православац i pokrenite ga ponovo.

Modul se pojavljuje u LSPosed-u pod nazivom *Pravoslavac Dark*, a paket je `com.pravoslavac.dark`.

## Podešavanja

U aplikaciji Православац otvorite bočni meni i izaberite poslednju grupu, **Подешавања** (ikonica zupčanika).
Ekran ima dva taba: **Опције** i **Теме**.

### Tab Опције

Sve četiri opcije su podrazumijevano uključene.

| Opcija | Šta radi |
| --- | --- |
| **Прескочи уводни екран** | Aplikacija se otvara bez uvodnog ekrana. |
| **Апликација отвара календар** | Pri pokretanju se otvara kalendar (tekući mesec i godina) umjesto žitija za taj dan. |
| **Текст поравнат лево** | Isključuje obostrano poravnanje teksta. |
| **Теме** | Uključeno: aplikacija koristi boje odabrane teme. Isključeno: originalne boje aplikacije. |

Promjena opcije „Теме“ i „Текст поравнат лево“ primjenjuje se kad zatvorite podešavanja. Ako se pri isključivanju
teme neke originalne boje ne mogu vratiti u hodu, aplikacija će zatražiti ponovno pokretanje.

### Tab Теме

Gore je lista tema; dodir bira temu, a dugme **⋮** pored nje otvara meni:

- **Уреди боје** – dodir na kvadrat boje otvara birač boje (ili upišite hex šifru),
- **Преименуј**,
- **Копирај**,
- **Обриши** (mora ostati bar jedna tema).

Traka s ikonicama ispod liste:

| Ikonica | Akcija |
| --- | --- |
| + | **Нова тема** – pravi kopiju odabrane teme i odmah otvara uređivanje boja |
| Kocka sa + | **Нова насумична тема** – svetla, tamna, ili iz vaše boje (vidi dolje) |
| Kružna strelica | **Поново генериши** odabranu nasumičnu temu |
| Dva lista | **Копирај све теме** u klipbord (JSON) |
| Lepljenje | **Учитај копиране теме** iz klipborda (već postojeće se preskaču) |

U uređivaču boja dugme **Врати** vraća jednu boju unazad (čuva se do 300 koraka po temi).

## Teme

Ugrađena je jedna tema, **Тамна**, a sve ostale pravite sami. 

| Grupa | Boje |
| --- | --- |
| Позадине | kalendar, meni, knjige i žitija |
| Календар | neparni dani, okvir današnjeg dana |
| Текст | kalendar i podmeniji, naslovi/praznici/postovi, oznake verzija (Српски, Црквенословенски), liste knjiga i molitvi, slova na tabovima, linkovi, posjećeni linkovi |
| Елементи | tabovi, traka mjeseca i godine, klizač |
| Линије | ispod aktivnog taba, ispod datuma u kalendaru, razdjelnici kalendara i menija, liste molitvi i knjiga |

Tri dodatne boje (statusna traka, akcent prekidača, linije i tabele u knjigama) čuvaju se u temi, ali se ne uređuju.

Pozadina može biti i **svijetla** – tada modul ne pretvara originalne svijetle boje u tamne, nego primjenjuje
vaše.

### Nasumične teme

Izbor iz menija „Насумична тема“:

- **Светла** / **Тамна** – potpuno nasumična tema,
- **Од моје боје: позадина** – nijansa, zasićenost i svjetlina pozadine dolaze iz vaše boje (ograničene na raspon
  u kojem je čitljivost zagarantovana), ostalo je nasumično,
- **Од моје боје: позадина и боја празника** – dodatno zadajete boju naslova i praznika.

Ako je zadatu boju trebalo prilagoditi radi čitljivosti, dobićete obavještenje. Generisane teme dobijaju
nazive tipa „boja + imenica“ (npr. *рубинска …*), a rod imenice govori kako je tema nastala (muški – nasumična,
ženski – iz boje pozadine, srednji – iz boje pozadine i praznika). Zato **Поново генериши** radi samo na
temama koje nisu preimenovane.