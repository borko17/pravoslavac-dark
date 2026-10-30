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

import de.robv.android.xposed.XSharedPreferences;
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
    private ScrollView scroll;

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
                if (e.getAction() == KeyEvent.ACTION_UP) close(true);
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
                    else s.close(true);
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

    private int text() {
        return Dark.TEXT;
    }

    /** Sekundarni tekst: boja teksta pomijesana s pozadinom. */
    private int dim() {
        int t = Dark.TEXT;
        int b = Dark.BG;
        int r = (((t >> 16) & 255) * 65 + ((b >> 16) & 255) * 35) / 100;
        int g = (((t >> 8) & 255) * 65 + ((b >> 8) & 255) * 35) / 100;
        int bl = ((t & 255) * 65 + (b & 255) * 35) / 100;
        return 0xff000000 | (r << 16) | (g << 8) | bl;
    }

    private AlertDialog.Builder builder() {
        return new AlertDialog.Builder(act, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
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
        final int scrollY = scroll != null ? scroll.getScrollY() : 0;

        int pad = dp(16);
        LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Dark.BG);

        root.addView(makeSwitch("Preskoči uvodni ekran", "Aplikacija se otvara bez uvodnog ekrana.",
                Prefs.KEY_SKIP_SPLASH, true));
        root.addView(makeSwitch("Tekst poravnat lijevo", "Umjesto obostranog poravnanja.",
                Prefs.KEY_LEFT, true));
        root.addView(makeSwitch("Aplikacija otvara kalendar", "Otvara se kalendar umjesto žitija za taj dan.",
                Prefs.KEY_OPEN_CALENDAR, true));

        addThemeSection(root);

        scroll = new ScrollView(act);
        scroll.setBackgroundColor(Dark.BG);
        scroll.setFillViewport(true);
        scroll.addView(root);
        host.setBackgroundColor(Dark.BG);
        host.removeAllViews();
        host.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scroll.post(new Runnable() {
            @Override
            public void run() {
                scroll.scrollTo(0, scrollY);
            }
        });
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
                    darkOff = !checked;
                    dirty = true;
                    if (checked) {
                        Dark.reload();
                        Hook.reapplyResources();
                    } else {
                        toast("Isključivanje teme se primjenjuje nakon ponovnog pokretanja aplikacije");
                    }
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
        root.addView(makeSwitch("Teme",
                "Uključeno: tamna tema u bojama odabrane teme. Isključeno: originalne boje aplikacije.",
                Prefs.KEY_DARK, true));

        TextView h = label("Odaberi temu (⋮ = uredi boje, preimenuj, obriši)", 14f, dim());
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

        Button add = new Button(act);
        add.setText("+ Nova tema");
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTheme();
            }
        });
        root.addView(add);

        Button reset = new Button(act);
        reset.setText("Vrati podrazumijevane teme");
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmResetThemes();
            }
        });
        root.addView(reset);

        Button imp = new Button(act);
        imp.setText("Uvezi postavke iz stare aplikacije modula");
        imp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importOld();
            }
        });
        root.addView(imp);
    }

    /** Jednokratni uvoz tema i prekidaca koje je stara aplikacija modula spremila u LSPosed. */
    private void importOld() {
        try {
            XSharedPreferences old = new XSharedPreferences(Prefs.MODULE_PKG, Prefs.OLD_FILE);
            old.makeWorldReadable();
            String json = old.getString(Themes.KEY_THEMES, null);
            if (json == null) {
                toast("Nema starih postavki za uvoz");
                return;
            }
            SharedPreferences.Editor e = sp.edit();
            e.putString(Themes.KEY_THEMES, json);
            String sel = old.getString(Themes.KEY_SEL, null);
            if (sel != null) e.putString(Themes.KEY_SEL, sel);
            String[] bools = { Prefs.KEY_SKIP_SPLASH, Prefs.KEY_LEFT, Prefs.KEY_OPEN_CALENDAR, Prefs.KEY_DARK };
            for (String k : bools) {
                if (old.contains(k)) e.putBoolean(k, old.getBoolean(k, true));
            }
            e.commit();
            Prefs.refresh();
            loadThemes();
            Dark.reload();
            Hook.reapplyResources();
            dirty = true;
            build();
            toast("Postavke uvezene");
        } catch (Throwable t) {
            XposedBridge.log(t);
            toast("Uvoz nije uspio");
        }
    }

    private void confirmResetThemes() {
        builder()
                .setTitle("Vrati podrazumijevane teme")
                .setMessage("Sve tvoje teme i izmjene bit će obrisane, a vratit će se ugrađene teme.")
                .setNegativeButton("Otkaži", null)
                .setPositiveButton("Vrati", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        sp.edit().remove(Themes.KEY_THEMES).remove(Themes.KEY_SEL).commit();
                        loadThemes();
                        saveThemes();
                        build();
                        toast("Teme vraćene na podrazumijevane");
                    }
                })
                .show();
    }

    private void showThemeMenu(final Themes.Theme t) {
        builder()
                .setTitle(t.name)
                .setItems(new CharSequence[] { "Uredi boje", "Preimenuj", "Obriši" },
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                if (which == 0) editColors(t);
                                else if (which == 1) renameTheme(t);
                                else deleteTheme(t);
                            }
                        })
                .show();
    }

    private void addTheme() {
        promptText("Nova tema", "Nova tema", "Napravi", new TextCb() {
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

    private void renameTheme(final Themes.Theme t) {
        promptText("Preimenuj temu", t.name, "Sačuvaj", new TextCb() {
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
            toast("Mora ostati bar jedna tema");
            return;
        }
        builder()
                .setTitle("Obriši temu")
                .setMessage("Obrisati temu „" + t.name + "“?")
                .setPositiveButton("Obriši", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        themes.remove(t);
                        if (selId.equals(t.id)) selId = themes.get(0).id;
                        saveThemes();
                        build();
                    }
                })
                .setNegativeButton("Odustani", null)
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
                .setTitle("Boje: " + t.name)
                .setView(s)
                .setPositiveButton("Gotovo", null)
                .setNeutralButton("Vrati", null)
                .create();
        d.show();
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
                h.setTextColor(0xff8ab4f8);
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
                ColorPicker.show(act, start, new ColorPicker.Listener() {
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
                            toast("Naziv ne može biti prazan");
                            return;
                        }
                        cb.onText(s);
                    }
                })
                .setNegativeButton("Odustani", null)
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