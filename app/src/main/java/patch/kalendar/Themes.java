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
            return c != null ? Colors.hex(c.intValue()) : Colors.def(key);
        }

        /** Potpuna kopija (sve boje se upisuju eksplicitno). */
        Theme copy(String newId, String newName) {
            Theme t = new Theme(newId, newName);
            for (String[] c : Colors.ALL) t.colors.put(c[0], color(c[0]));
            return t;
        }
    }

    // redoslijed = Colors.ALL: bg, surface, bar, bar_dark, text, accent, divider, red_line, row, row_border,
    //                          title, red_bar, web_text, web_head2, web_link, web_visited, web_line
    private static Theme make(String id, String name, String... hex) {
        Theme t = new Theme(id, name);
        for (int i = 0; i < Colors.ALL.length; i++) t.colors.put(Colors.ALL[i][0], hex[i]);
        return t;
    }

    static List<Theme> builtins() {
        List<Theme> l = new ArrayList<Theme>();
        l.add(make("orig", "Tamna (originalna)",
                "121212", "1a1a18", "1f2a3f", "0f1626", "e0dcd0", "6495ed", "333333", "dc143c", "1f1f1d", "2f5f99",
                "ff7a66", "8b0000", "d9d4c7", "e6e1d6", "8ab4f8", "c4a5f0", "444444"));
        l.add(make("amoled", "AMOLED crna",
                "000000", "050505", "0d1522", "000000", "e6e6e6", "6495ed", "222222", "b3243a", "0b0b0b", "1f4a7a",
                "ff7a66", "7a0f1a", "d2d2d2", "eeeeee", "8ab4f8", "c4a5f0", "333333"));
        l.add(make("sepia", "Topla sepija",
                "1c1712", "241e17", "4a3520", "2e2012", "e8dcc4", "d9a441", "3a3128", "b5533c", "2a231b", "8a6a2f",
                "ff8a65", "7a2e1f", "e0d4bc", "f0e6d0", "e0b060", "c9a0dc", "4a4033"));
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
                    out.add(t);
                }
            } catch (Throwable e) {
                out.clear();
            }
        }
        if (out.isEmpty()) out = builtins();
        return out;
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
