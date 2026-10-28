# Pravoslavac Dark (LSPosed modul)

Xposed/LSPosed modul za Android aplikaciju **Православац** (`com.pravoslavac`).

Aplikacija modula ima svoj ekran (ikona "Pravoslavac kalendar patch" u launcheru) s prekidačima:

- **Početni ekran je kalendar** - pri pokretanju se otvara kalendar (tekući mjesec) umjesto žitija za taj dan,
- **Preskoči uvodni ekran** - početni ekran je odmah kalendar (tekući mjesec),
- **Teme** - uključuje tamnu temu (boje aplikacije, liste, redovi kalendara, WebView sadrzaj preko CSS/JS-a)
  u bojama odabrane teme; isključeno = originalne boje aplikacije. Ispod prekidaca je lista tema
  (uključene 3: *Tamna (originalna)*, *AMOLED crna*, *Topla sepija*) iz koje se bira jedna. Meni **⋮** pored teme:
  uredi boje (dodir na kvadrat boje otvara paletu, ili upis hex sifre), preimenuj, obriši; dugme
  **+ Nova tema** pravi kopiju odabrane teme. "Vrati" u uređivanju vraća boje ugrađene teme.
- **Tekst poravnat lijevo** - umjesto obostranog poravnanja (justified).

Promjene se primjenjuju pri sljedećem pokretanju aplikacije Pravoslavac.
Modul mora biti aktivan u LSPosed-u (scope: `com.pravoslavac`).
