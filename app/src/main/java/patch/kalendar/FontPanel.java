package patch.kalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.robv.android.xposed.XposedBridge;

/**
 * Prozor za preuzimanje fontova unutar Pravoslavca (Podesavanja -> Фонтови). Izvrsava se u procesu Pravoslavca;
 * mrezu (spisak, pregled, preuzimanje) radi modul preko FontProvider-a (vidi FontClient), jer Pravoslavac nema INTERNET.
 * Izgled prati boje odabrane teme; dugmad su zaobljene (pilule) sa stanjem pritiska.
 */
final class FontPanel {

    static final int PRIMARY = 0;   // boje dugmadi iz teme (c_btn_bg, c_btn_text, c_btn_border)
    static final int TONAL = 1;     // blago istaknuta pozadina
    static final int DANGER = 2;    // brisanje (crvenkasto)
    static final int SUCCESS = 3;   // preuzimanje (zeleno)

    /** Boje prozora (zadaju se izricito iz SettingsUi). */
    static final class Pal {
        final int bg, text, dim, btnBg, btnText, btnBorder;
        final boolean dark;

        Pal(int bg, int text, int dim, int btnBg, int btnText, int btnBorder, boolean dark) {
            this.bg = bg;
            this.text = text;
            this.dim = dim;
            this.btnBg = btnBg;
            this.btnText = btnText;
            this.btnBorder = btnBorder;
            this.dark = dark;
        }
    }

    private static final String SAMPLE = FontCatalog.PREVIEW_TEXT;

    private final Activity act;
    private final Pal pal;
    private final FontClient client;
    private final Runnable onChanged;
    private final Runnable onDeleted;
    private final Handler main = new Handler(Looper.getMainLooper());

    private LinearLayout root;
    private LinearLayout list;
    private boolean started;
    private TextView count;
    private TextView installedInfo;
    private String filter = "";
    private boolean dead;
    private boolean loaded;
    private boolean warmed;
    private int totalCount;

    private String[] catalog = new String[0];
    private final Set<String> installed = new HashSet<String>();
    private final Set<String> bad = new HashSet<String>();
    private final Set<String> busy = new HashSet<String>();
    private final Map<String, Holder> rows = new HashMap<String, Holder>();
    private final Map<String, Typeface> faces = new HashMap<String, Typeface>();
    private final Set<String> previewBusy = new HashSet<String>();
    private final Set<String> previewFailed = new HashSet<String>();
    private final ExecutorService pool = Executors.newFixedThreadPool(3);

    private static final class Holder {
        String fam;
        LinearLayout row;
        TextView name;
        TextView sample;
        TextView btn;
    }

    FontPanel(Activity act, Pal pal, Runnable onChanged, Runnable onDeleted) {
        this.act = act;
        this.pal = pal;
        this.client = new FontClient(act);
        this.onChanged = onChanged;
        this.onDeleted = onDeleted;
        buildView();
    }

    /** View panela (ugraden u tab Фонтови; dodaje ga SettingsUi). */
    View view() {
        return root;
    }

    /** Ucitava spisak (jednom, kad se panel prvi put prikaze). */
    void start() {
        if (!started) {
            started = true;
            loadState();
        }
    }

    /** Poziva se pri skrolovanju stranice: ucitava preglede za redove na ekranu. */
    void onScroll() {
        loadVisiblePreviews();
    }

    /** Zatvaranje podesavanja: zaustavlja pozadinske niti. */
    void dispose() {
        dead = true;
        pool.shutdownNow();
    }

    boolean sameColors(Pal o) {
        return o.bg == pal.bg && o.text == pal.text && o.btnBg == pal.btnBg && o.dark == pal.dark;
    }

    // ------------------------------------------------------------------ zajednicko: dugmad i pozadine

    static int dp(Activity a, int v) {
        return Math.round(v * a.getResources().getDisplayMetrics().density);
    }

    private int dp(int v) {
        return dp(act, v);
    }

    private static Drawable stateBg(int fill, int press, int stroke, float radius, int strokePx) {
        GradientDrawable n = new GradientDrawable();
        n.setColor(fill);
        n.setCornerRadius(radius);
        GradientDrawable p = new GradientDrawable();
        p.setColor(press);
        p.setCornerRadius(radius);
        if (stroke != 0) {
            n.setStroke(strokePx, stroke);
            p.setStroke(strokePx, stroke);
        }
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[] { android.R.attr.state_pressed }, p);
        s.addState(new int[0], n);
        return s;
    }

    /** Zaobljeno dugme (pilula): kind = PRIMARY / TONAL / DANGER, icon = Icons.* ili -1, small = manje dugme za redove. */
    static TextView pill(Activity act, Pal p, String label, int kind, int icon, boolean small) {
        TextView b = new TextView(act);
        b.setTag(Dark.TAG_SETTINGS, Boolean.TRUE);   // prije setText (vidi SettingsUi.label)
        b.setText(label);
        b.setTextSize(small ? 13f : 14f);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        b.setSingleLine(true);
        b.setEllipsize(TextUtils.TruncateAt.END);
        b.setClickable(true);
        b.setMinHeight(dp(act, small ? 36 : 42));
        b.setMinWidth(dp(act, small ? 92 : 0));
        int px = dp(act, small ? 14 : 16);
        b.setPadding(px, 0, px, 0);
        b.setCompoundDrawablePadding(dp(act, 6));
        style(act, p, b, kind, icon);
        return b;
    }

    /** (Ponovo) postavlja boje i ikonicu dugmeta za datu vrstu. */
    static void style(Activity act, Pal p, TextView b, int kind, int icon) {
        int fill;
        int press;
        int fg;
        int stroke;
        if (kind == PRIMARY) {
            fill = 0xff000000 | p.btnBg;
            fg = 0xff000000 | p.btnText;
            press = Dark.mix(fill, fg, 0.22f);
            stroke = 0xff000000 | p.btnBorder;
        } else if (kind == SUCCESS) {   // isti stil kao DANGER: slova u boji, blijeda pozadina iste nijanse
            int green = p.dark ? 0xff81c784 : 0xff2e7d32;
            fill = Dark.mix(p.bg, green, 0.16f);
            press = Dark.mix(p.bg, green, 0.32f);
            fg = green;
            stroke = Dark.mix(p.bg, green, 0.45f);
        } else if (kind == DANGER) {
            int red = p.dark ? 0xffff8a80 : 0xffc62828;
            fill = Dark.mix(p.bg, red, 0.16f);
            press = Dark.mix(p.bg, red, 0.32f);
            fg = red;
            stroke = Dark.mix(p.bg, red, 0.45f);
        } else {
            fill = Dark.mix(p.bg, p.text, 0.13f);
            press = Dark.mix(p.bg, p.text, 0.26f);
            fg = 0xff000000 | p.text;
            stroke = Dark.mix(p.bg, p.text, 0.22f);
        }
        b.setBackground(stateBg(fill, press, stroke, dp(act, 30), dp(act, 1)));
        b.setTextColor(fg);
        if (icon >= 0) {
            b.setCompoundDrawablesWithIntrinsicBounds(Icons.get(icon, fg, dp(act, 18)), null, null, null);
        } else {
            b.setCompoundDrawables(null, null, null, null);
        }
    }

    private TextView text(String s, float size, int color) {
        TextView tv = new TextView(act);
        tv.setTag(Dark.TAG_SETTINGS, Boolean.TRUE);   // prije setText
        tv.setText(s);
        tv.setTextSize(size);
        tv.setTextColor(color);
        return tv;
    }

    private GradientDrawable windowBg(int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xff000000 | pal.bg);
        g.setCornerRadius(radius);
        g.setStroke(dp(2), Dark.mix(pal.bg, pal.text, 0.38f));
        return g;
    }

    private LinearLayout.LayoutParams lp(int w, int h, float weight, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h, weight);
        p.setMargins(l, t, r, b);
        return p;
    }

    private int dialogTheme() {
        return pal.dark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT;
    }

    // ------------------------------------------------------------------ panel

    private void buildView() {
        root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setTag(Dark.TAG_SETTINGS, Boolean.TRUE);

        TextView hint = text("Само ћирилични фонтови са свим српским словима. Интернет користи модул "
                + "Pravoslavac Dark (Православац га нема), само кад притиснете „Преузми“.", 12f, pal.dim);
        hint.setPadding(0, dp(4), 0, 0);
        root.addView(hint);

        // dugmad iznad liste
        LinearLayout acts = new LinearLayout(act);
        acts.setOrientation(LinearLayout.HORIZONTAL);
        acts.setPadding(0, dp(12), 0, dp(10));
        TextView refresh = pill(act, pal, "Освјежи списак", TONAL, Icons.REGENERATE, false);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshCatalog();
            }
        });
        TextView delAll = pill(act, pal, "Обриши све", DANGER, Icons.DELETE, false);
        delAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAll();
            }
        });
        acts.addView(refresh, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 0, 0, dp(4), 0));
        acts.addView(delAll, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, dp(4), 0, 0, 0));
        root.addView(acts);

        // претрага
        EditText search = new EditText(act);
        search.setTag(Dark.TAG_SETTINGS, Boolean.TRUE);
        search.setHint("Претрага");
        search.setSingleLine(true);
        search.setImeOptions(EditorInfo.IME_ACTION_DONE);
        search.setTextSize(15f);
        search.setTextColor(pal.text);
        search.setHintTextColor(pal.dim);
        search.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable sbg = new GradientDrawable();
        sbg.setColor(Dark.mix(pal.bg, pal.text, 0.09f));
        sbg.setCornerRadius(dp(12));
        sbg.setStroke(dp(1), Dark.mix(pal.bg, pal.text, 0.22f));
        search.setBackground(sbg);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filter = s.toString().trim().toLowerCase(Locale.ROOT);
                if (loaded) build();
            }
        });
        root.addView(search, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        count = text("Учитавам списак…", 13f, pal.dim);
        count.setPadding(dp(2), dp(8), 0, 0);
        root.addView(count);
        installedInfo = text("", 13f, pal.dark ? 0xff81c784 : 0xff2e7d32);
        installedInfo.setPadding(dp(2), dp(2), 0, dp(8));
        root.addView(installedInfo);

        list = new LinearLayout(act);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void ui(final Runnable r) {
        main.post(new Runnable() {
            @Override
            public void run() {
                if (!dead) r.run();
            }
        });
    }

    private void toast(String s) {
        Toast.makeText(act, s, Toast.LENGTH_LONG).show();
    }

    private static String msg(Throwable e) {
        return e.getMessage() != null ? e.getMessage() : e.toString();
    }

    /** Font je preuzet/obrisan: aplikacija ponovo ucitava spisak fontova iz postavki modula. */
    private void markChanged() {
        try {
            Fonts.reset();
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
        if (onChanged != null) onChanged.run();   // osvjezi nazive fontova u tabu
    }

    // ------------------------------------------------------------------ stanje i lista

    private void loadState() {
        count.setTextColor(pal.dim);
        count.setText("Учитавам списак…");
        pool.execute(new Runnable() {
            @Override
            public void run() {
                FontClient.State st = null;
                String err = null;
                try {
                    st = client.state();
                    try {
                        client.sync(st);   // fontovi se cuvaju u Pravoslavcu: prenesi nove / ukloni obrisane
                    } catch (Throwable ignored) {
                        // nije kriticno: ponovo se pokusava pri sljedecem otvaranju
                    }
                } catch (Throwable e) {
                    err = msg(e);
                }
                final FontClient.State fs = st;
                final String fe = err;
                ui(new Runnable() {
                    @Override
                    public void run() {
                        if (fs == null) {
                            if (!warmed && fe != null && fe.contains("видљивост")) {
                                warmed = true;
                                warmUp();   // otkrij modul Pravoslavcu pa pokusaj ponovo
                                return;
                            }
                            showError(fe);
                            return;
                        }
                        catalog = fs.catalog;
                        installed.clear();
                        installed.addAll(fs.installed);
                        bad.clear();
                        bad.addAll(fs.bad);
                        loaded = true;
                        build();
                    }
                });
            }
        });
    }

    /** Pokrece nevidljivu aktivnost modula (otkriva paket modula Pravoslavcu), pa ponovo ucitava stanje. */
    private void warmUp() {
        try {
            android.content.Intent i = new android.content.Intent();
            i.setClassName(FontCatalog.MODULE_APP_ID, "patch.kalendar.WarmupActivity");
            act.startActivity(i);
            act.overridePendingTransition(0, 0);
        } catch (Throwable t) {
            showError("не могу да покренем модул: " + msg(t));
            return;
        }
        main.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!dead) loadState();
            }
        }, 800);
    }

    private void showError(String err) {
        loaded = false;
        rows.clear();
        list.removeAllViews();
        count.setText("");
        TextView t = text("Модул Pravoslavac Dark није доступан (" + err + ").\n\n"
                + "У LSPosed-у за модул укључите и „System Framework“ (android) и рестартујте телефон, "
                + "па покушајте поново.", 14f, pal.dim);
        t.setPadding(dp(2), dp(4), dp(2), dp(12));
        list.addView(t);
        LinearLayout bx = new LinearLayout(act);
        bx.setOrientation(LinearLayout.HORIZONTAL);
        TextView retry = pill(act, pal, "Покушај поново", TONAL, -1, false);
        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadState();
            }
        });
        bx.addView(retry, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 0, 0, 0, 0));
        list.addView(bx);
    }

    /** Spisak sa kataloga plus preuzeti fontovi kojih nema u katalogu (da se mogu obrisati). */
    private List<String> names() {
        List<String> out = new ArrayList<String>();
        Set<String> seen = new HashSet<String>();
        for (String f : catalog) {
            if (seen.add(f)) out.add(f);
        }
        List<String> extra = new ArrayList<String>(installed);
        java.util.Collections.sort(extra, String.CASE_INSENSITIVE_ORDER);
        for (String f : extra) {
            if (seen.add(f)) out.add(f);
        }
        return out;
    }

    private void build() {
        rows.clear();
        list.removeAllViews();
        totalCount = 0;
        int shown = 0;
        for (String fam : names()) {
            boolean has = installed.contains(fam);
            if (!has && bad.contains(fam)) continue;   // pregled je pokazao da nema sva srpska slova
            totalCount++;
            if (filter.length() > 0 && !fam.toLowerCase(Locale.ROOT).contains(filter)) continue;
            Holder h = makeRow(fam);
            rows.put(fam, h);
            list.addView(h.row);
            bindRow(h);
            shown++;
        }
        if (shown == 0) {
            TextView none = text(filter.length() > 0 ? "Нема резултата" : "Списак је празан", 14f, pal.dim);
            none.setPadding(dp(2), dp(8), 0, 0);
            list.addView(none);
        }
        updateCount();
        list.post(new Runnable() {
            @Override
            public void run() {
                if (dead) return;
                loadVisiblePreviews();
                checkAllPreviews();
            }
        });
    }

    private Holder makeRow(final String fam) {
        final Holder h = new Holder();
        h.fam = fam;
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(10), dp(10), dp(10));
        GradientDrawable rbg = new GradientDrawable();
        rbg.setColor(Dark.mix(pal.bg, pal.text, 0.07f));
        rbg.setCornerRadius(dp(14));
        rbg.setStroke(dp(1), Dark.mix(pal.bg, pal.text, 0.14f));
        row.setBackground(rbg);
        row.setTag(fam);
        h.row = row;

        LinearLayout left = new LinearLayout(act);
        left.setOrientation(LinearLayout.VERTICAL);
        h.name = text(fam, 13f, pal.dim);
        left.addView(h.name);
        h.sample = text(SAMPLE, 16f, pal.text);
        h.sample.setPadding(0, dp(2), 0, 0);
        left.addView(h.sample);
        row.addView(left, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 0, 0, dp(10), 0));

        h.btn = pill(act, pal, "Преузми", SUCCESS, -1, true);
        h.btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (busy.contains(fam)) return;
                if (installed.contains(fam)) delete(fam);
                else download(fam);
            }
        });
        row.addView(h.btn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setLayoutParams(lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 0, 0, 0, dp(8)));
        return h;
    }

    private void bindRow(Holder h) {
        boolean has = installed.contains(h.fam);
        boolean working = busy.contains(h.fam);
        h.name.setText(has ? h.fam + "  ✓ преузет" : h.fam);
        h.name.setTextColor(has ? pal.text : pal.dim);
        Typeface tf = faces.get(h.fam);
        if (tf != null) h.sample.setTypeface(tf);
        if (working) {
            h.btn.setText("…");
            style(act, pal, h.btn, TONAL, -1);
            h.btn.setAlpha(0.6f);
        } else if (has) {
            h.btn.setText("Обриши");
            style(act, pal, h.btn, DANGER, -1);
            h.btn.setAlpha(1f);
        } else {
            h.btn.setText("Преузми");
            style(act, pal, h.btn, SUCCESS, -1);
            h.btn.setAlpha(1f);
        }
        h.btn.setEnabled(!working);
    }

    private void bindRow(String fam) {
        Holder h = rows.get(fam);
        if (h != null) bindRow(h);
    }

    /** Broj fontova koji jos nisu provjereni (nisu preuzeti, nisu izbaceni, pregled jos nije gotov). */
    private int pendingChecks() {
        int n = 0;
        for (String fam : catalog) {
            if (installed.contains(fam) || bad.contains(fam) || faces.containsKey(fam) || previewFailed.contains(fam)) continue;
            n++;
        }
        return n;
    }

    private void updateCount() {
        if (!loaded) return;
        if (pendingChecks() > 0) {
            count.setTextColor(pal.dark ? 0xffff8a80 : 0xffc62828);
            count.setText("Провјера фонтова који немају српска слова ћ, ђ, љ, њ, џ…");
        } else {
            count.setTextColor(pal.dark ? 0xff81c784 : 0xff2e7d32);
            count.setText(totalCount + " " + fontWord(totalCount) + " са српском ћирилицом.");
        }
        if (installedInfo != null) {
            int n = installed.size();
            installedInfo.setText("Преузето: " + n + " " + fontWord(n));
        }
    }

    /** фонт / фонта / фонтова (уз број). */
    private static String fontWord(int n) {
        int m10 = n % 10;
        int m100 = n % 100;
        if (m10 == 1 && m100 != 11) return "фонт";
        if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) return "фонта";
        return "фонтова";
    }

    // ------------------------------------------------------------------ pregled (isjecci fontova)

    /** Provjerava u pozadini sve fontove sa spiska, da se nekompatibilni izbace sto prije. */
    private void checkAllPreviews() {
        for (String fam : catalog) {
            if (!installed.contains(fam)) startPreview(fam);
        }
        updateCount();
    }

    /** Pokrece ucitavanje pregleda za redove na ekranu (i za nekoliko redova ispod njih). */
    private void loadVisiblePreviews() {
        if (dead || list == null) return;
        Rect r = new Rect();
        int n = list.getChildCount();
        List<String> todo = new ArrayList<String>();
        for (int i = 0; i < n; i++) {
            View row = list.getChildAt(i);
            if (!row.getGlobalVisibleRect(r)) continue;
            for (int k = i; k < Math.min(n, i + 4); k++) {
                Object t = list.getChildAt(k).getTag();
                if (t instanceof String && !todo.contains(t)) todo.add((String) t);
            }
        }
        for (String fam : todo) startPreview(fam);
    }

    private void startPreview(final String fam) {
        if (dead || faces.containsKey(fam) || previewBusy.contains(fam) || previewFailed.contains(fam)) return;
        previewBusy.add(fam);
        try {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    FontClient.Preview p = null;
                    try {
                        p = client.preview(fam);
                    } catch (Throwable e) {
                        p = null;
                    }
                    final FontClient.Preview fp = p;
                    ui(new Runnable() {
                        @Override
                        public void run() {
                            previewBusy.remove(fam);
                            if (fp == null) {
                                previewFailed.add(fam);
                                updateCount();
                            } else {
                                applyPreview(fam, fp);
                            }
                        }
                    });
                }
            });
        } catch (Throwable e) {
            previewBusy.remove(fam);   // pool je ugasen (prozor se zatvara)
        }
    }

    private void applyPreview(String fam, FontClient.Preview p) {
        if (!p.complete) {
            if (!installed.contains(fam)) hideFamily(fam);
            return;
        }
        Typeface tf;
        Fonts.creating = true;   // da se ne smatra fontom aplikacije
        try {
            tf = Typeface.createFromFile(p.file);
        } catch (Throwable e) {
            previewFailed.add(fam);
            updateCount();
            return;
        } finally {
            Fonts.creating = false;
        }
        faces.put(fam, tf);
        Holder h = rows.get(fam);
        if (h != null) h.sample.setTypeface(tf);
        updateCount();
    }

    /** Font cijem pregledu fale srpska slova nestaje sa spiska (modul to pamti, pa se ne nudi ni poslije). */
    private void hideFamily(String fam) {
        bad.add(fam);
        faces.remove(fam);
        Holder h = rows.remove(fam);
        if (h != null) {
            list.removeView(h.row);
            totalCount--;
        }
        updateCount();
        list.post(new Runnable() {
            @Override
            public void run() {
                loadVisiblePreviews();
            }
        });
    }

    // ------------------------------------------------------------------ radnje

    private void download(final String fam) {
        busy.add(fam);
        bindRow(fam);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String err = null;
                try {
                    client.download(fam);
                } catch (Throwable e) {
                    err = msg(e);
                }
                final String fe = err;
                ui(new Runnable() {
                    @Override
                    public void run() {
                        busy.remove(fam);
                        if (fe == null) {
                            installed.add(fam);
                            markChanged();
                        }
                        bindRow(fam);
                        updateCount();
                        toast(fe == null ? fam + ": преузето" : fam + ": " + fe);
                    }
                });
            }
        }).start();
    }

    private void delete(final String fam) {
        busy.add(fam);
        bindRow(fam);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String err = null;
                try {
                    client.delete(fam);
                } catch (Throwable e) {
                    err = msg(e);
                }
                final String fe = err;
                ui(new Runnable() {
                    @Override
                    public void run() {
                        busy.remove(fam);
                        if (fe == null) {
                            installed.remove(fam);
                            if (onDeleted != null) onDeleted.run();   // tekstovi sa ovim fontom se vracaju na original
                            markChanged();
                            updateCount();
                            if (!inCatalog(fam)) {
                                build();   // font nije u katalogu: red nestaje
                                return;
                            }
                        }
                        bindRow(fam);
                        if (fe != null) toast(fam + ": " + fe);
                    }
                });
            }
        }).start();
    }

    private boolean inCatalog(String fam) {
        for (String f : catalog) {
            if (f.equals(fam)) return true;
        }
        return false;
    }

    private void deleteAll() {
        if (!loaded) return;
        if (installed.isEmpty()) {
            toast("Нема преузетих фонтова");
            return;
        }
        confirm("Обрисати све преузете фонтове?", "Текстови који их користе вратиће се на оригинални фонт.",
                "Обриши", new Runnable() {
                    @Override
                    public void run() {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String err = null;
                                try {
                                    client.deleteAll();
                                } catch (Throwable e) {
                                    err = msg(e);
                                }
                                final String fe = err;
                                ui(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (fe != null) {
                                            toast("Грешка: " + fe);
                                            return;
                                        }
                                        installed.clear();
                                        if (onDeleted != null) onDeleted.run();   // svi tekstovi se vracaju na original
                                        markChanged();
                                        build();
                                    }
                                });
                            }
                        }).start();
                    }
                });
    }

    private void refreshCatalog() {
        if (!loaded) {
            loadState();
            return;
        }
        count.setText("Освјежавам списак са Гугла…");
        new Thread(new Runnable() {
            @Override
            public void run() {
                String err = null;
                FontClient.State st = null;
                int n = 0;
                try {
                    n = client.refresh();
                    st = client.state();
                    try {
                        client.sync(st);   // fontovi se cuvaju u Pravoslavcu: prenesi nove / ukloni obrisane
                    } catch (Throwable ignored) {
                        // nije kriticno: ponovo se pokusava pri sljedecem otvaranju
                    }
                } catch (Throwable e) {
                    err = msg(e);
                }
                final String fe = err;
                final FontClient.State fs = st;
                final int fn = n;
                ui(new Runnable() {
                    @Override
                    public void run() {
                        if (fe == null && fs != null) {
                            catalog = fs.catalog;
                            installed.clear();
                            installed.addAll(fs.installed);
                            bad.clear();
                            bad.addAll(fs.bad);
                            previewFailed.clear();
                            build();
                            toast("Списак освјежен: " + totalCount + " " + fontWord(totalCount) + " са ћирилицом");
                        } else {
                            updateCount();
                            toast("Грешка: " + fe);
                        }
                    }
                });
            }
        }).start();
    }

    /** Potvrda (zaobljeni prozor sa dva dugmeta). */
    private void confirm(String title, String message, String okLabel, final Runnable action) {
        final Dialog d = new Dialog(act, dialogTheme());
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(20), dp(22), dp(16));
        box.setTag(Dark.TAG_SETTINGS, Boolean.TRUE);
        TextView t = text(title, 18f, pal.text);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(t);
        TextView m = text(message, 14f, pal.dim);
        m.setPadding(0, dp(8), 0, dp(18));
        box.addView(m);
        LinearLayout bx = new LinearLayout(act);
        bx.setOrientation(LinearLayout.HORIZONTAL);
        bx.setGravity(Gravity.END);
        TextView no = pill(act, pal, "Одустани", TONAL, -1, false);
        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        TextView yes = pill(act, pal, okLabel, DANGER, Icons.DELETE, false);
        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
                action.run();
            }
        });
        bx.addView(no, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 0, 0, dp(8), 0));
        bx.addView(yes);
        box.addView(bx);
        d.setContentView(box);
        Window w = d.getWindow();
        w.getDecorView().setTag(Dark.TAG_SETTINGS, Boolean.TRUE);
        w.setBackgroundDrawable(windowBg(dp(16)));
        w.setLayout(act.getResources().getDisplayMetrics().widthPixels - dp(48), WindowManager.LayoutParams.WRAP_CONTENT);
        w.setGravity(Gravity.CENTER);
        d.show();
    }
}
