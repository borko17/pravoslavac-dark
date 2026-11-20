package patch.kalendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Teme (skupovi boja): model, ugradjene teme i (de)serijalizacija u JSON. Bez Xposed zavisnosti. */
final class Themes {
    private Themes() {}

    static final String KEY_THEMES = "themes";
    static final String KEY_SEL = "theme_sel";
    /** Id ugradjene teme "Православац": originalne boje aplikacije (modul ne dira boje). Samo za izbor. */
    static final String ORIG_ID = "pravoslavac";

    static final class Theme {
        final String id;
        String name;
        /** kljuc (iz Colors.ALL) -> "rrggbb"; nedostajuci kljucevi se uzimaju iz Colors.def. */
        final Map<String, String> colors = new LinkedHashMap<String, String>();

        Theme(String id, String name) {
            this.id = id;
            this.name = name;
        }

        /** "rrggbb" za dati kljuc (sa podrazumijevanom vrijednoscu ako nije podesen). */
        String color(String key) {
            Integer c = Colors.parse(colors.get(key));
            if (c != null) return Colors.hex(c.intValue());
            if ("c_web_bg".equals(key)) return color("c_bg");   // starije teme: pozadina knjiga = opsta pozadina
            if ("c_scroll".equals(key)) return color("c_red_bar");  // starije teme: klizač = boja crvenih linija
            if ("c_set_bg".equals(key)) return color("c_bg");        // starije teme: podesavanja su u bojama kalendara
            if ("c_set_text".equals(key)) return color("c_text");
            if ("c_switch".equals(key)) return color("c_accent");
            if ("c_web_body".equals(key)) return color("c_web_text");   // starije teme: tekst stranica = boja liste knjiga i molitvi
            if ("c_web_mark".equals(key)) {
                // starije teme: svijetle zadrzavaju originalnu zelenu, tamne dobijaju svijetlozelenu
                Integer b = Colors.parse(color("c_web_bg"));
                double lum = b == null ? 0 : (0.299 * ((b.intValue() >> 16) & 255)
                        + 0.587 * ((b.intValue() >> 8) & 255) + 0.114 * (b.intValue() & 255)) / 255.0;
                return lum >= 0.5 ? "008000" : "77f877";
            }
            // nove boje kalendara: starije teme dobijaju boje koje se uklapaju u njih (kontrast je vec provjeren)
            if ("c_holiday".equals(key) || "c_menu_text".equals(key)) return color("c_title");
            if ("c_day_date".equals(key) || "c_day_name".equals(key) || "c_meat".equals(key)) return color("c_text");
            if ("c_fast".equals(key)) return color("c_holiday");
            if ("c_arrow_pressed".equals(key)) return color("c_accent");
            if ("c_toolbar".equals(key)) {
                Integer bg = Colors.parse(color("c_bg"));
                return Dark.lum(bg == null ? 0 : bg.intValue()) > 0.5f ? color("c_bar") : "212121";
            }
            if ("c_menu_divider".equals(key)) return color("c_divider");
            if ("c_cal_title_line".equals(key)) return color("c_bg");
            if ("c_sub_text1".equals(key)) return color("c_sub_text");
            if ("c_sub_text".equals(key)) return color("c_text");
            if ("c_sun_bg".equals(key)) return color("c_row");
            if ("c_cal_title_bg".equals(key)) return color("c_bg");
            if ("c_sun_text".equals(key) || "c_month_text".equals(key) || "c_cal_title_text".equals(key)) {
                return color("c_title");
            }
            return Colors.def(key);
        }

        /** Potpuna kopija (sve boje se upisuju eksplicitno). */
        Theme copy(String newId, String newName) {
            Theme t = new Theme(newId, newName);
            for (String[] c : Colors.ALL) t.colors.put(c[0], color(c[0]));
            for (String[] c : Colors.HIDDEN) t.colors.put(c[0], color(c[0]));
            return t;
        }
    }

    /** Redoslijed vrijednosti u ugrađenim temama ispod (ne mijenjati bez izmjene tih nizova). */
    private static final String[] ORDER = {
            "c_bg", "c_surface", "c_web_bg", "c_row", "c_row_border", "c_text", "c_title", "c_web_mark", "c_web_text",
            "c_tab_text", "c_web_link", "c_web_visited", "c_bar", "c_month_bar", "c_scroll", "c_tab_line",
            "c_red_bar", "c_divider", "c_red_line" };

    /** Boje kalendara koje svaka tema ima kao zasebne vrijednosti (ne prate druge boje). */
    private static final String[] CAL_KEYS = { "c_sun_bg", "c_sun_text", "c_month_text", "c_cal_title_bg",
            "c_cal_title_text", "c_holiday", "c_sub_text", "c_menu_text",
            "c_menu_divider", "c_cal_title_line", "c_sub_text1", "c_btn_bg", "c_btn_text", "c_btn_border",
            "c_arrows", "c_cal_icon", "c_toolbar", "c_arrow_pressed", "c_toolbar_text", "c_day_date", "c_day_name", "c_meat", "c_fast",
            "c_web_body", "c_set_bg", "c_set_text", "c_switch" };

    /**
     * Starije teme (sacuvane prije ovih boja) dobijaju zasebne vrijednosti: jednom se uzmu iz boja uklopljenih u
     * temu, pa se poslije mogu mijenjati nezavisno od njih.
     */
    private static void materialize(Theme t) {
        for (String k : CAL_KEYS) {
            if (!t.colors.containsKey(k)) t.colors.put(k, t.color(k));
        }
    }

    private static Theme make(String id, String name, String... hex) {
        Theme t = new Theme(id, name);
        for (int i = 0; i < ORDER.length; i++) t.colors.put(ORDER[i], hex[i]);
        for (String k : CAL_KEYS) {
            if (!"c_toolbar".equals(k) && !"c_arrow_pressed".equals(k) && !"c_day_date".equals(k)
                    && !"c_day_name".equals(k) && !"c_meat".equals(k) && !"c_fast".equals(k)
                    && !"c_web_body".equals(k) && !"c_set_bg".equals(k) && !"c_set_text".equals(k)
                    && !"c_switch".equals(k)) {
                t.colors.put(k, Colors.def(k));
            }
        }
        materialize(t);   // gornja traka i boja dodira: iz boja teme (isti izgled kao do sada)
        return t;
    }

    /** Tema "Православац": prva u listi; boje se ne primjenjuju (vrijednosti su samo za mali pregled u listi). */
    static Theme origApp() {
        Theme t = new Theme(ORIG_ID, "Православац");
        t.colors.put("c_bg", "faf5e9");
        t.colors.put("c_row", "fbf7ea");
        t.colors.put("c_holiday", "cc0000");
        t.colors.put("c_row_border", "33b5e5");
        return t;
    }

    static boolean isOrig(String id) {
        return ORIG_ID.equals(id);
    }

    static List<Theme> builtins() {
        List<Theme> l = new ArrayList<Theme>();
        l.add(origApp());
        Theme dark = new Theme("orig", "Тамна");
        dark.colors.put("c_bg", "0a0c0e");
        dark.colors.put("c_surface", "101316");
        dark.colors.put("c_web_bg", "0a0c0e");
        dark.colors.put("c_row", "161c21");
        dark.colors.put("c_row_border", "484def");
        dark.colors.put("c_text", "e3e6e8");
        dark.colors.put("c_title", "efaf8a");
        dark.colors.put("c_holiday", "f37779");
        dark.colors.put("c_sub_text", "d5dbdd");
        dark.colors.put("c_sub_text1", "d5d7dd");
        dark.colors.put("c_day_date", "e0e4e6");
        dark.colors.put("c_day_name", "e0e3e6");
        dark.colors.put("c_meat", "e0e1e6");
        dark.colors.put("c_fast", "f3778a");
        dark.colors.put("c_btn_bg", "a90e11");
        dark.colors.put("c_btn_text", "f8f6f6");
        dark.colors.put("c_btn_border", "cd7678");
        dark.colors.put("c_arrows", "eeacaa");
        dark.colors.put("c_cal_icon", "d14e47");
        dark.colors.put("c_toolbar", "1a2025");
        dark.colors.put("c_toolbar_text", "ffffff");
        dark.colors.put("c_arrow_pressed", "55454a");
        dark.colors.put("c_menu_text", "f093a4");
        dark.colors.put("c_web_text", "d2d6db");
        dark.colors.put("c_web_body", "d2d6db");
        dark.colors.put("c_set_bg", "0a0c0e");
        dark.colors.put("c_set_text", "e3e6e8");
        dark.colors.put("c_switch", "4c88f0");
        dark.colors.put("c_tab_text", "ffffff");
        dark.colors.put("c_web_link", "7ba7f4");
        dark.colors.put("c_web_visited", "d9a1ed");
        dark.colors.put("c_web_mark", "9ef47b");
        dark.colors.put("c_bar", "1a2025");
        dark.colors.put("c_tab_line", "dd3c3f");
        dark.colors.put("c_month_bar", "141921");
        dark.colors.put("c_scroll", "ac2e30");
        dark.colors.put("c_divider", "2f3133");
        dark.colors.put("c_menu_divider", "303336");
        dark.colors.put("c_cal_title_line", "2c2e30");
        dark.colors.put("c_red_bar", "ac2e30");
        dark.colors.put("c_red_line", "ee3a3d");
        dark.colors.put("c_sun_bg", "181e2c");
        dark.colors.put("c_sun_text", "f585a3");
        dark.colors.put("c_month_text", "f59585");
        dark.colors.put("c_cal_title_bg", "12151b");
        dark.colors.put("c_cal_title_text", "f5b185");
        dark.colors.put("c_bar_dark", "10172d");
        dark.colors.put("c_accent", "4c88f0");
        dark.colors.put("c_web_head2", "e8ebed");
        dark.colors.put("c_web_line", "47494b");
        l.add(dark);
        return l;
    }

    /** Boje ugradjene teme sa datim id-jem, ili null ako to nije ugradjena tema. */
    static Map<String, String> builtinColors(String id) {
        for (Theme t : builtins()) {
            if (t.id.equals(id)) return new LinkedHashMap<String, String>(t.colors);
        }
        return null;
    }

    static List<Theme> parse(String json) {
        List<Theme> out = new ArrayList<Theme>();
        if (json != null) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    Theme t = new Theme(o.getString("id"), o.optString("name", "Tema"));
                    JSONObject c = o.optJSONObject("colors");
                    if (c != null) {
                        Iterator<String> it = c.keys();
                        while (it.hasNext()) {
                            String k = it.next();
                            Integer v = Colors.parse(c.optString(k, null));
                            if (v != null) t.colors.put(k, Colors.hex(v.intValue()));
                        }
                    }
                    materialize(t);
                    out.add(t);
                }
            } catch (Throwable e) {
                out.clear();
            }
        }
        // "Православац" je uvijek prva i nikad se ne cuva u postavkama
        for (int i = out.size() - 1; i >= 0; i--) {
            if (isOrig(out.get(i).id)) out.remove(i);
        }
        if (out.isEmpty()) {
            out = builtins();
        } else {
            out.add(0, origApp());
        }
        return out;
    }

    /**
     * Teme iz JSON-a (npr. iz klipborda) ili null ako tekst nije ispravan niz tema. Za razliku od parse()
     * ne vraca ugradjene teme; id-jevi iz teksta se ne koriste (pozivalac dodjeljuje nove).
     */
    static List<Theme> parseImport(String json) {
        if (json == null) return null;
        String s = json.trim();
        if (!s.startsWith("[")) return null;
        List<Theme> out = new ArrayList<Theme>();
        try {
            JSONArray arr = new JSONArray(s);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                JSONObject c = o.optJSONObject("colors");
                if (c == null) continue;
                Theme t = new Theme("", o.optString("name", "Тема").trim());
                if (t.name.length() == 0) t.name = "Тема";
                Iterator<String> it = c.keys();
                while (it.hasNext()) {
                    String k = it.next();
                    Integer v = Colors.parse(c.optString(k, null));
                    if (v != null) t.colors.put(k, Colors.hex(v.intValue()));
                }
                if (!t.colors.isEmpty()) {
                    materialize(t);
                    out.add(t);
                }
            }
        } catch (Throwable e) {
            return null;
        }
        return out.isEmpty() ? null : out;
    }

    static String toJson(List<Theme> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Theme t : list) {
                if (isOrig(t.id)) continue;   // ugradjena tema se ne cuva
                JSONObject o = new JSONObject();
                o.put("id", t.id);
                o.put("name", t.name);
                JSONObject c = new JSONObject();
                for (Map.Entry<String, String> e : t.colors.entrySet()) c.put(e.getKey(), e.getValue());
                o.put("colors", c);
                arr.put(o);
            }
        } catch (JSONException e) {
            // ne bi se trebalo desiti
        }
        return arr.toString();
    }

    /** Tema sa datim id-jem, a ako je nema - prva koja nije "Православац" (pa tek onda prva u listi). */
    static Theme find(List<Theme> list, String id) {
        for (Theme t : list) {
            if (t.id.equals(id)) return t;
        }
        for (Theme t : list) {
            if (!isOrig(t.id)) return t;
        }
        return list.get(0);
    }
}
