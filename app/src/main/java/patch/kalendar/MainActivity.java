package patch.kalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Map;

/** Ekran sa podešavanjima modula (otvara se iz launchera). */
public class MainActivity extends Activity {

    private static final String TARGET = "com.pravoslavac";

    private static final int BG = 0xff121212;      // pozadina ekrana modula
    private static final int DIM = 0xffaaaaaa;     // sekundarni tekst

    private SharedPreferences sp;
    private boolean worldReadable = true;

    private List<Themes.Theme> themes;
    private String selId;
    private LinearLayout themeBox;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Pravoslavac kalendar patch");

        try {
            // LSPosed pretvara MODE_WORLD_READABLE u citljiv fajl za hook u drugom procesu.
            sp = getSharedPreferences(Prefs.FILE, MODE_WORLD_READABLE);
        } catch (SecurityException e) {
            worldReadable = false;
            sp = getSharedPreferences(Prefs.FILE, MODE_PRIVATE);
        }
        loadThemes();

        int pad = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(BG);

        if (!worldReadable) {
            TextView warn = new TextView(this);
            warn.setText("Modul trenutno nije aktivan u LSPosed-u, pa se postavke ne mogu primijeniti na "
                    + "aplikaciju. Aktiviraj modul u LSPosed-u (scope: Pravoslavac) i ponovo otvori ovaj ekran.");
            warn.setTextColor(0xffff6b6b);
            warn.setPadding(0, 0, 0, pad);
            root.addView(warn);
        }

        root.addView(new View(this), new LinearLayout.LayoutParams(1, dp(80)));

        root.addView(makeSwitch("Preskoči uvodni ekran", "Aplikacija se otvara bez uvodnog ekrana.",
                Prefs.KEY_SKIP_SPLASH, true));
        root.addView(makeSwitch("Tekst poravnat lijevo", "Umjesto obostranog poravnanja.",
                Prefs.KEY_LEFT, true));
        root.addView(makeSwitch("Aplikacija otvara kalendar", "Otvara se kalendar umjesto žitija za taj dan.",
                Prefs.KEY_OPEN_CALENDAR, true));

        addThemeSection(root);

        TextView hint = new TextView(this);
        hint.setText("Promjene se primjenjuju pri sljedećem pokretanju aplikacije Pravoslavac. "
                + "Ako je već otvorena, prvo je zaustavi (Force stop).");
        hint.setTextColor(DIM);
        hint.setPadding(0, pad, 0, pad);
        root.addView(hint);

        Button open = new Button(this);
        open.setText("Otvori Pravoslavac");
        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = getPackageManager().getLaunchIntentForPackage(TARGET);
                if (i != null) {
                    startActivity(i);
                } else {
                    toast("Pravoslavac nije instaliran");
                }
            }
        });
        root.addView(open);

        Button stop = new Button(this);
        stop.setText("Zaustavi Pravoslavac (Force stop)");
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + TARGET));
                    startActivity(i);
                } catch (Throwable t) {
                    toast("Ne mogu otvoriti podešavanja aplikacije");
                }
            }
        });
        root.addView(stop);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        sv.setFillViewport(true);
        sv.addView(root);
        setContentView(sv);
    }

    private View makeSwitch(String title, String desc, final String key, boolean def) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(8), 0, dp(8));

        Switch sw = new Switch(this);
        sw.setText(title);
        sw.setTextSize(18f);
        sw.setChecked(sp.getBoolean(key, def));
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                sp.edit().putBoolean(key, checked).apply();
            }
        });
        box.addView(sw);

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextSize(14f);
        d.setTextColor(DIM);
        box.addView(d);
        return box;
    }

    // ------------------------------------------------------------------ teme

    private void loadThemes() {
        String json = sp.getString(Themes.KEY_THEMES, null);
        themes = Themes.parse(json);
        selId = Themes.find(themes, sp.getString(Themes.KEY_SEL, null)).id;
        if (json == null) saveThemes(); // prvo pokretanje: upisi ugradjene teme
    }

    private void saveThemes() {
        sp.edit()
                .putString(Themes.KEY_THEMES, Themes.toJson(themes))
                .putString(Themes.KEY_SEL, selId)
                .apply();
    }

    private void addThemeSection(LinearLayout root) {
        root.addView(makeSwitch("Teme",
                "Uključeno: tamna tema u bojama odabrane teme. Isključeno: originalne boje aplikacije.",
                Prefs.KEY_DARK, true));

        TextView h = new TextView(this);
        h.setText("Odaberi temu (⋮ = uredi boje, preimenuj, obriši)");
        h.setTextSize(14f);
        h.setTextColor(DIM);
        h.setPadding(0, dp(8), 0, dp(4));
        root.addView(h);

        themeBox = new LinearLayout(this);
        themeBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(themeBox);
        refreshThemes();

        Button add = new Button(this);
        add.setText("+ Nova tema");
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTheme();
            }
        });
        root.addView(add);
    }

    private void refreshThemes() {
        themeBox.removeAllViews();
        for (final Themes.Theme t : themes) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            RadioButton rb = new RadioButton(this);
            rb.setText(t.name);
            rb.setTextSize(16f);
            rb.setChecked(t.id.equals(selId));
            rb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selId = t.id;
                    saveThemes();
                    refreshThemes();
                }
            });
            row.addView(rb, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            // mali pregled teme: pozadina, traka, naslov, link
            String[] keys = { "c_bg", "c_bar", "c_title", "c_web_link" };
            for (String k : keys) {
                View dot = new View(this);
                setSwatch(dot, Colors.parse(t.color(k)).intValue(), 1);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(16), dp(16));
                lp.setMargins(dp(2), 0, dp(2), 0);
                row.addView(dot, lp);
            }

            TextView more = new TextView(this);
            more.setText("\u22EE");
            more.setTextSize(24f);
            more.setPadding(dp(14), dp(8), dp(10), dp(8));
            more.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showThemeMenu(t);
                }
            });
            row.addView(more);
            themeBox.addView(row);
        }
    }

    private void showThemeMenu(final Themes.Theme t) {
        new AlertDialog.Builder(this)
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
                refreshThemes();
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
                refreshThemes();
            }
        });
    }

    private void deleteTheme(final Themes.Theme t) {
        if (themes.size() <= 1) {
            toast("Mora ostati bar jedna tema");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Obriši temu")
                .setMessage("Obrisati temu „" + t.name + "“?")
                .setPositiveButton("Obriši", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        themes.remove(t);
                        if (selId.equals(t.id)) selId = themes.get(0).id;
                        saveThemes();
                        refreshThemes();
                    }
                })
                .setNegativeButton("Odustani", null)
                .show();
    }

    // ------------------------------------------------------------------ uredjivanje boja teme

    private void editColors(final Themes.Theme t) {
        final LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(dp(16), dp(8), dp(16), 0);
        fillRows(rows, t);

        ScrollView s = new ScrollView(this);
        s.addView(rows);

        final AlertDialog d = new AlertDialog.Builder(this)
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
                refreshThemes();
            }
        });
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                refreshThemes();
            }
        });
    }

    private void fillRows(LinearLayout rows, Themes.Theme t) {
        rows.removeAllViews();
        for (String[] c : Colors.ALL) rows.addView(makeColorRow(t, c[0], c[1]));
    }

    private View makeColorRow(final Themes.Theme t, final String key, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(2), 0, dp(2));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(15f);
        row.addView(tv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final View swatch = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(36), dp(36));
        lp.setMargins(dp(8), 0, dp(8), 0);
        row.addView(swatch, lp);

        final EditText et = new EditText(this);
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
                ColorPicker.show(MainActivity.this, start, new ColorPicker.Listener() {
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
        final EditText et = new EditText(this);
        et.setSingleLine(true);
        et.setFilters(new InputFilter[] { new InputFilter.LengthFilter(30) });
        et.setText(initial);
        et.setSelection(et.getText().length());

        LinearLayout wrap = new LinearLayout(this);
        wrap.setPadding(dp(20), dp(8), dp(20), 0);
        wrap.addView(et, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
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
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
