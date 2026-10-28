# Pravoslavac Dark (LSPosed modul)

Xposed/LSPosed modul za Android aplikaciju **Православац** (`com.pravoslavac`).

Aplikacija modula ima svoj ekran (ikona "Pravoslavac kalendar patch" u launcheru) s prekidacima:

- **Pocetni ekran je kalendar** - pri pokretanju se otvara kalendar (tekuci mjesec) umjesto zitija za taj dan,
- **Preskoci uvodni ekran** - pocetni ekran je odmah kalendar (tekuci mjesec),
- **Teme** - ukljucuje tamnu temu (boje aplikacije, liste, redovi kalendara, WebView sadrzaj preko CSS/JS-a)
  u bojama odabrane teme; iskljuceno = originalne boje aplikacije. Ispod prekidaca je lista tema
  (ukljucene 3: *Tamna (originalna)*, *AMOLED crna*, *Topla sepija*) iz koje se bira jedna. Meni **⋮** pored teme:
  uredi boje (dodir na kvadrat boje otvara paletu, ili upis hex sifre), preimenuj, obrisi; dugme
  **+ Nova tema** pravi kopiju odabrane teme. "Vrati" u uredjivanju vraca boje ugradjene teme.
- **Tekst poravnat lijevo** - umjesto obostranog poravnanja (justified).

Promjene se primjenjuju pri sljedecem pokretanju aplikacije Pravoslavac.
Modul mora biti aktivan u LSPosed-u (scope: `com.pravoslavac`).
