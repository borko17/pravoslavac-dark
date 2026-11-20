package patch.kalendar;

import org.json.JSONObject;

import java.io.File;

import de.robv.android.xposed.XSharedPreferences;

/** Cita postavke koje korisnik podesi u Podesavanjima unutar Pravoslavca (SettingsUi). Izvrsava se u procesu Pravoslavca. */
final class Prefs {
    private Prefs() {}

    static final String MODULE_PKG = "patch.kalendar";
    /** Fajl postavki u podacima Pravoslavca (pise ga SettingsUi, cita ovaj hook). */
    static final String FILE = "patch_settings";
    /** Fajl koji je pisala stara aplikacija modula (koristi se samo za jednokratni uvoz). */
    static final String OLD_FILE = "settings";

    static final String KEY_OPEN_CALENDAR = "open_calendar";
    static final String KEY_SKIP_SPLASH = "skip_splash";
    static final String KEY_DARK = "dark";
    static final String KEY_LEFT = "left_align";
    /** JSON {"f": {kljuc teksta: naziv fonta}, "s": {kljuc teksta: velicina u %}} (fontovi ne zavise od teme). */
    static final String KEY_FONTS = "fonts";

    private static String dataDir;
    private static XSharedPreferences xp;
    private static long lastCheck;
    private static boolean force;

    /** Postavlja direktorij podataka Pravoslavca (poziva se cim je poznat). */
    static void init(String dir) {
        if (dir != null && dir.length() > 0) dataDir = dir;
    }

    /** Odmah ponovo ucitaj postavke iz fajla (poslije izmjene u Podesavanjima). */
    static synchronized void refresh() {
        force = true;
    }

    private static XSharedPreferences sp() {
        if (xp == null) {
            String dir = dataDir != null ? dataDir : "/data/data/" + Hook.PKG;
            xp = new XSharedPreferences(new File(dir, "shared_prefs/" + FILE + ".xml"));
        }
        long now = System.currentTimeMillis();
        if (force) {
            force = false;
            lastCheck = now;
            xp.reload();
        } else if (now - lastCheck > 1000L) {
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

    /** Direktorij podataka aplikacije u cijem je procesu modul ucitan. */
    static String dir() {
        return dataDir != null ? dataDir : "/data/data/" + Hook.PKG;
    }

    private static String lastFonts;
    private static JSONObject fontsObj;

    private static JSONObject fonts() {
        String s = sp().getString(KEY_FONTS, null);
        if (fontsObj != null && eq(s, lastFonts)) return fontsObj;
        JSONObject o;
        try {
            o = s == null ? new JSONObject() : new JSONObject(s);
        } catch (Throwable t) {
            o = new JSONObject();
        }
        lastFonts = s;
        fontsObj = o;
        return o;
    }

    /** Naziv fonta izabranog za dati tekst (kljuc iz Colors), ili null (originalni font). */
    static synchronized String font(String key) {
        try {
            JSONObject f = fonts().optJSONObject("f");
            String v = f == null ? "" : f.optString(key, "");
            return v.length() == 0 ? null : v;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Da li se preuzeti fontovi NE primjenjuju na crkvenoslovenske fontove (triodion-ieUcs i IrmolSkun_0). */
    static synchronized boolean keepSlavonic() {
        return true;
    }

    /** Velicina teksta u procentima originalne (50..200, podrazumijevano 100). */
    static synchronized int fontSize(String key) {
        try {
            JSONObject s = fonts().optJSONObject("s");
            int v = s == null ? 100 : s.optInt(key, 100);
            return v < 50 ? 50 : (v > 200 ? 200 : v);
        } catch (Throwable t) {
            return 100;
        }
    }

    /** Boja za dati kljuc iz odabrane teme (podrazumijevana ako nije podesena). */
    static synchronized int color(String key) {
        return Colors.parse(theme().color(key)).intValue();
    }

    static boolean openCalendar() { return get(KEY_OPEN_CALENDAR, true); }

    static boolean skipSplash() { return get(KEY_SKIP_SPLASH, true); }

    /**
     * Boje odabrane teme se primjenjuju. Netacno je kad je izabrana tema "Православац" (originalne boje aplikacije);
     * tada se boje ne diraju, a fontovi i ostale opcije rade i dalje. (Stari prekidac KEY_DARK = iskljucen vrijedi
     * kao "Православац" dok se postavke ne otvore.)
     */
    static boolean dark() {
        if (!get(KEY_DARK, true)) return false;
        try {
            return !Themes.isOrig(sp().getString(Themes.KEY_SEL, null));
        } catch (Throwable t) {
            return true;
        }
    }

    static boolean left() { return get(KEY_LEFT, true); }
}
