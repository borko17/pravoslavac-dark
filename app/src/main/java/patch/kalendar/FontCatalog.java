package patch.kalendar;

/** Spisak Google fontova sa ćirilicom i zajedničke konstante (bez Xposed zavisnosti: koristi ga i aplikacija modula). */
final class FontCatalog {
    private FontCatalog() {}

    /** Paket (applicationId) aplikacije modula: iz njenih postavki Pravoslavac čita preuzete fontove. */
    static final String MODULE_APP_ID = "io.github.borko17.pravoslavac.dark";
    /** Aktivnost modula (spisak fontova za preuzimanje). */
    static final String MODULE_ACTIVITY = "patch.kalendar.MainActivity";
    /** ContentProvider modula (FontProvider): preko njega prozor u Pravoslavcu trazi od modula mrezu (mora biti isto kao u manifestu). */
    static final String PROVIDER_AUTHORITY = MODULE_APP_ID + ".fonts";
    /** Postavke modula: spisak preuzetih fontova ("list", odvojeni sa |) i verzije ("v_<slug>"). */
    static final String PREFS_INDEX = "fonts_index";
    /** Prefiks postavki jednog fonta: "reg" i "bold" (base64 TTF) i "ver". */
    static final String PREFS_FONT = "font_";
    /** Tekst pregleda fonta: pangram sa svim slovima srpske ćirilice (pregled odmah pokaže ako font nema neko slovo). */
    static final String PREVIEW_TEXT = "Хаџи Ђера је заћутао и бацио чежњив поглед на шољу с кафом.";
    /** Lazni direktorij asseta preko kojeg se fontovi serviraju WebView stranicama. */
    static final String WEB_DIR = "__pdkfonts__";

    /**
     * Fontovi sa ćirilicom (uključujući srpska slova). Preuzeti font se ionako provjerava (Ж Ђ Ћ Џ Ј Љ Њ),
     * pa pogrešan unos ovdje samo daje poruku o grešci pri preuzimanju.
     */
    static final String[] FAMILIES = {
            "Alice", "Balsamiq Sans", "Gabriela",
    };

    /** "Open Sans" -> "open_sans" (ime fajla i CSS familije). */
    static String slug(String family) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < family.length(); i++) {
            char c = Character.toLowerCase(family.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) sb.append(c);
            else if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '_') sb.append('_');
        }
        return sb.toString();
    }

    /** Familija iz spiska sa datim slug-om, ili null. */
    static String bySlug(String slug) {
        for (String f : FAMILIES) {
            if (slug(f).equals(slug)) return f;
        }
        return null;
    }
}
