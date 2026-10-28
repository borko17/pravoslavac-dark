package patch.kalendar;

import de.robv.android.xposed.XSharedPreferences;

/** Cita postavke koje korisnik podesi u aplikaciji modula (MainActivity). Izvrsava se u procesu Pravoslavca. */
final class Prefs {
    private Prefs() {}

    static final String MODULE_PKG = "patch.kalendar";
    static final String FILE = "settings";

    static final String KEY_OPEN_CALENDAR = "open_calendar";
    static final String KEY_SKIP_SPLASH = "skip_splash";
    static final String KEY_DARK = "dark";
    static final String KEY_LEFT = "left_align";

    private static XSharedPreferences xp;
    private static long lastCheck;

    private static XSharedPreferences sp() {
        if (xp == null) {
            xp = new XSharedPreferences(MODULE_PKG, FILE);
            xp.makeWorldReadable();
        }
        long now = System.currentTimeMillis();
        if (now - lastCheck > 1000L) {
            lastCheck = now;
            if (xp.hasFileChanged()) xp.reload();
        }
        return xp;
    }

    private static synchronized boolean get(String key, boolean def) {
        try {
            return sp().getBoolean(key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    private static String lastJson, lastSel;
    private static Themes.Theme lastTheme;

    /** Trenutno odabrana tema (kesirana dok se postavke ne promijene). */
    private static Themes.Theme theme() {
        try {
            String json = sp().getString(Themes.KEY_THEMES, null);
            String sel = sp().getString(Themes.KEY_SEL, null);
            if (lastTheme != null && eq(json, lastJson) && eq(sel, lastSel)) return lastTheme;
            lastTheme = Themes.find(Themes.parse(json), sel);
            lastJson = json;
            lastSel = sel;
            return lastTheme;
        } catch (Throwable t) {
            return Themes.builtins().get(0);
        }
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    /** Boja za dati kljuc iz odabrane teme (podrazumijevana ako nije podesena). */
    static synchronized int color(String key) {
        return Colors.parse(theme().color(key)).intValue();
    }

    static boolean openCalendar() { return get(KEY_OPEN_CALENDAR, true); }

    static boolean skipSplash() { return get(KEY_SKIP_SPLASH, true); }

    /** Prekidac "Teme": ukljucen = tamna tema u bojama odabrane teme, iskljucen = originalne boje. */
    static boolean dark() { return get(KEY_DARK, true); }

    static boolean left() { return get(KEY_LEFT, true); }
}
