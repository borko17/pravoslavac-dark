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

    private static synchronized boolean get(String key) {
        try {
            if (xp == null) {
                xp = new XSharedPreferences(MODULE_PKG, FILE);
                xp.makeWorldReadable();
            }
            long now = System.currentTimeMillis();
            if (now - lastCheck > 1000L) {
                lastCheck = now;
                if (xp.hasFileChanged()) xp.reload();
            }
            return xp.getBoolean(key, true);
        } catch (Throwable t) {
            return true; // podrazumijevano: sve ukljuceno
        }
    }

    static boolean openCalendar() { return get(KEY_OPEN_CALENDAR); }

    static boolean skipSplash() { return get(KEY_SKIP_SPLASH); }

    static boolean dark() { return get(KEY_DARK); }

    static boolean left() { return get(KEY_LEFT); }
}
