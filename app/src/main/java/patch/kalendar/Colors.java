package patch.kalendar;

/** Tabela boja koje se mogu podesiti u prilagođenoj temi (bez Xposed zavisnosti). */
final class Colors {
    private Colors() {}

    /** {kljuc, naziv, podrazumijevana boja (RRGGBB)}; redoslijed je redoslijed prikaza (po grupama, vidi GROUPS). */
    static final String[][] ALL = {
            // Позадине
            { "c_bg",            "Позадина календара",                  "121212" },
            { "c_surface",        "Позадина менија",                    "181818" },
            { "c_web_bg",         "Позадина књига и житија",            "121212" },
            { "c_row",            "Непарни дани",                       "1b1c1e" },
            { "c_sun_bg",         "Позадина недељне траке",             "272a30" },
            { "c_month_bar",      "Позадина месеца и године",           "1d1f23" },
            { "c_cal_title_bg",   "Позадина наслова испод месеца",      "181a1d" },
            // Текст
            { "c_text",           "Текст календара",                    "e1e6ea" },
            { "c_menu_text",      "Текст менија",                       "f58f92" },
            { "c_sub_text1",      "Текст подменија 1",                  "d5dbe0" },
            { "c_sub_text",       "Текст подменија 2",                  "d5dbe0" },
            { "c_title",          "Текст наслова",                      "f07a7d" },
            { "c_holiday",        "Празници и постови",                 "ff8a7a" },
            { "c_sun_text",       "Текст недељне траке",                "f5a0a2" },
            { "c_month_text",     "Текст месеца и године",              "ff9a9c" },
            { "c_cal_title_text", "Текст наслова испод месеца",         "f07a7d" },
            { "c_web_mark",       "Српски | Црквенословенски",          "97f07f" },
            { "c_web_text",       "Листе књига и молитви",              "cfd7de" },
            { "c_tab_text",       "Слова на табовима",                  "ffffff" },
            { "c_web_link",       "Линкови",                            "9194f2" },
            { "c_web_visited",    "Посећени линкови",                   "e293e7" },
            // Елементи
            { "c_bar",            "Табови",                             "1f2538" },
            { "c_scroll",         "Клизач",                             "b02e31" },
            { "c_row_border",     "Оквир данашњег дана",                "7546e1" },
            // Линије и разделници
            { "c_tab_line",       "Испод активног таба",                "dd3c3f" },
            { "c_red_bar",        "Испод датума у календару",           "b02e31" },
            { "c_cal_title_line", "Линије око наслова испод месеца",    "2e3034" },
            { "c_divider",        "Разделници календара",               "353637" },
            { "c_menu_divider",   "Разделници менија",                  "353637" },
            { "c_red_line",       "Листе молитви и књига",              "e93f43" },
    };

    /** {naslov grupe, kljuc prve boje u grupi}: naslov se prikazuje ispred te boje u ALL. */
    static final String[][] GROUPS = {
            { "Позадине",     "c_bg" },
            { "Текст",        "c_text" },
            { "Елементи",     "c_bar" },
            { "Линије и разделници", "c_tab_line" },
    };

    /** Naslov grupe koja pocinje ovom bojom, ili null. */
    static String groupBefore(String key) {
        for (String[] g : GROUPS) if (g[1].equals(key)) return g[0];
        return null;
    }

    /** Boje koje se ne uređuju u aplikaciji, ali ih tema i dalje ima (ostaju sa vrijednostima iz teme). */
    static final String[][] HIDDEN = {
            { "c_bar_dark",   "Статусна трака",          "0f1626" },
            { "c_accent",     "Акцент (прекидачи)",      "6495ed" },
            { "c_web_line",   "Линије и табеле",         "444444" },
            { "c_web_head2",  "Наслови h5/h6",           "e6e1d6" },
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
