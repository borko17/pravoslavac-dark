# Pravoslavac Dark (LSPosed modul)

Xposed/LSPosed modul za Android aplikaciju **Православац** (`com.pravoslavac`).

Aplikacija modula ima svoj ekran (ikona "Pravoslavac kalendar patch" u launcheru) s prekidacima:

- **Pocetni ekran je kalendar** - pri pokretanju se otvara kalendar (tekuci mjesec) umjesto zitija za taj dan,
- **Preskoci uvodni ekran** - pocetni ekran je odmah kalendar (tekuci mjesec),
- **Tamna tema** - boje aplikacije, liste, redovi kalendara, WebView sadrzaj preko CSS/JS-a
  (ako je uvodni ekran ukljucen, pozadina mu je crna),
- **Tekst poravnat lijevo** - umjesto obostranog poravnanja (justified).

Promjene se primjenjuju pri sljedecem pokretanju aplikacije Pravoslavac.
Modul mora biti aktivan u LSPosed-u (scope: `com.pravoslavac`).
