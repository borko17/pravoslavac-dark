package patch.kalendar;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Ekran modula (LSPosed -> podesavanja modula): samo kratko obavjestenje. Sva podesavanja i preuzimanje fontova
 * su u aplikaciji Православац (Мени -> Подешавања). Preuzimanje fontova radi FontProvider ovog modula u pozadini.
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Pravoslavac Dark");
        int pad = Math.round(24 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setBackgroundColor(0xff121212);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Pravoslavac Dark");
        title.setTextSize(22f);
        title.setTextColor(0xffe0e0e0);
        root.addView(title);

        TextView info = new TextView(this);
        info.setText("Ово је LSPosed модул. Овдје нема подешавања.\n\n"
                + "Тему, фонтове и остале опције бирате у апликацији Православац (или Житије светих): "
                + "Мени → Подешавања. Фонтови се преузимају у табу „Фонтови“ (интернет за то користи овај модул, "
                + "па га не уклањајте).");
        info.setTextSize(15f);
        info.setTextColor(0xff9aa0a6);
        info.setPadding(0, pad / 2, 0, 0);
        root.addView(info);

        setContentView(root);
    }
}
