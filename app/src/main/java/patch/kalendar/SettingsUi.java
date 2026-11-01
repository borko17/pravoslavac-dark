package patch.kalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Podesavanja modula unutar aplikacije Pravoslavac (Meni -> Опције -> Подешавања).
 * Izvrsava se u procesu Pravoslavca; postavke se cuvaju u njegovim podacima (Prefs.FILE).
 * Pozadina ekrana je Dark.BG, a tekst Dark.TEXT (c_bg i c_text odabrane teme).
 */
final class SettingsUi {

    private final Activity act;
    private final SharedPreferences sp;
    private static SettingsUi current;
    private Host host;
    private ViewGroup parent;
    private View drawerChild;
    private Object backCb;
    private boolean closed;
    /** Odabrani tab: 0 = opcije (prekidaci), 1 = teme. */
    private int tab;
    private int builtTab = -1;
    private ScrollView scroll;
    /** Kompaktni podmeni (preko ekrana podesavanja) ili null. */
    private View menuOverlay;

    private List<Themes.Theme> themes;
    private String selId;

    /** Neka postavka koja trazi ponovno kreiranje ekrana je promijenjena. */
    private boolean dirty;
    /** Tema je iskljucena: ne mogu se vratiti originalni resursi bez restarta aplikacije. */
    private boolean darkOff;

    private SettingsUi(Activity act) {
        this.act = act;
        this.sp = act.getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE);
    }

    static void show(Activity act) {
        try {
            SettingsUi c = current;
            if (c != null && !c.closed && c.act == act && c.host != null && c.host.getParent() != null) {
                return; // vec otvoreno
            }
            if (c != null) c.close(false);
            SettingsUi s = new SettingsUi(act);
            current = s;
            s.open();
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    /** Zatvara podesavanja ako su otvorena (npr. kad se u meniju izabere druga stavka). */
    static void dismiss(Activity act) {
        SettingsUi s = current;
        if (s != null && !s.closed && s.act == act) s.close(true);
    }

    /** Back: ako je bocni meni otvoren pusta aplikaciji da ga zatvori, inace zatvara podesavanja. */
    static boolean onBack(Activity act) {
        SettingsUi s = current;
        if (s == null || s.closed || s.act != act) return false;
        if (s.drawerOpen()) return false;
        if (s.closeMenu()) return true;
        s.close(true);
        return true;
    }

    private void open() {
        loadThemes();
        host = new Host(act);
        host.setClickable(true);
        host.setFocusableInTouchMode(true);
        build();
        attach();
    }

    private void close(boolean apply) {
        if (closed) return;
        closed = true;
        if (current == this) current = null;
        unregisterBack();
        try {
            if (host != null && host.getParent() instanceof ViewGroup) {
                ((ViewGroup) host.getParent()).removeView(host);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
        if (apply && dirty && !darkOff) {
            // Primijeni promjene na otvoreni ekran aplikacije.
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        act.recreate();
                    } catch (Throwable t) {
                        XposedBridge.log(t);
                    }
                }
            });
        }
    }

    /** Prozor podesavanja: hvata Back dok je fokus unutar njega. */
    private final class Host extends FrameLayout {
        Host(Context c) {
            super(c);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.KEYCODE_BACK && !closed && !drawerOpen()) {
                if (e.getAction() == KeyEvent.ACTION_UP && !closeMenu()) close(true);
                return true;
            }
            return super.dispatchKeyEvent(e);
        }
    }

    // ------------------------------------------- unutar prozora aplikacije

    /**
     * Ubacuje podesavanja u hijerarhiju aplikacije, ispod trake i ISPOD bocnog menija
     * (u DrawerLayout, ispred ladice), pa meni iz ikone s tri crtice iskace preko njih.
     */
    private void attach() {
        View decor = act.getWindow().getDecorView();
        ViewGroup p = null;
        int index = -1;
        View dl = findByName(decor, "DrawerLayout");
        if (dl instanceof ViewGroup) {
            p = (ViewGroup) dl;
            View lv = null;
            try {
                lv = (View) XposedHelpers.getObjectField(act, "H0");
            } catch (Throwable t) {
                // probaj trazenje po klasi
            }
            if (lv == null) lv = findInstance(decor, ExpandableListView.class);
            View c = lv;
            while (c != null && c.getParent() != p) {
                c = (c.getParent() instanceof View) ? (View) c.getParent() : null;
            }
            if (c != null) {
                drawerChild = c;
                index = p.indexOfChild(c);
            }
        }
        if (p == null) p = (ViewGroup) act.findViewById(android.R.id.content);
        if (p == null) throw new IllegalStateException("nema kontejnera");
        parent = p;

        int top = 0;
        int bar = barBottom();
        if (bar > 0) {
            int[] l = new int[2];
            p.getLocationOnScreen(l);
            top = Math.max(0, bar - l[1]);
        }
        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.topMargin = top;
        if (index >= 0) p.addView(host, index, lp);
        else p.addView(host, lp);
        host.requestFocus();
        registerBack();
    }

    /** Bocni meni je (djelimicno) vidljiv. */
    private boolean drawerOpen() {
        View d = drawerChild;
        if (d == null || parent == null) return false;
        return d.getVisibility() == View.VISIBLE && d.getRight() > 0 && d.getLeft() < parent.getWidth();
    }

    private void registerBack() {
        if (Build.VERSION.SDK_INT < 33) return;
        try {
            backCb = Back33.register(act, this);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private void unregisterBack() {
        if (backCb == null) return;
        try {
            Back33.unregister(act, backCb);
        } catch (Throwable t) {
            // nije kriticno
        }
        backCb = null;
    }

    /** Back gesta (predictive back) na Androidu 13+. */
    private static final class Back33 {
        static Object register(final Activity a, final SettingsUi s) {
            OnBackInvokedCallback cb = new OnBackInvokedCallback() {
                @Override
                public void onBackInvoked() {
                    if (s.drawerOpen()) Hook.closeDrawer(a);
                    else if (!s.closeMenu()) s.close(true);
                }
            };
            a.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_OVERLAY, cb);
            return cb;
        }

        static void unregister(Activity a, Object cb) {
            a.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback((OnBackInvokedCallback) cb);
        }
    }

    private static View findByName(View v, String simpleName) {
        for (Class<?> c = v.getClass(); c != null; c = c.getSuperclass()) {
            if (c.getSimpleName().equals(simpleName)) return v;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View r = findByName(g.getChildAt(i), simpleName);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static View findInstance(View v, Class<?> cls) {
        if (cls.isInstance(v)) return v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View r = findInstance(g.getChildAt(i), cls);
                if (r != null) return r;
            }
        }
        return null;
    }

    /** Donja ivica trake aplikacije na ekranu (px) ili -1 ako trake nema. */
    private int barBottom() {
        try {
            View root = act.getWindow().getDecorView();
            String[] names = {"action_bar_container", "action_bar", "toolbar", "tool_bar"};
            for (String n : names) {
                int id = act.getResources().getIdentifier(n, "id", act.getPackageName());
                if (id == 0) continue;
                View v = root.findViewById(id);
                int b = bottomOf(v);
                if (b > 0) return b;
            }
            return bottomOf(findBar(root));
        } catch (Throwable t) {
            return -1;
        }
    }

    private static int bottomOf(View v) {
        if (v == null || v.getVisibility() != View.VISIBLE || v.getHeight() <= 0) return -1;
        int[] loc = new int[2];
        v.getLocationOnScreen(loc);
        return loc[1] + v.getHeight();
    }

    private static View findBar(View v) {
        for (Class<?> c = v.getClass(); c != null; c = c.getSuperclass()) {
            String n = c.getSimpleName();
            if (n.equals("Toolbar") || n.equals("ActionBarContainer")) return v;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View r = findBar(g.getChildAt(i));
                if (r != null) return r;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ izgled

    /** Teme uključene (boje odabrane teme) ili isključene (originalne boje aplikacije). */
    private boolean themed() {
        return sp.getBoolean(Prefs.KEY_DARK, true);
    }

    private int bg() {
        return themed() ? Dark.BG : 0xfffaf5e9;
    }

    private int bar() {
        return themed() ? Dark.BAR : 0xff2f3e5c;
    }

    private int tabText() {
        return themed() ? Dark.TAB_TEXT : 0xffffffff;
    }

    private int text() {
        return themed() ? Dark.TEXT : 0xff000000;
    }

    /** Sekundarni tekst: boja teksta pomijesana s pozadinom. */
    private int dim() {
        int t = text();
        int b = bg();
        int r = (((t >> 16) & 255) * 65 + ((b >> 16) & 255) * 35) / 100;
        int g = (((t >> 8) & 255) * 65 + ((b >> 8) & 255) * 35) / 100;
        int bl = ((t & 255) * 65 + (b & 255) * 35) / 100;
        return 0xff000000 | (r << 16) | (g << 8) | bl;
    }

    private AlertDialog.Builder builder() {
        return new AlertDialog.Builder(act, themed() && !Dark.light()
                ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    }

    private TextView label(String s, float size, int color) {
        TextView tv = new TextView(act);
        tv.setText(s);
        tv.setTextSize(size);
        tv.setTextColor(color);
        return tv;
    }

    /** (Ponovo) gradi cijeli ekran; cuva poziciju skrolovanja. */
    private void build() {
        final int scrollY = (scroll != null && builtTab == tab) ? scroll.getScrollY() : 0;
        builtTab = tab;
        menuOverlay = null;

        int pad = dp(16);
        LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(bg());

        if (tab == 0) {
            root.addView(makeSwitch("Прескочи уводни екран", "Апликација се отвара без уводног екрана.",
                    Prefs.KEY_SKIP_SPLASH, true));
            root.addView(makeSwitch("Текст поравнат лево", "Уместо обостраног поравнања.",
                    Prefs.KEY_LEFT, true));
            root.addView(makeSwitch("Апликација отвара календар", "Отвара се календар уместо житија за тај дан.",
                    Prefs.KEY_OPEN_CALENDAR, true));
            root.addView(makeSwitch("Теме",
                    "Укључено: Тема у одабраним бојама. Искључено: оригинална боја апликације.",
                    Prefs.KEY_DARK, true));
        } else {
            addThemeSection(root);
        }

        scroll = new ScrollView(act);
        scroll.setBackgroundColor(bg());
        scroll.setFillViewport(true);
        scroll.addView(root);

        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(makeTabs(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        col.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        host.setBackgroundColor(bg());
        host.removeAllViews();
        host.addView(col, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scroll.post(new Runnable() {
            @Override
            public void run() {
                scroll.scrollTo(0, scrollY);
            }
        });
    }

    /** Tabovi izgledaju kao u aplikaciji (kalendar / raspored citanja): traka boje teme, indikator 3dp. */
    private View makeTabs() {
        LinearLayout bar = new LinearLayout(act);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(bar());
        String[] names = { "Опције", "Теме" };
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            FrameLayout cell = new FrameLayout(act);
            TextView tv = new TextView(act);
            tv.setText(names[i]);
            tv.setAllCaps(true);
            tv.setTextSize(14f);
            tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            tv.setTextColor(tabText());
            tv.setGravity(Gravity.CENTER);
            cell.addView(tv, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            if (i == tab) {
                View ind = new View(act);
                ind.setBackgroundColor(themed() ? Dark.TAB_LINE : 0xff33b5e5);
                cell.addView(ind, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(3), Gravity.BOTTOM));
            }
            cell.setClickable(true);
            cell.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (idx != tab) {
                        tab = idx;
                        build();
                    }
                }
            });
            bar.addView(cell, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        }
        return bar;
    }

    private View makeSwitch(String title, String desc, final String key, boolean def) {
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(8), 0, dp(8));

        Switch sw = new Switch(act);
        sw.setText(title);
        sw.setTextSize(18f);
        sw.setTextColor(text());
        sw.setChecked(sp.getBoolean(key, def));
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                sp.edit().putBoolean(key, checked).commit();
                Prefs.refresh();
                if (Prefs.KEY_DARK.equals(key)) {
                    dirty = true;
                    if (checked) {
                        darkOff = false;
                        Dark.reload();
                        Hook.reapplyResources();
                    } else {
                        // vrati originalne boje resursa; ekran se ponovo pravi kad se podesavanja zatvore
                        boolean ok = Hook.restoreResources(act);
                        darkOff = !ok;
                        toast(ok ? "Тема је искључена. Затвори подешавања да се примени."
                                : "Поново покрени апликацију.");
                    }
                    host.post(new Runnable() {
                        @Override
                        public void run() {
                            if (!closed) build();
                        }
                    });
                } else if (Prefs.KEY_LEFT.equals(key)) {
                    dirty = true;
                }
            }
        });
        box.addView(sw);
        box.addView(label(desc, 14f, dim()));
        return box;
    }

    // ------------------------------------------------------------------ teme

    private void loadThemes() {
        String json = sp.getString(Themes.KEY_THEMES, null);
        themes = Themes.parse(json);
        selId = Themes.find(themes, sp.getString(Themes.KEY_SEL, null)).id;
        if (json == null) saveThemes(); // prvo otvaranje: upisi ugradjene teme
    }

    /** Upisuje teme i odmah primjenjuje boje (Dark + resursi); ekran se osvjezava pri zatvaranju. */
    private void saveThemes() {
        sp.edit()
                .putString(Themes.KEY_THEMES, Themes.toJson(themes))
                .putString(Themes.KEY_SEL, selId)
                .commit();
        Prefs.refresh();
        Dark.reload();
        Hook.reapplyResources();
        dirty = true;
    }

    private void addThemeSection(LinearLayout root) {
        TextView h = label("Одабери тему (⋮ = уреди боје, преименуј, обриши)", 14f, dim());
        h.setPadding(0, dp(8), 0, dp(4));
        root.addView(h);

        for (final Themes.Theme t : themes) {
            LinearLayout row = new LinearLayout(act);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            RadioButton rb = new RadioButton(act);
            rb.setText(t.name);
            rb.setTextSize(16f);
            rb.setTextColor(text());
            // kruzic mora pratiti boje teme uzivo (inace ostaje u bojama teme prozora iz trenutka otvaranja)
            rb.setButtonTintList(new android.content.res.ColorStateList(
                    new int[][] { { android.R.attr.state_checked }, {} }, new int[] { text(), dim() }));
            rb.setChecked(t.id.equals(selId));
            rb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selId = t.id;
                    saveThemes();
                    build();
                }
            });
            row.addView(rb, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            // mali pregled teme: pozadina, traka, naslov, link
            String[] keys = { "c_bg", "c_bar", "c_title", "c_web_link" };
            for (String k : keys) {
                View dot = new View(act);
                setSwatch(dot, Colors.parse(t.color(k)).intValue(), 1);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(16), dp(16));
                lp.setMargins(dp(2), 0, dp(2), 0);
                row.addView(dot, lp);
            }

            TextView more = label("\u22EE", 24f, text());
            more.setPadding(dp(14), dp(8), dp(10), dp(8));
            more.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showThemeMenu(t);
                }
            });
            row.addView(more);
            root.addView(row);
        }

        LinearLayout bar = new LinearLayout(act);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.addView(iconButton(Icons.PLUS, "Нова тема", new Runnable() {
            @Override
            public void run() {
                addTheme();
            }
        }), btnLp());
        bar.addView(iconButton(Icons.DICE_NEW, "Нова насумична тема", new Runnable() {
            @Override
            public void run() {
                randomThemeMenu();
            }
        }), btnLp());
        bar.addView(iconButton(Icons.REFRESH, "Поново генериши насумичну тему (без нове теме)", new Runnable() {
            @Override
            public void run() {
                regenerateRandomTheme();
            }
        }), btnLp());
        root.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private LinearLayout.LayoutParams btnLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(56), dp(48));
        lp.setMargins(dp(6), dp(12), dp(6), 0);
        return lp;
    }

    private View iconButton(int kind, String desc, final Runnable action) {
        android.widget.ImageView v = new android.widget.ImageView(act);
        v.setImageDrawable(new Icons(kind, text()));
        v.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        v.setPadding(dp(12), dp(10), dp(12), dp(10));
        v.setBackground(buttonBg());
        v.setContentDescription(desc);
        if (Build.VERSION.SDK_INT >= 26) v.setTooltipText(desc);
        v.setClickable(true);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View x) {
                action.run();
            }
        });
        return v;
    }

    private android.graphics.drawable.StateListDrawable buttonBg() {
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(Dark.mix(bg(), text(), 0.14f));
        normal.setCornerRadius(dp(4));
        GradientDrawable pressed = new GradientDrawable();
        pressed.setColor(Dark.mix(bg(), text(), 0.26f));
        pressed.setCornerRadius(dp(4));
        android.graphics.drawable.StateListDrawable states = new android.graphics.drawable.StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed }, pressed);
        states.addState(new int[0], normal);
        return states;
    }

    /** Boje dugmeta se zadaju izricito iz teme (inace ostaju u bojama teme prozora iz trenutka otvaranja). */
    private void styleButton(Button b) {
        b.setBackground(buttonBg());
        b.setTextColor(text());
        b.setStateListAnimator(null);
    }

    private void showThemeMenu(final Themes.Theme t) {
        showMenu(t.name,
                new int[] { Icons.PALETTE, Icons.PENCIL, Icons.TRASH },
                new String[] { "Уреди боје", "Преименуј", "Обриши" },
                new Runnable[] {
                        new Runnable() {
                            @Override
                            public void run() {
                                editColors(t);
                            }
                        },
                        new Runnable() {
                            @Override
                            public void run() {
                                renameTheme(t);
                            }
                        },
                        new Runnable() {
                            @Override
                            public void run() {
                                deleteTheme(t);
                            }
                        } });
    }

    /** Mali podmeni centriran na ekranu, sirine prema tekstu; svaka stavka ima ikonicu ispred teksta. */
    private void showMenu(String title, int[] icons, String[] labels, final Runnable[] actions) {
        closeMenu();
        FrameLayout scrim = new FrameLayout(act);
        scrim.setBackgroundColor(0x66000000);
        scrim.setClickable(true);
        scrim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeMenu();
            }
        });

        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(0, dp(8), 0, dp(8));
        GradientDrawable cbg = new GradientDrawable();
        cbg.setColor(0xff000000 | bg());
        cbg.setCornerRadius(dp(8));
        cbg.setStroke(dp(1), Dark.mix(bg(), text(), 0.25f));
        card.setBackground(cbg);
        card.setElevation(dp(8));
        card.setClickable(true); // dodir na kartici ne zatvara meni

        TextView tt = label(title, 16f, text());
        tt.setTypeface(Typeface.DEFAULT_BOLD);
        tt.setSingleLine(true);
        tt.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tt.setMaxWidth(dp(240));
        tt.setPadding(dp(20), dp(8), dp(20), dp(10));
        card.addView(tt);

        for (int i = 0; i < labels.length; i++) {
            final Runnable action = actions[i];
            LinearLayout row = new LinearLayout(act);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(20), dp(12), dp(24), dp(12));
            GradientDrawable pr = new GradientDrawable();
            pr.setColor(Dark.mix(bg(), text(), 0.18f));
            android.graphics.drawable.StateListDrawable st = new android.graphics.drawable.StateListDrawable();
            st.addState(new int[] { android.R.attr.state_pressed }, pr);
            st.addState(new int[0], new ColorDrawable(0));
            row.setBackground(st);

            android.widget.ImageView iv = new android.widget.ImageView(act);
            iv.setImageDrawable(new Icons(icons[i], text()));
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(24), dp(24));
            ilp.rightMargin = dp(16);
            row.addView(iv, ilp);
            row.addView(label(labels[i], 16f, text()));

            row.setClickable(true);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeMenu();
                    action.run();
                }
            });
            card.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        scrim.addView(card, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        menuOverlay = scrim;
        host.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /** Zatvara kompaktni podmeni; vraca true ako je bio otvoren. */
    private boolean closeMenu() {
        View m = menuOverlay;
        if (m == null) return false;
        menuOverlay = null;
        if (m.getParent() instanceof ViewGroup) ((ViewGroup) m.getParent()).removeView(m);
        return true;
    }

    private void addTheme() {
        promptText("Назив нове теме:", "", "Направи", new TextCb() {
            @Override
            public void onText(String name) {
                Themes.Theme src = Themes.find(themes, selId);
                Themes.Theme n = src.copy("t" + System.currentTimeMillis(), name);
                themes.add(n);
                selId = n.id;
                saveThemes();
                build();
                editColors(n);
            }
        });
    }

    private static final String KEY_LAST_BASE = "last_base_color";

    private void randomThemeMenu() {
        builder()
                .setTitle("Насумична тема")
                .setItems(new CharSequence[] { "Светла", "Тамна", "Од моје боје…" },
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                if (which == 2) {
                                    // osnovna boja: svijetla/tamna tema se odredjuje po njenoj svjetlini
                                    // pocetna boja je posljednja koju je korisnik unio (a ne pozadina trenutne teme)
                                    ColorPicker.show(act, sp.getInt(KEY_LAST_BASE, Dark.BG), bg(), text(),
                                            new ColorPicker.Listener() {
                                                @Override
                                                public void onColor(int color) {
                                                    sp.edit().putInt(KEY_LAST_BASE, color).commit();
                                                    addRandomTheme(RandomTheme.ANY, Integer.valueOf(color));
                                                }
                                            });
                                } else {
                                    addRandomTheme(which == 0 ? RandomTheme.LIGHT : RandomTheme.DARK, null);
                                }
                            }
                        })
                .show();
    }

    /** Pravi NOVU nasumicnu temu (kontrast je zagarantovan) i odmah je bira. Postojece teme se ne diraju. */
    private void addRandomTheme(int mode, Integer base) {
        RandomTheme.Result res = RandomTheme.generate(new java.util.Random(), mode, base);
        if (base != null && res.adjusted) toast("Боја је прилагођена ради читљивости");
        Themes.Theme n = themeFrom(res);
        themes.add(n);
        selId = n.id;
        saveThemes();
        build();
    }

    /**
     * Ponovo generise izabranu (nasumicnu ili "iz boje") temu na istom mjestu, bez pravljenja nove.
     * Svijetla/tamna i osnovna boja uzimaju se iz naziva teme; preimenovane teme se ne diraju.
     */
    private void regenerateRandomTheme() {
        Themes.Theme cur = Themes.find(themes, selId);
        boolean fromColor = cur.name.startsWith(RandomTheme.FROM_COLOR_PREFIX + " ·");
        boolean random = cur.name.startsWith(RandomTheme.NAME_PREFIX + " ·");
        if (!fromColor && !random) {
            toast("Изабрана тема није насумична");
            return;
        }
        int mode = cur.name.endsWith("светла") ? RandomTheme.LIGHT
                : (cur.name.endsWith("тамна") ? RandomTheme.DARK : RandomTheme.ANY);
        Integer base = null;
        if (fromColor) {
            int h = cur.name.indexOf('#');
            if (h >= 0) {
                int e = cur.name.indexOf(' ', h);
                base = Colors.parse(cur.name.substring(h + 1, e < 0 ? cur.name.length() : e));
            }
            if (base == null) base = Integer.valueOf(sp.getInt(KEY_LAST_BASE, Dark.BG));
        }
        RandomTheme.Result res = RandomTheme.generate(new java.util.Random(), mode, base);
        Themes.Theme n = themeFrom(res);
        int at = themes.indexOf(cur);
        if (at >= 0) themes.set(at, n);
        else themes.add(n);
        selId = n.id;
        saveThemes();
        build();
    }

    private Themes.Theme themeFrom(RandomTheme.Result res) {
        Themes.Theme n = new Themes.Theme("t" + System.currentTimeMillis(), res.name);
        n.colors.putAll(res.colors);
        for (String[] c : Colors.HIDDEN) {
            if (!n.colors.containsKey(c[0])) n.colors.put(c[0], Colors.def(c[0]));
        }
        return n;
    }

    private void renameTheme(final Themes.Theme t) {
        promptText("Преименуј тему", t.name, "Сачувај", new TextCb() {
            @Override
            public void onText(String name) {
                t.name = name;
                saveThemes();
                build();
            }
        });
    }

    private void deleteTheme(final Themes.Theme t) {
        if (themes.size() <= 1) {
            toast("Мора остати бар једна тема");
            return;
        }
        builder()
                .setTitle("Обриши тему")
                .setMessage("Обрисати тему „" + t.name + "“?")
                .setPositiveButton("Обриши", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        themes.remove(t);
                        if (selId.equals(t.id)) selId = themes.get(0).id;
                        saveThemes();
                        build();
                    }
                })
                .setNegativeButton("Одустани", null)
                .show();
    }

    // ------------------------------------------------------------------ uredjivanje boja teme

    private void editColors(final Themes.Theme t) {
        final LinearLayout rows = new LinearLayout(act);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(dp(16), dp(8), dp(16), 0);
        fillRows(rows, t);

        ScrollView s = new ScrollView(act);
        s.addView(rows);

        final AlertDialog d = builder()
                .setTitle("Боје: " + t.name)
                .setView(s)
                .setPositiveButton("Готово", null)
                .setNeutralButton("Врати", null)
                .create();
        // pozadina editora boja je c_bg, a slova su c_text (naslov i dugmad se boje izricito)
        android.graphics.drawable.GradientDrawable dbg = new android.graphics.drawable.GradientDrawable();
        dbg.setColor(0xff000000 | bg());
        dbg.setCornerRadius(dp(6));
        d.getWindow().setBackgroundDrawable(dbg);
        d.show();
        int tid = act.getResources().getIdentifier("alertTitle", "id", "android");
        View title = tid != 0 ? d.findViewById(tid) : null;
        if (title instanceof TextView) ((TextView) title).setTextColor(text());
        d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(text());
        d.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(text());
        // "Vrati" ne zatvara dijalog
        d.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, String> orig = Themes.builtinColors(t.id);
                t.colors.clear();
                if (orig != null) t.colors.putAll(orig);
                saveThemes();
                fillRows(rows, t);
            }
        });
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dlg) {
                build(); // osvjezi pregled boja i boje ovog ekrana
            }
        });
    }

    private void fillRows(LinearLayout rows, Themes.Theme t) {
        rows.removeAllViews();
        for (String[] c : Colors.ALL) {
            String g = Colors.groupBefore(c[0]);
            if (g != null) {
                TextView h = new TextView(act);
                h.setText(g);
                h.setTextSize(13f);
                h.setTypeface(Typeface.DEFAULT_BOLD);
                h.setTextColor(Dark.light() ? 0xff1a56b0 : 0xff8ab4f8);
                h.setPadding(0, dp(14), 0, dp(4));
                rows.addView(h);
            }
            rows.addView(makeColorRow(t, c[0], c[1]));
        }
    }

    private View makeColorRow(final Themes.Theme t, final String key, String label) {
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(2), 0, dp(2));

        TextView tv = new TextView(act);
        tv.setText(label);
        tv.setTextSize(15f);
        tv.setTextColor(text());
        row.addView(tv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final View swatch = new View(act);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(36), dp(36));
        lp.setMargins(dp(8), 0, dp(8), 0);
        row.addView(swatch, lp);

        final EditText et = new EditText(act);
        et.setSingleLine(true);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        et.setFilters(new InputFilter[] { new InputFilter.LengthFilter(7) });
        et.setTypeface(Typeface.MONOSPACE);
        et.setTextSize(15f);
        et.setTextColor(text());
        et.setBackgroundTintList(android.content.res.ColorStateList.valueOf(dim()));
        String cur = t.color(key);
        et.setText("#" + cur);
        setSwatch(swatch, Colors.parse(cur).intValue(), 1);
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void afterTextChanged(Editable s) {
                Integer c = Colors.parse(s.toString());
                if (c != null) {
                    setSwatch(swatch, c.intValue(), 1);
                    t.colors.put(key, Colors.hex(c.intValue()));
                    saveThemes();
                } else {
                    swatch.setBackground(null);
                }
            }
        });
        swatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Integer cur = Colors.parse(et.getText().toString());
                int start = cur != null ? cur.intValue() : Colors.parse(t.color(key)).intValue();
                ColorPicker.show(act, start, bg(), text(), new ColorPicker.Listener() {
                    @Override
                    public void onColor(int color) {
                        et.setText("#" + Colors.hex(color));
                    }
                });
            }
        });
        row.addView(et, new LinearLayout.LayoutParams(dp(100), LinearLayout.LayoutParams.WRAP_CONTENT));
        return row;
    }

    // ------------------------------------------------------------------ pomocno

    private interface TextCb {
        void onText(String s);
    }

    private void promptText(String title, String initial, String ok, final TextCb cb) {
        final EditText et = new EditText(act);
        et.setSingleLine(true);
        et.setFilters(new InputFilter[] { new InputFilter.LengthFilter(30) });
        et.setTextColor(text());
        et.setBackgroundTintList(android.content.res.ColorStateList.valueOf(dim()));
        et.setText(initial);
        et.setSelection(et.getText().length());

        LinearLayout wrap = new LinearLayout(act);
        wrap.setPadding(dp(20), dp(8), dp(20), 0);
        wrap.addView(et, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        builder()
                .setTitle(title)
                .setView(wrap)
                .setPositiveButton(ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        String s = et.getText().toString().trim();
                        if (s.length() == 0) {
                            toast("Назив не може бити празан");
                            return;
                        }
                        cb.onText(s);
                    }
                })
                .setNegativeButton("Одустани", null)
                .show();
    }

    private void setSwatch(View v, int color, int strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setStroke(dp(strokeDp), 0xff888888);
        v.setBackground(g);
    }

    private void toast(String s) {
        Toast.makeText(act, s, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return Math.round(v * act.getResources().getDisplayMetrics().density);
    }
}