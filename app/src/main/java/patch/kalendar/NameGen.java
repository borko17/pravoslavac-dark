package patch.kalendar;

import java.util.Random;
import java.util.Set;

/**
 * Nazivi generisanih tema: "НТ: <pridjev> <imenica>". Rod imenice govori kako je tema napravljena:
 * muski = nasumicna svijetla/tamna, zenski = iz boje pozadine, srednji = iz boje pozadine i boje praznika.
 * Bez Android/Xposed zavisnosti.
 */
final class NameGen {
    private NameGen() {}

    static final String PREFIX = "НТ: ";

    static final int MASC = 0; // svijetla/tamna (nasumicna)
    static final int FEM = 1;  // iz boje pozadine
    static final int NEUT = 2; // iz boje pozadine i boje praznika

    /** Osnove pridjeva: + и (muski), + а (zenski), + о ili е (srednji). Bez rijeci sa jatom (ekavica/ijekavica). */
    private static final String[] ADJ = {
            "коврџав", "храбр", "тих", "бучн", "ведр", "плах", "мудр", "питом", "дивљ", "поспан",
            "љут", "раскошн", "скромн", "хитр", "спор", "бесн", "сањив", "сјајн", "мрзовољн", "весел",
            "радознал", "стрпљив", "плашљив", "лукав", "поносн", "зелен", "плав", "црвен", "жут", "љубичаст",
            "наранџаст", "сив", "румен", "златн", "сребрн", "бакарн", "кристалн", "мермерн", "лаган", "мек",
            "чврст", "глатк", "храпав", "мирн", "вртоглав", "слатк", "горк", "кисел", "сочан", "зачињен",
            "свеж", "топл", "хладн", "влажн", "сув", "магловит", "олујн", "сунчан", "облачн", "леден",
            "кишн", "зимск", "јесењ", "оштр", "танк", "висок", "широк", "дуг", "кратк", "стар",
            "млад", "нов", "чаробн", "тајанствен", "заборављен", "изгубљен", "усамљен", "полетн", "блистав", "несташн",
            "замишљен", "задовољн", "срећн", "нестрпљив", "лежеран", "елегантн", "жив", "тамн", "мрачн", "прозрачн",
            "древн", "бајковит", "ужурбан", "замагљен", "тромав", "окретн", "свечан", "дрзовит"
    };

    private static final String[][] NOUNS = {
            // MASC
            { "пас", "вук", "лав", "орао", "соко", "гавран", "коњ", "јеж", "зец", "паун",
                    "делфин", "јелен", "слон", "кит", "тигар", "леопард", "кенгур", "пингвин", "јастреб", "лептир",
                    "крокодил", "камен", "облак", "дуб", "бор", "храст", "кестен", "јавор", "мост", "торањ",
                    "брод", "воз", "балон", "вулкан", "океан", "поток", "талас", "ураган", "чаробњак", "витез",
                    "пират", "морнар", "ковач", "мајстор", "астронаут", "кактус", "вртлог", "фењер", "сат", "кишобран" },
            // FEM
            { "мачка", "лисица", "сова", "рода", "веверица", "жирафа", "патка", "зебра", "корњача", "пчела",
                    "ласта", "риба", "медуза", "звезда", "планета", "река", "шума", "планина", "долина", "пећина",
                    "обала", "лука", "лађа", "кула", "тврђава", "кућа", "соба", "лампа", "свећа", "ружа",
                    "јабука", "крушка", "шљива", "купина", "малина", "јагода", "трешња", "дуња", "лубеница", "бундева",
                    "чаролија", "мелодија", "бура", "магла", "олуја", "дуга", "јесен", "зора", "ноћ", "капља",
                    "пахуља", "пустиња", "оаза", "лопта", "гуска", "коза", "овца", "крава" },
            // NEUT
            { "маче", "пиле", "јагње", "магаре", "теле", "паче", "куче", "лане", "младунче", "дрво",
                    "језеро", "море", "поље", "брдо", "небо", "сунце", "огледало", "перо", "крило", "срце",
                    "зрно", "семе", "јаје", "дугме", "острво", "стакло", "злато", "сребро", "јутро", "вече",
                    "подне", "чудо", "јато", "стадо", "здање", "једро", "весло", "звоно", "воће", "жито",
                    "лишће", "камење", "брашно", "огњиште", "време", "блато", "дрвеће" }
    };

    private static String ending(int gender, String stem) {
        if (gender == MASC) return "и";
        if (gender == FEM) return "а";
        char c = stem.charAt(stem.length() - 1);
        return "љњжшчћџђј".indexOf(c) >= 0 ? "е" : "о";
    }

    /** Slucajan naziv za zadati rod. */
    static String make(int gender, Random r) {
        String stem = ADJ[r.nextInt(ADJ.length)];
        String[] nouns = NOUNS[gender];
        return PREFIX + stem + ending(gender, stem) + " " + nouns[r.nextInt(nouns.length)];
    }

    /** Naziv koji nije u skupu zauzetih (poslije 200 pokusaja dodaje broj). */
    static String unique(int gender, Random r, Set<String> taken) {
        String n = make(gender, r);
        for (int i = 0; i < 200 && taken.contains(n); i++) n = make(gender, r);
        int k = 2;
        String base = n;
        while (taken.contains(n)) n = base + " " + (k++);
        return n;
    }

    /**
     * Rod ako naziv izgleda kao naziv generisane teme (prefiks, pridjev i imenica iz spiska, rod se slaze),
     * inace -1. Preimenovana tema se zato ne prepoznaje.
     */
    static int parse(String name) {
        if (name == null || !name.startsWith(PREFIX)) return -1;
        String rest = name.substring(PREFIX.length());
        int sp = rest.indexOf(' ');
        if (sp <= 0) return -1;
        String adj = rest.substring(0, sp);
        String noun = rest.substring(sp + 1);
        for (int g = 0; g < NOUNS.length; g++) {
            for (String n : NOUNS[g]) {
                if (!n.equals(noun)) continue;
                for (String stem : ADJ) {
                    if (adj.equals(stem + ending(g, stem))) return g;
                }
                return -1;
            }
        }
        return -1;
    }
}
