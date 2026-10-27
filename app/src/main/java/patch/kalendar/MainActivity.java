package patch.kalendar;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Ekran sa podešavanjima modula (otvara se iz launchera). */
public class MainActivity extends Activity {

    private static final String TARGET = "com.pravoslavac";

    private SharedPreferences sp;
    private boolean worldReadable = true;

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

        int pad = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        if (!worldReadable) {
            TextView warn = new TextView(this);
            warn.setText("Modul trenutno nije aktivan u LSPosed-u, pa se postavke ne mogu primijeniti na "
                    + "aplikaciju. Aktiviraj modul u LSPosed-u (scope: Pravoslavac) i ponovo otvori ovaj ekran.");
            warn.setTextColor(0xffcc0000);
            warn.setPadding(0, 0, 0, pad);
            root.addView(warn);
        }

        root.addView(new View(this), new LinearLayout.LayoutParams(1, dp(104)));

        root.addView(makeSwitch("Preskoči uvodni ekran", "Aplikacija se otvara bez uvodnog ekrana.",
                Prefs.KEY_SKIP_SPLASH));
        root.addView(makeSwitch("Tamna tema", "Tamne boje, liste, kalendar i sadržaj stranica.",
                Prefs.KEY_DARK));
        root.addView(makeSwitch("Tekst poravnat lijevo", "Umjesto obostranog poravnanja.",
                Prefs.KEY_LEFT));
        root.addView(makeSwitch("Aplikacija otvara kalendar", "Otvara se kalendar umjesto žitija za taj dan.",
                Prefs.KEY_OPEN_CALENDAR));

        TextView hint = new TextView(this);
        hint.setText("Promjene se primjenjuju pri sljedećem pokretanju aplikacije Pravoslavac. "
                + "Ako je već otvorena, prvo je zaustavi (Force stop).");
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
                    Toast.makeText(MainActivity.this, "Pravoslavac nije instaliran", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(MainActivity.this, "Ne mogu otvoriti podešavanja aplikacije",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        root.addView(stop);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }

    private View makeSwitch(String title, String desc, final String key) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(8), 0, dp(8));

        Switch sw = new Switch(this);
        sw.setText(title);
        sw.setTextSize(18f);
        sw.setChecked(sp.getBoolean(key, true));
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
        box.addView(d);
        return box;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}