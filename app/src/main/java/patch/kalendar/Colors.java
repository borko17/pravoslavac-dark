package patch.kalendar;

/** Tabela boja koje se mogu podesiti u prilagođenoj temi (bez Xposed zavisnosti). */
final class Colors {
    private Colors() {}

    /** {kljuc, naziv, podrazumijevana boja (RRGGBB)} */
    static final String[][] ALL = {
            { "c_bg",         "Pozadina",                    "121212" },
            { "c_surface",    "Pozadina lista",              "1a1a18" },
            { "c_bar",        "Gornja traka i tabovi",       "1f2a3f" },
            { "c_bar_dark",   "Statusna traka",              "0f1626" },
            { "c_text",       "Tekst (aplikacija)",          "e0dcd0" },
            { "c_accent",     "Akcent",                      "6495ed" },
            { "c_divider",    "Razdjelnici lista",           "333333" },
            { "c_red_line",   "Crvene linije (liste molitvi)","dc143c" },
            { "c_row",        "Kalendar: neparni dani",      "1f1f1d" },
            { "c_row_border", "Kalendar: okvir današnjeg dana",        "2f5f99" },
            { "c_title",      "Crveni naslovi",              "ff7a66" },
            { "c_web_text",   "Tekst stranica",              "d9d4c7" },
            { "c_web_head2",  "Podnaslovi (h5, h6)",         "e6e1d6" },
            { "c_web_link",   "Linkovi",                     "8ab4f8" },
            { "c_web_visited","Posjećeni linkovi",           "c4a5f0" },
            { "c_web_line",   "Linije i tabele",             "444444" },
    };

    static String def(String key) {
        for (String[] c : ALL) if (c[0].equals(key)) return c[2];
        return "000000";
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
