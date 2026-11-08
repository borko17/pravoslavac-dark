package patch.kalendar;

/** Tabela boja koje se mogu podesiti u prilagođenoj temi (bez Xposed zavisnosti). */
final class Colors {
    private Colors() {}

    /** {kljuc, naziv, podrazumijevana boja (RRGGBB)}; redoslijed je redoslijed prikaza (po grupama, vidi GROUPS). */
    static final String[][] ALL = {
            // Позадине
            { "c_bg",         "Позадина календара",                 "121212" },
            { "c_surface",    "Позадина менија",                    "181818" },
            { "c_web_bg",     "Позадина књига и житија",            "121212" },
            // Календар
            { "c_row",        "Непарни дани",                       "1b1c1e" },
            { "c_row_border", "Оквир данашњег дана",                "7546e1" },
            // Текст
            { "c_text",       "Календар и подмени",                 "e1e6ea" },
            { "c_title",      "Наслови, празници, постови",         "f07a7d" },
            { "c_web_mark",   "Ознаке верзија (Српски, Црквенословенски)", "97f07f" },
            { "c_web_text",   "Листе књига и молитви",              "cfd7de" },
            { "c_tab_text",   "Слова на табовима",                  "ffffff" },
            { "c_web_link",   "Линкови",                            "9194f2" },
            { "c_web_visited","Посећени линкови",                   "e293e7" },
            // Елементи
            { "c_bar",        "Табови",                             "1f2538" },
            { "c_month_bar",  "Трака месеца и године",              "1d1f23" },
            { "c_scroll",     "Клизач",                             "b02e31" },
            // Линије
            { "c_tab_line",   "Испод активног таба",                "dd3c3f" },
            { "c_red_bar",    "Испод датума у календару",           "b02e31" },
            // Разделници
            { "c_divider",    "Календар и мени",                    "353637" },
            { "c_red_line",   "Листе молитви и књига",              "e93f43" },
    };

    /** {naslov grupe, kljuc prve boje u grupi}: naslov se prikazuje ispred te boje u ALL. */
    static final String[][] GROUPS = {
            { "Позадине",     "c_bg" },
            { "Календар",     "c_row" },
            { "Текст",        "c_text" },
            { "Елементи",     "c_bar" },
            { "Линије",       "c_tab_line" },
            { "Разделници",   "c_divider" },
    };

    /** Naslov grupe koja pocinje ovom bojom, ili null. */
    static String groupBefore(String key) {
        for (String[] g : GROUPS) if (g[1].equals(key)) return g[0];
        return null;
    }

    /** Boje koje se ne uređuju u aplikaciji, ali ih tema i dalje ima (ostaju sa vrijednostima iz teme). */
    static final String[][] HIDDEN = {
            { "c_bar_dark",   "Статусна трака",              "0f1626" },
            { "c_accent",     "Акцент (прекидачи)",          "6495ed" },
            { "c_web_line",   "Линије и табеле",             "444444" },
    };

    static String def(String key) {
        for (String[] c : ALL) if (c[0].equals(key)) return c[2];
        for (String[] c : HIDDEN) if (c[0].equals(key)) return c[2];
        return "000000";
    }

    /** Neuređive boje (ne prikazuju se u listi). */
    static boolean hidden(String key) {
        for (String[] c : HIDDEN) if (c[0].equals(key)) return true;
        return false;
    }

    /** "#rgb", "#rrggbb", "rgb" ili "rrggbb" -> 0xffRRGGBB; null ako nije ispravno. */
    static Integer parse(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() == 3) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 3; i++) sb.append(s.charAt(i)).append(s.charAt(i));
            s = sb.toString();
        }
        if (s.length() != 6) return null;
        int v = 0;
        for (int i = 0; i < 6; i++) {
            int d = Character.digit(s.charAt(i), 16);
            if (d < 0) return null;
            v = (v << 4) | d;
        }
        return Integer.valueOf(0xff000000 | v);
    }

    /** 0xffRRGGBB -> "rrggbb" */
    static String hex(int c) {
        String h = Integer.toHexString(c & 0xffffff);
        while (h.length() < 6) h = "0" + h;
        return h;
    }
}
