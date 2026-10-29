package patch.kalendar;

/** Tabela boja koje se mogu podesiti u prilagođenoj temi (bez Xposed zavisnosti). */
final class Colors {
    private Colors() {}

    /** {kljuc, naziv, podrazumijevana boja (RRGGBB)} */
    static final String[][] ALL = {
            { "c_bg",         "Pozadina",                           "121212" },
            { "c_surface",    "Pozadina menija",                    "1a1a18" },
            { "c_bar",        "Tabovi",                             "1f2a3f" },
            { "c_text",       "Tekst kalendar i podmeni",           "e0dcd0" },
            { "c_divider",    "Razdjelnici (kalendar i meni)",      "333333" },
            { "c_red_line",   "Razdjelnici (liste molitvi)",        "dc143c" },
            { "c_row",        "Kalendar: neparni dani",             "1f1f1d" },
            { "c_row_border", "Kalendar: okvir današnjeg dana",     "2f5f99" },
            { "c_title",      "Crveni naslovi i praznici",          "ff7a66" },
            { "c_red_bar",    "Crvena traka i klizač",              "8b0000" },
            { "c_web_bg",     "Pozadina knjiga i žitija",           "121212" },
            { "c_web_text",   "Slova u listama knjiga i molitvi",   "d9d4c7" },
            { "c_tab_text",   "Slova na tabovima",                  "ffffff" },
            { "c_month_bar",  "Traka mjeseca i godine",             "1d1a13" },
    };

    /** Boje koje se ne uređuju u aplikaciji, ali ih tema i dalje ima (ostaju sa vrijednostima iz teme). */
    static final String[][] HIDDEN = {
            { "c_bar_dark",   "Statusna traka",              "0f1626" },
            { "c_accent",     "Akcent (prekidači)",          "6495ed" },
            { "c_web_head2",  "Podnaslovi (h5, h6)",         "e6e1d6" },
            { "c_web_link",   "Linkovi",                     "8ab4f8" },
            { "c_web_visited","Posjećeni linkovi",           "c4a5f0" },
            { "c_web_line",   "Linije i tabele",             "444444" },
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
