package patch.kalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.ImageView;
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
 * Pozadina ekrana je Dark.SET_BG, a tekst Dark.SET_TEXT (c_set_bg i c_set_text odabrane teme), prekidaci Dark.SET_SWITCH.
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
    private FontPanel fontPanel;       // ugradjeno preuzimanje fontova u tabu Фонтови
    private boolean fontPanelOpen;
    /** Odabrani tab: 0 = opcije (prekidaci), 1 = teme, 2 = fontovi. */
    private int tab;
    private int builtTab = -1;
    private ScrollView scroll;

    private List<Themes.Theme> themes;
    /** Zadnja boja koju je izmijenio otvoreni dijalog (uzastopne izmjene iste boje su jedan korak vracanja). */
    private String lastUndoKey;
    /** Osvjezava stanje dugmeta za vracanje u otvorenom dijalogu (ili null). */
    private Runnable undoRefresh;
    private String selId;
    /** Stanje boja u aplikaciji trenutno: true = originalne ("Православац"), false = boje teme. */
    private boolean appliedOrig;

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
        if (!s.backInside()) s.close(true);
        return true;
    }

    /** Back dok je otvoreno preuzimanje fontova: zatvara samo taj dio (isto kao dugme "Затвори преузимање фонтова"). */
    private boolean backInside() {
        if (tab == 2 && fontPanelOpen) {
            fontPanelOpen = false;
            build();
            return true;
        }
        return false;
    }

    private void open() {
        loadThemes();
        host = new Host(act);
        host.setTag(Dark.TAG_SETTINGS, Boolean.TRUE);
        host.setClickable(true);
        host.setFocusableInTouchMode(true);
        build();
        attach();
    }

    /** Poslije recreate() podesavanja se ponovo otvaraju (ako je ovo postavljeno). */
    private static boolean pendingReopen;
    private static int pendingTab;

    /** Ponovo napravi ekran aplikacije (nova tema/originalne boje) i odmah vrati podesavanja na isti tab. */
    private void recreateKeepingSettings() {
        final Activity a = act;
        pendingReopen = true;
        pendingTab = tab;
        close(false);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    a.recreate();
                } catch (Throwable t) {
                    pendingReopen = false;
                    XposedBridge.log(t);
                }
            }
        });
    }

    /** Poziva se iz onCreate-a glavne aktivnosti: ako je recreate pokrenut iz podesavanja, otvori ih opet. */
    static void reopenIfPending(final Activity a) {
        if (!pendingReopen) return;
        pendingReopen = false;
        final int t = pendingTab;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (a.isFinishing() || a.isDestroyed()) return;
                    show(a);
                    SettingsUi s = current;
                    if (s != null && !s.closed && s.act == a && s.tab != t) {
                        s.tab = t;
                        s.build();
                    }
                } catch (Throwable e) {
                    XposedBridge.log(e);
                }
            }
        }, 250);
    }

    private void close(boolean apply) {
        if (closed) return;
        closed = true;
        if (fontPanel != null) {
            fontPanel.dispose();
            fontPanel = null;
        }
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
                if (e.getAction() == KeyEvent.ACTION_UP && !backInside()) close(true);
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
            OnBackInvokedCallback cb = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                cb = new OnBackInvokedCallback() {
                    @Override
                    public void onBackInvoked() {
                        if (s.drawerOpen()) Hook.closeDrawer(a);
                        else if (!s.backInside()) s.close(true);
                    }
                };
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                a.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                        OnBackInvokedDispatcher.PRIORITY_OVERLAY, cb);
            }
            return cb;
        }

        static void unregister(Activity a, Object cb) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                a.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback((OnBackInvokedCallback) cb);
            }
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

    /** Izabrana je tema sa bojama (true) ili "Православац" = originalne boje aplikacije (false). */
    private boolean themed() {
        return !Themes.isOrig(selId);
    }

    private int bg() {
        return themed() ? Dark.SET_BG : 0xfffaf5e9;
    }

    /** Pozadina podesavanja je svijetla (odlucuje o svijetloj/tamnoj varijanti dijaloga). */
    private boolean setLight() {
        return Dark.lum(bg()) >= 0.5f;
    }

    private int bar() {
        return themed() ? Dark.BAR : 0xff2f3e5c;
    }

    private int tabText() {
        return themed() ? Dark.TAB_TEXT : 0xffffffff;
    }

    private int text() {
        return themed() ? Dark.SET_TEXT : 0xff000000;
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
        return new AlertDialog.Builder(act, themed() && !setLight()
                ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    }

    private TextView label(String s, float size, int color) {
        TextView tv = new TextView(act);
        // oznaka PRIJE setText: tekst jos nema roditelja, pa bi hook prepoznao "Постови" kao oznaku iz kalendara
        tv.setTag(Dark.TAG_SETTINGS, Boolean.TRUE);
        tv.setText(s);
        tv.setTextSize(size);
        tv.setTextColor(color);
        return tv;
    }

    /** (Ponovo) gradi cijeli ekran; cuva poziciju skrolovanja. */
    private void build() {
        final int scrollY = (scroll != null && builtTab == tab) ? scroll.getScrollY() : 0;
        builtTab = tab;

        int pad = dp(16);
        LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(bg());

        if (tab == 0) {
            root.addView(makeSwitch("Прескочи уводни екран", "Апликација се отвара без уводног екрана.",
                    Prefs.KEY_SKIP_SPLASH, true));
            root.addView(makeSwitch("Апликација отвара календар", "Отвара се календар уместо житија за тај дан.",
                    Prefs.KEY_OPEN_CALENDAR, true));
            root.addView(makeSwitch("Текст поравнат лево", "Уместо обостраног поравнања.",
                    Prefs.KEY_LEFT, true));
        } else if (tab == 1) {
            addThemeSection(root);
        } else {
            addFontSection(root);
        }

        scroll = new ScrollView(act);
        scroll.setBackgroundColor(bg());
        scroll.setFillViewport(true);
        scroll.addView(root);
        scroll.getViewTreeObserver().addOnScrollChangedListener(new android.view.ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if (tab == 2 && fontPanel != null && fontPanelOpen) fontPanel.onScroll();
            }
        });

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
        String[] names = { "Опције", "Теме", "Фонтови" };
        int shown = names.length;
        for (int i = 0; i < shown; i++) {
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

    /** Boja prekidaca (c_switch): ukljucen = boja teme, iskljucen = neutralna nijansa teksta. */
    private void tintSwitch(Switch sw) {
        int on = 0xff000000 | Dark.SET_SWITCH;
        int offThumb = Dark.mix(bg(), text(), 0.65f);
        int offTrack = 0x55000000 | (text() & 0xffffff);
        int onTrack = 0x88000000 | (on & 0xffffff);
        int[][] st = { new int[] { android.R.attr.state_checked }, new int[0] };
        android.content.res.ColorStateList thumb = new android.content.res.ColorStateList(st, new int[] { on, offThumb });
        android.content.res.ColorStateList track = new android.content.res.ColorStateList(st, new int[] { onTrack, offTrack });
        try {
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                sw.setThumbTintList(thumb);
                sw.setTrackTintList(track);
            } else {
                // Android 5.x: nema setThumbTintList; boji se direktno unutrasnje drawable-e
                java.lang.reflect.Field tf = Switch.class.getDeclaredField("mThumbDrawable");
                java.lang.reflect.Field kf = Switch.class.getDeclaredField("mTrackDrawable");
                tf.setAccessible(true);
                kf.setAccessible(true);
                android.graphics.drawable.Drawable td = (android.graphics.drawable.Drawable) tf.get(sw);
                android.graphics.drawable.Drawable kd = (android.graphics.drawable.Drawable) kf.get(sw);
                if (td != null) td.mutate().setTintList(thumb);
                if (kd != null) kd.mutate().setTintList(track);
            }
        } catch (Throwable ignored) {
        }
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
        if (themed()) tintSwitch(sw);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                sp.edit().putBoolean(key, checked).commit();
                Prefs.refresh();
                if (Prefs.KEY_DARK.equals(key)) {
                    boolean ok = true;
                    if (checked) {
                        darkOff = false;
                        Dark.reload();
                        Hook.reapplyResources();
                    } else {
                        // vrati originalne boje resursa
                        ok = Hook.restoreResources(act);
                        darkOff = !ok;
                    }
                    if (ok) {
                        // odmah ponovo napravi ekran i vrati se u podesavanja (isti tab)
                        recreateKeepingSettings();
                        return;
                    }
                    dirty = true;
                    toast("Поново покрени апликацију.");
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
        boolean legacyOff = !sp.getBoolean(Prefs.KEY_DARK, true);   // stari prekidac "Теме и фонтови" je bio iskljucen
        if (legacyOff) selId = Themes.ORIG_ID;                       // to je sada tema "Православац"
        appliedOrig = Themes.isOrig(selId);
        if (json == null || legacyOff) saveThemes(); // prvo otvaranje: upisi ugradjene teme
    }

    /** Upisuje teme i odmah primjenjuje boje (Dark + resursi); ekran se osvjezava pri zatvaranju. */
    private boolean saveThemes() {
        sp.edit()
                .putString(Themes.KEY_THEMES, Themes.toJson(themes))
                .putString(Themes.KEY_SEL, selId)
                .putBoolean(Prefs.KEY_DARK, true)   // stari prekidac vise ne postoji: tema se bira u listi
                .commit();
        Prefs.refresh();
        Dark.reload();
        boolean nowOrig = Themes.isOrig(selId);
        boolean changed = nowOrig != appliedOrig;
        appliedOrig = nowOrig;
        if (nowOrig) {
            if (changed) {
                // "Православац": vrati originalne boje resursa
                boolean ok = Hook.restoreResources(act);
                darkOff = !ok;
            }
        } else {
            darkOff = false;
            Hook.reapplyResources();
            applyToolbarNow();
        }
        dirty = true;
        return changed;
    }

    /** Odmah oboji gornju traku aplikacije (ne ceka zatvaranje podesavanja). */
    private void applyToolbarNow() {
        try {
            if (!themed()) return;
            Dark.refreshToolbar(act.getWindow().getDecorView());
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    // ------------------------------------------------------------------ tab "Фонтови"

    private org.json.JSONObject fontsJson;

    private org.json.JSONObject fonts() {
        if (fontsJson == null) {
            try {
                String j = sp.getString(Prefs.KEY_FONTS, null);
                fontsJson = j == null ? new org.json.JSONObject() : new org.json.JSONObject(j);
            } catch (Throwable t) {
                fontsJson = new org.json.JSONObject();
            }
        }
        return fontsJson;
    }

    private org.json.JSONObject fontsSub(String name) throws org.json.JSONException {
        org.json.JSONObject o = fonts().optJSONObject(name);
        if (o == null) {
            o = new org.json.JSONObject();
            fonts().put(name, o);
        }
        return o;
    }

    private String fontOf(String key) {
        org.json.JSONObject f = fonts().optJSONObject("f");
        String v = f == null ? "" : f.optString(key, "");
        return v.length() == 0 ? null : v;
    }

    private int sizeOf(String key) {
        org.json.JSONObject o = fonts().optJSONObject("s");
        int v = o == null ? 100 : o.optInt(key, 100);
        return v < 50 ? 50 : (v > 200 ? 200 : v);
    }

    /** Zajednicka velicina svih tekstova (ako su svi isti), inace 100. */
    private int allSize() {
        int common = -1;
        for (String[] c : Colors.ALL) {
            if (!Colors.hasFont(c[0])) continue;
            int v = sizeOf(c[0]);
            if (common == -1) common = v;
            else if (common != v) return 100;
        }
        return common == -1 ? 100 : common;
    }

    private void putFont(String key, String fam) {
        try {
            if (fam == null) fontsSub("f").remove(key);
            else fontsSub("f").put(key, fam);
        } catch (Throwable ignored) {
            // nista
        }
    }

    private void putSize(String key, int pct) {
        try {
            if (pct == 100) fontsSub("s").remove(key);
            else fontsSub("s").put(key, pct);
        } catch (Throwable ignored) {
            // nista
        }
    }

    /** Snima izbor fontova; izmjene se vide kad se podesavanja zatvore (ekran se ponovo napravi). */
    private void saveFonts() {
        sp.edit().putString(Prefs.KEY_FONTS, fonts().toString()).commit();
        Prefs.refresh();
        Fonts.reset();
        dirty = true;
    }

    /** Poslije brisanja preuzetih fontova: tekstovi koji su koristili obrisani font vracaju se na originalni. */
    private void pruneMissingFonts() {
        try {
            Fonts.reset();
            org.json.JSONObject f = fonts().optJSONObject("f");
            if (f == null) return;
            java.util.List<String> have = Fonts.available();
            java.util.List<String> drop = new java.util.ArrayList<String>();
            java.util.Iterator<String> it = f.keys();
            while (it.hasNext()) {
                String k = it.next();
                String fam = f.optString(k, "");
                if (fam.length() > 0 && !have.contains(fam)) drop.add(k);
            }
            if (drop.isEmpty()) return;
            for (String k : drop) f.remove(k);
            saveFonts();   // snimi izbor i oznaci da se ekran aplikacije ponovo napravi pri zatvaranju podesavanja
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private void addFontSection(LinearLayout root) {
        final FontPanel.Pal pal = fontPal();
        TextView toggle = fontPanelOpen
                ? FontPanel.pill(act, pal, "Затвори преузимање фонтова", FontPanel.TONAL, Icons.CLOSE, false)
                : FontPanel.pill(act, pal, "Отвори преузимање фонтова", FontPanel.SUCCESS, Icons.DOWNLOAD, false);
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fontPanelOpen = !fontPanelOpen;
                build();
            }
        });
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.setMargins(0, dp(6), 0, dp(10));
        root.addView(toggle, tlp);

        if (fontPanelOpen) {
            if (fontPanel != null && !fontPanel.sameColors(pal)) {   // promijenjena tema: napravi panel iznova
                fontPanel.dispose();
                fontPanel = null;
            }
            if (fontPanel == null) {
                fontPanel = new FontPanel(act, pal, new Runnable() {
                    @Override
                    public void run() {
                        if (!closed) build();   // osvjezi nazive fontova u tabu
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        pruneMissingFonts();
                    }
                });
            }
            View pv = fontPanel.view();
            if (pv.getParent() instanceof ViewGroup) ((ViewGroup) pv.getParent()).removeView(pv);
            root.addView(pv, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(14));
            root.addView(new View(act), gap);
            fontPanel.start();
        }

        root.addView(makeFontItem(null, "Цео календар (сви текстови)"));
        for (String[] c : Colors.ALL) {
            if (Colors.fontListed(c[0])) root.addView(makeFontItem(c[0], c[1]));
        }
    }

    private String fontLabel(String key) {
        if (key == null) return "Фонт за све: изабери ▾";
        String f = fontOf(key);
        if (f != null && !Fonts.available().contains(f)) f = f + " (није преузет)";
        return "Фонт: " + (f == null ? "оригинални" : f) + " ▾";
    }

    /** Jedan red: naziv teksta, izbor fonta i velicina (key == null: primjenjuje se na sve tekstove odjednom). */
    private View makeFontItem(final String key, String name) {
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(10), 0, dp(6));
        box.addView(label(name, key == null ? 17f : 15f, text()));

        if (key != null && !Colors.hasFont(key)) {   // posjecene linkove preglednik ne dozvoljava posebno
            TextView note = label("Користи фонт и величину линкова (прегледач не дозвољава посебно подешавање).",
                    13f, dim());
            note.setPadding(0, dp(2), 0, dp(4));
            box.addView(note);
            return box;
        }

        LinearLayout line = new LinearLayout(act);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);

        final TextView fontTv = label(fontLabel(key), 15f, dim());
        fontTv.setPadding(0, dp(8), dp(8), dp(8));
        fontTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickFont(key, fontTv);
            }
        });
        line.addView(fontTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView pct = label((key == null ? allSize() : sizeOf(key)) + "%", 15f, text());
        pct.setGravity(Gravity.CENTER);
        pct.setMinWidth(dp(52));
        line.addView(stepButton("−", key, -5, pct));
        line.addView(pct);
        line.addView(stepButton("+", key, 5, pct));
        box.addView(line);
        return box;
    }

    private TextView stepButton(String sign, final String key, final int delta, final TextView pct) {
        TextView b = label(sign, 22f, text());
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(14), dp(4), dp(14), dp(4));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (key == null) {   // ceo kalendar: isti korak za sve tekstove
                    int n = Math.max(50, Math.min(200, allSize() + delta));
                    for (String[] c : Colors.ALL) {
                        if (Colors.hasFont(c[0])) putSize(c[0], n);
                    }
                    saveFonts();
                    build();   // osvjezi procente u svim redovima
                    return;
                }
                int n = Math.max(50, Math.min(200, sizeOf(key) + delta));
                putSize(key, n);
                pct.setText(n + "%");
                saveFonts();
            }
        });
        return b;
    }

    /** Izbor fonta za jedan tekst (key != null) ili za sve tekstove (key == null). */
    private void pickFont(final String key, final TextView label) {
        Fonts.reset();
        java.util.List<String> names = new java.util.ArrayList<String>();
        names.add("Оригинални фонт");
        names.addAll(Fonts.available());
        names.add("＋ Преузми фонтове…");
        final String[] items = names.toArray(new String[0]);
        showStyled(builder()
                .setTitle(key == null ? "Фонт за све текстове" : "Фонт")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        if (which == items.length - 1) {
                            fontPanelOpen = true;
                            build();
                            scroll.post(new Runnable() {
                                @Override
                                public void run() {
                                    scroll.scrollTo(0, 0);
                                }
                            });
                            return;
                        }
                        String fam = which == 0 ? null : items[which];
                        if (key == null) {
                            for (String[] c : Colors.ALL) {
                                if (Colors.hasFont(c[0])) putFont(c[0], fam);
                            }
                            saveFonts();
                            build();
                        } else {
                            putFont(key, fam);
                            saveFonts();
                            label.setText(fontLabel(key));
                        }
                    }
                })
                .setNegativeButton("Одустани", null));
    }

    /** Boje prozora za fontove (iz teme; kad su teme iskljucene, originalne svijetle boje). */
    private FontPanel.Pal fontPal() {
        boolean t = themed();
        int fill = t ? Dark.BTN_BG : 0xff2f3e5c;
        return new FontPanel.Pal(bg(), text(), dim(), fill,
                t ? Dark.BTN_TEXT : 0xffffffff,
                t ? Dark.BTN_BORDER : Dark.mix(fill, 0xffffffff, 0.25f),
                t && !setLight());
    }

    private void addThemeSection(LinearLayout root) {
        TextView h = label("Одабери тему:", 14f, dim());
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
                    if (t.id.equals(selId)) return;
                    selId = t.id;
                    boolean changed = saveThemes();
                    if (changed) {
                        if (!darkOff) {
                            // prelaz "Православац" <-> tema sa bojama: odmah ponovo napravi ekran (isti tab)
                            recreateKeepingSettings();
                            return;
                        }
                        toast("Поново покрени апликацију.");
                    }
                    build();
                }
            });
            row.addView(rb, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            // mali pregled teme: pozadina, traka, naslov, link
            String[] keys = { "c_bg", "c_row", "c_holiday", "c_row_border" };
            for (String k : keys) {
                View dot = new View(act);
                GradientDrawable circle = new GradientDrawable();
                circle.setShape(GradientDrawable.OVAL);
                circle.setColor(Colors.parse(t.color(k)).intValue());
                circle.setStroke(dp(1), 0xff888888);
                dot.setBackground(circle);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(16), dp(16));
                lp.setMargins(dp(2), 0, dp(2), 0);
                row.addView(dot, lp);
            }

            if (Themes.isOrig(t.id)) {
                // "Православац": samo za izbor, bez opcija (uredi, preimenuj, kopiraj, obrisi)
                row.addView(new View(act), new LinearLayout.LayoutParams(dp(38), 1));
                root.addView(row);
                continue;
            }
            ImageView more = new ImageView(act);
            more.setImageDrawable(Icons.get(Icons.OPTIONS, text(), dp(22)));
            more.setScaleType(ImageView.ScaleType.CENTER);
            more.setPadding(dp(10), dp(8), dp(6), dp(8));
            more.setContentDescription("Опције теме");
            more.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showThemeMenu(t);
                }
            });
            row.addView(more);
            root.addView(row);
        }

        // ikonice: nova tema | nova nasumicna tema (uvijek nova) | ponovo generisi izabranu nasumicnu temu
        LinearLayout icons = new LinearLayout(act);
        icons.setOrientation(LinearLayout.HORIZONTAL);
        icons.setGravity(Gravity.CENTER);
        icons.setPadding(0, dp(12), 0, dp(4));
        icons.addView(iconButton(Icons.NEW, "Нова тема", 1f, new Runnable() {
            @Override
            public void run() {
                addTheme();
            }
        }));
        icons.addView(iconButton(Icons.RANDOM_NEW, "Нова насумична тема", 1f, new Runnable() {
            @Override
            public void run() {
                randomThemeMenu();
            }
        }));
        boolean canRegen = isGeneratedTheme(Themes.find(themes, selId));
        icons.addView(iconButton(Icons.REGENERATE, "Поново генериши изабрану насумичну тему",
                canRegen ? 1f : 0.35f, new Runnable() {
                    @Override
                    public void run() {
                        regenerateRandomTheme();
                    }
                }));
        icons.addView(iconButton(Icons.COPY_ALL, "Копирај све теме", 1f, new Runnable() {
            @Override
            public void run() {
                copyThemes(themes);
            }
        }));
        icons.addView(iconButton(Icons.PASTE, "Учитај копиране теме", 1f, new Runnable() {
            @Override
            public void run() {
                pasteThemes();
            }
        }));
        root.addView(icons);
    }

    /** Dugme sa ikonicom (boje iz teme, zadate izricito). Dugi pritisak pokazuje znacenje. */
    private View iconButton(int icon, final String hint, float alpha, final Runnable action) {
        ImageView v = new ImageView(act);
        v.setImageDrawable(Icons.get(icon, text(), dp(26)));
        v.setScaleType(ImageView.ScaleType.CENTER);
        v.setBackground(pressBg(dp(8), Dark.mix(bg(), text(), 0.14f), Dark.mix(bg(), text(), 0.26f)));
        v.setContentDescription(hint);
        v.setAlpha(alpha);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View x) {
                action.run();
            }
        });
        v.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View x) {
                toast(hint);
                return true;
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(48), dp(48));
        lp.setMargins(dp(4), 0, dp(4), 0);
        v.setLayoutParams(lp);
        return v;
    }

    /** Pozadina sa stanjem pritiska (boje se zadaju izricito iz teme). */
    private android.graphics.drawable.Drawable pressBg(int radius, int normal, int pressed) {
        GradientDrawable n = new GradientDrawable();
        n.setColor(normal);
        n.setCornerRadius(radius);
        GradientDrawable p = new GradientDrawable();
        p.setColor(pressed);
        p.setCornerRadius(radius);
        android.graphics.drawable.StateListDrawable states = new android.graphics.drawable.StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed }, p);
        states.addState(new int[0], n);
        return states;
    }

    /** Pozadina prozora: boja teme (c_bg) i svjetliji obrub, da se vide ivice. */
    private GradientDrawable windowBg(int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xff000000 | bg());
        g.setCornerRadius(radius);
        g.setStroke(dp(2), Dark.mix(bg(), text(), 0.38f));
        return g;
    }

    /** Prikazuje dijalog sa bojama teme: pozadina i obrub prozora, boje naslova, poruke i dugmadi. */
    private AlertDialog showStyled(AlertDialog.Builder b) {
        AlertDialog d = b.create();
        if (d.getWindow() != null && d.getWindow().getDecorView() != null) {
            d.getWindow().getDecorView().setTag(Dark.TAG_SETTINGS, Boolean.TRUE);
        }
        d.getWindow().setBackgroundDrawable(windowBg(dp(8)));
        d.show();
        int tid = act.getResources().getIdentifier("alertTitle", "id", "android");
        View t = tid != 0 ? d.findViewById(tid) : null;
        if (t instanceof TextView) ((TextView) t).setTextColor(text());
        int mid = act.getResources().getIdentifier("message", "id", "android");
        View m = mid != 0 ? d.findViewById(mid) : null;
        if (m instanceof TextView) ((TextView) m).setTextColor(text());
        int[] ids = { AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL };
        for (int id : ids) {
            Button bt = d.getButton(id);
            if (bt != null) bt.setTextColor(text());
        }
        return d;
    }

    /** Mali meni teme (ikonica + tekst). */
    private void showThemeMenu(final Themes.Theme t) {
        showIconMenu(t.name,
                new int[] { Icons.EDIT_COLORS, Icons.RENAME, Icons.COPY, Icons.DELETE },
                new String[] { "Уреди боје", "Преименуј", "Копирај", "Обриши" },
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
                                copyThemes(java.util.Collections.singletonList(t));
                            }
                        },
                        new Runnable() {
                            @Override
                            public void run() {
                                deleteTheme(t);
                            }
                        } }, true);
    }

    /** Mali meni sa ikonicama: prozor je sirine sadrzaja, centriran, sa obrubom. */
    private void showIconMenu(String titleText, int[] icons, String[] captions, Runnable[] actions) {
        showIconMenu(titleText, icons, captions, actions, false);
    }

    /** withExit = true dodaje na dno red "Одустани" koji samo zatvara meni. */
    private void showIconMenu(String titleText, int[] icons, String[] captions, Runnable[] actions,
            boolean withExit) {
        final Dialog d = new Dialog(act, themed() && !setLight()
                ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        // bocni razmak = debljina obruba, da redovi ne prelaze preko njega
        box.setPadding(dp(2), dp(6), dp(2), dp(6));

        TextView title = label(titleText, 14f, dim());
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setMaxWidth(dp(260));
        title.setPadding(dp(20), dp(10), dp(28), dp(8));
        box.addView(title);

        for (int i = 0; i < icons.length; i++) {
            menuRow(box, d, icons[i], captions[i], actions[i]);
        }
        if (withExit) menuRow(box, d, -1, "Одустани", null, dim());

        d.setContentView(box);
        Window w = d.getWindow();
        w.setBackgroundDrawable(windowBg(dp(10)));
        // sirina po sadrzaju (tacna sirina u pikselima zaobilazi minimalnu sirinu dijaloga iz teme)
        int maxW = act.getResources().getDisplayMetrics().widthPixels - dp(64);
        box.measure(View.MeasureSpec.makeMeasureSpec(maxW, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        w.setLayout(Math.max(box.getMeasuredWidth(), dp(200)), WindowManager.LayoutParams.WRAP_CONTENT);
        w.setGravity(Gravity.CENTER);
        d.show();
    }

    private void menuRow(LinearLayout box, final Dialog d, int icon, String caption, final Runnable action) {
        menuRow(box, d, icon, caption, action, text());
    }

    /** icon < 0 = bez ikonice (prazan razmak iste sirine); action == null = samo zatvara meni. */
    private void menuRow(LinearLayout box, final Dialog d, int icon, String caption, final Runnable action,
            int captionColor) {
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(20), dp(13), dp(28), dp(13));
        row.setBackground(pressBg(0, 0, Dark.mix(bg(), text(), 0.18f)));
        if (icon >= 0) {
            ImageView iv = new ImageView(act);
            iv.setImageDrawable(Icons.get(icon, text(), dp(22)));
            row.addView(iv, new LinearLayout.LayoutParams(dp(22), dp(22)));
        } else {
            row.addView(new View(act), new LinearLayout.LayoutParams(dp(22), dp(22)));
        }
        TextView tv = label(caption, 16f, captionColor);
        tv.setMaxWidth(dp(210)); // duzi tekst ide u dva reda, da prozor ne postane preSirok
        tv.setPadding(dp(16), 0, 0, 0);
        row.addView(tv);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
                if (action != null) action.run();
            }
        });
        box.addView(row);
    }

    // ------------------------------------------------------------------ kopiranje i ucitavanje tema (klipbord)

    /** Stavlja zadate teme u klipbord kao JSON. */
    private void copyThemes(List<Themes.Theme> list) {
        try {
            ClipboardManager cm = (ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("kalendar-teme", Themes.toJson(list)));
            toast(list.size() == 1 ? "Тема је копирана" : "Копирано тема: " + list.size());
        } catch (Throwable e) {
            toast("Копирање није успјело");
        }
    }

    private boolean sameTheme(Themes.Theme a, Themes.Theme b) {
        if (!a.name.equals(b.name)) return false;
        for (String[] c : Colors.ALL) {
            if (!a.color(c[0]).equals(b.color(c[0]))) return false;
        }
        for (String[] c : Colors.HIDDEN) {
            if (!a.color(c[0]).equals(b.color(c[0]))) return false;
        }
        return true;
    }

    /**
     * Dodaje teme iz klipborda kao nove (stare se ne diraju). Tema sa istim nazivom i istim bojama kao postojeca
     * se preskace, a ako je naziv zauzet (sa drugim bojama) dobija sufiks " 2", " 3"...
     */
    private void pasteThemes() {
        String text = null;
        try {
            ClipboardManager cm = (ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = cm.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                CharSequence cs = clip.getItemAt(0).coerceToText(act);
                if (cs != null) text = cs.toString();
            }
        } catch (Throwable e) {
            // ostaje null
        }
        List<Themes.Theme> in = Themes.parseImport(text);
        if (in == null) {
            toast("Клипборд не садржи теме");
            return;
        }
        java.util.Set<String> names = new java.util.HashSet<String>();
        for (Themes.Theme x : themes) names.add(x.name);
        long base = System.currentTimeMillis();
        int added = 0;
        int skipped = 0;
        for (Themes.Theme t : in) {
            boolean exists = false;
            for (Themes.Theme x : themes) {
                if (sameTheme(x, t)) {
                    exists = true;
                    break;
                }
            }
            if (exists) {
                skipped++;
                continue;
            }
            String name = t.name;
            int k = 2;
            while (names.contains(name)) name = t.name + " " + (k++);
            names.add(name);
            themes.add(t.copy("t" + (base + added), name));
            added++;
        }
        if (added == 0) {
            toast("Све копиране теме већ постоје");
            return;
        }
        saveThemes();
        build();
        toast("Учитано тема: " + added + (skipped > 0 ? " (већ постоје: " + skipped + ")" : ""));
    }

    private void addTheme() {
        promptText("Назив нове теме:", "", "Направи", new TextCb() {
            @Override
            public void onText(String name) {
                Themes.Theme src = Themes.find(themes, selId);
                if (Themes.isOrig(src.id)) src = Themes.find(themes, null);   // "Православац" nema boja za kopiranje
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
        showIconMenu("Нова насумична тема",
                new int[] { Icons.SUN, Icons.MOON, Icons.DROP, Icons.DROP_STAR },
                new String[] { "Светла", "Тамна", "Од моје боје: позадина",
                        "Од моје боје: позадина и боја празника" },
                new Runnable[] {
                        new Runnable() {
                            @Override
                            public void run() {
                                addRandomTheme(RandomTheme.LIGHT, null, null, false);
                            }
                        },
                        new Runnable() {
                            @Override
                            public void run() {
                                addRandomTheme(RandomTheme.DARK, null, null, false);
                            }
                        },
                        new Runnable() {
                            @Override
                            public void run() {
                                pickBaseColor(false);
                            }
                        },
                        new Runnable() {
                            @Override
                            public void run() {
                                pickBaseColor(true);
                            }
                        } }, true);
    }

    private static final String KEY_LAST_HOLIDAY = "last_holiday_color";

    /**
     * Bira se boja pozadine (svijetla/tamna tema se odredjuje po njenoj svjetlini); sa holidays = true bira se
     * zatim i boja praznika (naslovi i praznici, svjetlina se podesava samo radi kontrasta). Ostalo je nasumicno.
     * Pocetne boje u biracu su posljednje koje je korisnik unio.
     */
    private void pickBaseColor(final boolean holidays) {
        ColorPicker.show(act, sp.getInt(KEY_LAST_BASE, Dark.BG), bg(), text(), "Боја позадине",
                new ColorPicker.Listener() {
                    @Override
                    public void onColor(final int base) {
                        sp.edit().putInt(KEY_LAST_BASE, base).commit();
                        if (!holidays) {
                            addRandomTheme(RandomTheme.ANY, Integer.valueOf(base), null, false);
                            return;
                        }
                        ColorPicker.show(act, sp.getInt(KEY_LAST_HOLIDAY, Dark.HOLIDAY), bg(), text(),
                                "Боја празника", new ColorPicker.Listener() {
                                    @Override
                                    public void onColor(int holiday) {
                                        sp.edit().putInt(KEY_LAST_HOLIDAY, holiday).commit();
                                        addRandomTheme(RandomTheme.ANY, Integer.valueOf(base),
                                                Integer.valueOf(holiday), false);
                                    }
                                });
                    }
                });
    }

    /** Tema koju je napravio generator i nije preimenovana (naziv "Pridjev imenica"). */
    private boolean isGeneratedTheme(Themes.Theme t) {
        return NameGen.parse(t.name) >= 0;
    }

    /**
     * Pravi nasumicnu temu (kontrast je zagarantovan) i odmah je bira. replaceCurrent = true mijenja izabranu
     * generisanu temu novom (bez pravljenja nove u listi), a false uvijek dodaje novu.
     */
    private void addRandomTheme(int mode, Integer base, Integer holidayColor, boolean replaceCurrent) {
        RandomTheme.Result res = RandomTheme.generate(new java.util.Random(), mode, base, holidayColor);
        if (!replaceCurrent) {
            if (res.adjusted && res.adjustedHoliday) toast("Боје су прилагођене ради читљивости");
            else if (res.adjusted) toast("Боја позадине је прилагођена ради читљивости");
            else if (res.adjustedHoliday) toast("Боја празника је прилагођена ради читљивости");
        }
        Themes.Theme cur = Themes.find(themes, selId);
        int gender = base == null ? NameGen.MASC : (holidayColor == null ? NameGen.FEM : NameGen.NEUT);
        java.util.Set<String> taken = new java.util.HashSet<String>();
        for (Themes.Theme x : themes) {
            if (!(replaceCurrent && x == cur)) taken.add(x.name);
        }
        String name = taken.contains(res.name) ? NameGen.unique(gender, new java.util.Random(), taken, res.name) : res.name;
        Themes.Theme n = new Themes.Theme("t" + System.currentTimeMillis(), name);
        n.colors.putAll(res.colors);
        for (String[] c : Colors.HIDDEN) {
            if (!n.colors.containsKey(c[0])) n.colors.put(c[0], Colors.def(c[0]));
        }
        int at = (replaceCurrent && isGeneratedTheme(cur)) ? themes.indexOf(cur) : -1;
        if (at >= 0) {
            clearUndo(themes.get(at));
            themes.set(at, n);
        } else {
            themes.add(n);
        }
        selId = n.id;
        saveThemes();
        build();
    }

    /**
     * Ponovo generise izabranu generisanu temu (bez nove teme). Rod u nazivu govori kako je nastala:
     * muski = nasumicna svijetla/tamna (po svjetlini pozadine), zenski = iz boje pozadine,
     * srednji = iz boje pozadine i boje praznika (obje se uzimaju iz same teme).
     */
    private void regenerateRandomTheme() {
        Themes.Theme cur = Themes.find(themes, selId);
        int gender = NameGen.parse(cur.name);
        if (gender < 0) {
            toast("Изабрана тема није насумична (или је преименована)");
            return;
        }
        int bgColor = Colors.parse(cur.color("c_bg")).intValue();
        if (gender == NameGen.MASC) {
            int mode = RandomTheme.rgbToHsl(bgColor)[2] >= 0.5f ? RandomTheme.LIGHT : RandomTheme.DARK;
            addRandomTheme(mode, null, null, true);
            return;
        }
        Integer holiday = gender == NameGen.NEUT ? Integer.valueOf(Colors.parse(cur.color("c_holiday")).intValue()) : null;
        addRandomTheme(RandomTheme.ANY, Integer.valueOf(bgColor), holiday, true);
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
        int editable = 0;
        for (Themes.Theme x : themes) if (!Themes.isOrig(x.id)) editable++;
        if (editable <= 1) {
            toast("Мора остати бар једна тема");
            return;
        }
        showStyled(builder()
                .setTitle("Обриши тему")
                .setMessage("Обрисати тему „" + t.name + "“?")
                .setPositiveButton("Обриши", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        themes.remove(t);
                        clearUndo(t);
                        if (selId.equals(t.id)) selId = Themes.find(themes, null).id;
                        saveThemes();
                        build();
                    }
                })
                .setNegativeButton("Одустани", null)
                );
    }

    // ------------------------------------------------------------------ uredjivanje boja teme

    // ------------------------------------------------------------------ vracanje boja korak po korak

    private static final int UNDO_MAX = 300;

    private static String undoKey(Themes.Theme t) {
        return "undo_" + t.id;
    }

    /** Koraci vracanja teme: {kljuc boje, stara vrijednost}; zadnji je prvi na redu. Cuvaju se trajno. */
    private List<String[]> loadUndo(Themes.Theme t) {
        List<String[]> out = new java.util.ArrayList<String[]>();
        String json = sp.getString(undoKey(t), null);
        if (json == null) return out;
        try {
            org.json.JSONArray a = new org.json.JSONArray(json);
            for (int i = 0; i < a.length(); i++) {
                org.json.JSONArray e = a.getJSONArray(i);
                out.add(new String[] { e.getString(0), e.getString(1) });
            }
        } catch (Throwable e) {
            out.clear();
        }
        return out;
    }

    private void saveUndo(Themes.Theme t, List<String[]> l) {
        SharedPreferences.Editor ed = sp.edit();
        if (l.isEmpty()) {
            ed.remove(undoKey(t));
        } else {
            org.json.JSONArray a = new org.json.JSONArray();
            for (String[] e : l) {
                org.json.JSONArray p = new org.json.JSONArray();
                p.put(e[0]);
                p.put(e[1]);
                a.put(p);
            }
            ed.putString(undoKey(t), a.toString());
        }
        ed.apply();
    }

    private void clearUndo(Themes.Theme t) {
        sp.edit().remove(undoKey(t)).apply();
    }

    /** Pamti staru vrijednost boje prije izmjene; uzastopne izmjene iste boje se spajaju u jedan korak. */
    private void pushUndo(Themes.Theme t, String key, String oldHex) {
        if (key.equals(lastUndoKey)) return;
        lastUndoKey = key;
        List<String[]> l = loadUndo(t);
        l.add(new String[] { key, oldHex });
        while (l.size() > UNDO_MAX) l.remove(0);
        saveUndo(t, l);
        if (undoRefresh != null) undoRefresh.run();
    }

    /** Vraca jednu boju unazad; false ako nema sta da se vrati. */
    private boolean undoColor(Themes.Theme t) {
        List<String[]> l = loadUndo(t);
        if (l.isEmpty()) return false;
        String[] e = l.remove(l.size() - 1);
        t.colors.put(e[0], e[1]);
        saveUndo(t, l);
        lastUndoKey = null;
        saveThemes();
        return true;
    }

    private void editColors(final Themes.Theme t) {
        lastUndoKey = null;
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
                .setNeutralButton(" ", null) // tekst se ispod zamjenjuje ikonicom (prazan tekst bi sakrio dugme)
                .create();
        // pozadina editora boja je c_bg, a slova su c_text (naslov i dugmad se boje izricito)
        d.getWindow().getDecorView().setTag(Dark.TAG_SETTINGS, Boolean.TRUE);
        d.getWindow().setBackgroundDrawable(windowBg(dp(8)));
        d.show();
        int tid = act.getResources().getIdentifier("alertTitle", "id", "android");
        View title = tid != 0 ? d.findViewById(tid) : null;
        if (title instanceof TextView) ((TextView) title).setTextColor(text());
        d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(text());
        // dugme "vrati": ikonica, vraca jednu boju unazad (do boja pri pravljenju teme ili prvom uredjivanju)
        final Button undoBtn = d.getButton(AlertDialog.BUTTON_NEUTRAL);
        undoBtn.setText("");
        undoBtn.setCompoundDrawablesWithIntrinsicBounds(Icons.get(Icons.UNDO, text(), dp(22)), null, null, null);
        undoBtn.setContentDescription("Врати");
        undoRefresh = new Runnable() {
            @Override
            public void run() {
                boolean has = !loadUndo(t).isEmpty();
                undoBtn.setEnabled(has);
                undoBtn.setAlpha(has ? 1f : 0.35f);
            }
        };
        undoRefresh.run();
        // "Vrati" ne zatvara dijalog
        undoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (undoColor(t)) fillRows(rows, t);
                undoRefresh.run();
            }
        });
        undoBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                toast("Врати једну боју уназад");
                return true;
            }
        });
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dlg) {
                undoRefresh = null;
                lastUndoKey = null;
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
                h.setTextColor(setLight() ? 0xff1a56b0 : 0xff8ab4f8);
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
        tv.setTag(Dark.TAG_SETTINGS, Boolean.TRUE);   // prije setText (vidi label()): inace "Постови" dobije boju postova
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
                    String old = t.color(key);
                    String now = Colors.hex(c.intValue());
                    if (!now.equals(old)) pushUndo(t, key, old);
                    t.colors.put(key, now);
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

        showStyled(builder()
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
                );
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