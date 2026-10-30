# Pravoslavac Dark (LSPosed modul)

Xposed/LSPosed modul za Android aplikaciju **Православац** (`com.pravoslavac`).

Modul nema zasebnu aplikaciju ni ikonu u launcheru: dovoljno je aktivirati ga u LSPosed-u.
Podešavanja su unutar aplikacije Pravoslavac (Meni → Подешавања), u dva taba (**Opcije** i **Teme**):

- **Preskoči uvodni ekran** - Aplikacija se otvara bez uvodnog ekrana,
- **Tekst poravnat lijevo** - umjesto obostranog poravnanja (justified).
- **Aplikacija otvara kalendar** - pri pokretanju se otvara kalendar (tekući mjesec) umjesto žitija za taj dan,

- **Teme** - uključuje tamnu temu (boje aplikacije, liste, redovi kalendara, WebView sadrzaj preko CSS/JS-a)
  u bojama odabrane teme;
  isključeno = originalne boje aplikacije. Ispod prekidaca je lista tema
  (uključene 3: *Tamna (originalna)*, *AMOLED crna*, *Topla sepija*) iz koje se bira jedna.
  Meni **⋮** pored teme:
  - uredi boje (dodir na kvadrat boje otvara paletu, ili upis hex sifre),
  - preimenuj,
  - obriši;
  **+ Nova tema** pravi kopiju odabrane teme.
  - "Vrati" u uređivanju vraća boje ugrađene teme.

Modul mora biti aktivan u LSPosed-u (scope: `com.pravoslavac`).
