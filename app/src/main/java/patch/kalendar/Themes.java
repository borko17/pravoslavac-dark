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
            if ("c_web_mark".equals(key)) {
                // starije teme: svijetle zadrzavaju originalnu zelenu, tamne dobijaju svijetlozelenu
                Integer b = Colors.parse(color("c_web_bg"));
                double lum = b == null ? 0 : (0.299 * ((b.intValue() >> 16) & 255)
                        + 0.587 * ((b.intValue() >> 8) & 255) + 0.114 * (b.intValue() & 255)) / 255.0;
                return lum >= 0.5 ? "008000" : "77f877";
            }
            // nove boje kalendara: starije teme dobijaju boje koje se uklapaju u njih (kontrast je vec provjeren)
            if ("c_holiday".equals(key) || "c_menu_text".equals(key)) return color("c_title");
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
            "c_arrows", "c_cal_icon", "c_toolbar", "c_arrow_pressed" };

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
            if (!"c_toolbar".equals(k) && !"c_arrow_pressed".equals(k)) t.colors.put(k, Colors.def(k));
        }
        materialize(t);   // gornja traka i boja dodira: iz boja teme (isti izgled kao do sada)
        return t;
    }

    static List<Theme> builtins() {
        List<Theme> l = new ArrayList<Theme>();
        l.add(make("orig", "Тамна",
                "121212", "181818", "121212", "1b1c1e", "7546e1", "e1e6ea", "f07a7d", "97f07f", "cfd7de", "ffffff",
                "9194f2", "e293e7", "1f2538", "1d1f23", "b02e31", "dd3c3f", "b02e31", "353637", "e93f43"));
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
        if (out.isEmpty()) out = builtins();
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

    /** Tema sa datim id-jem, a ako je nema - prva u listi. */
    static Theme find(List<Theme> list, String id) {
        for (Theme t : list) {
            if (t.id.equals(id)) return t;
        }
        return list.get(0);
    }
}
