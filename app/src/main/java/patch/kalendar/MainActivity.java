package patch.kalendar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Ekran modula iz launchera: podesavanja su sada unutar Pravoslavca (Meni, Опције, Подешавања). */
public class MainActivity extends Activity {

    private static final String TARGET = "com.pravoslavac";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Pravoslavac kalendar patch");

        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(0xff121212);

        TextView info = new TextView(this);
        info.setText("Podešavanja modula su unutar aplikacije Pravoslavac:\n"
                + "Meni → Опције → Подешавања.\n\n"
                + "Modul mora biti aktiviran u LSPosed-u (scope: Pravoslavac).");
        info.setTextSize(16f);
        info.setTextColor(0xffe0e0e0);
        info.setPadding(0, 0, 0, pad);
        root.addView(info);

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
        setContentView(root);
    }
}
