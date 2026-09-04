package com.browserrtp.displaymanager;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(11, 16, 23);
    private static final int PANEL = Color.rgb(21, 28, 38);
    private static final int PANEL_ALT = Color.rgb(28, 38, 49);
    private static final int TEXT = Color.rgb(233, 237, 245);
    private static final int MUTED = Color.rgb(145, 157, 175);
    private static final int LIME = Color.rgb(199, 243, 107);
    private static final int LINE = Color.rgb(54, 66, 81);

    private EditText widthInput;
    private EditText heightInput;
    private EditText dpiInput;
    private TextView commandView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildScreen();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(18), dp(20), dp(28));
        scroll.addView(page);

        TextView eyebrow = text("DISPLAY CONTROL  /  RTP", 11, LIME);
        eyebrow.setTypeface(null, 1);
        page.addView(eyebrow, params(WRAP, dp(30)));

        TextView title = text("RTP Display Manager", 28, TEXT);
        title.setTypeface(null, 1);
        page.addView(title, params(WRAP, dp(46)));
        page.addView(text("Ajuste a resolucao e a densidade do seu Android.", 14, MUTED), params(WRAP, dp(34)));

        LinearLayout statusCard = card();
        statusCard.addView(text("TELA ATUAL", 10, MUTED), params(WRAP, dp(24)));
        statusCard.addView(text(getDisplayInfo(), 18, TEXT), params(WRAP, dp(32)));
        statusCard.addView(text("O valor pode mudar conforme o fabricante e a barra de navegacao.", 11, MUTED), params(WRAP, dp(26)));
        page.addView(statusCard, params(MATCH, dp(106)));

        page.addView(text("PRESETS RAPIDOS", 11, MUTED), params(WRAP, dp(42)));
        LinearLayout presets = new LinearLayout(this);
        presets.setOrientation(LinearLayout.HORIZONTAL);
        addPreset(presets, "800 x 600", "480", 800, 600, 480);
        addPreset(presets, "720 x 1280", "420", 720, 1280, 420);
        addPreset(presets, "1080 x 1920", "440", 1080, 1920, 440);
        HorizontalScrollView presetScroll = new HorizontalScrollView(this);
        presetScroll.setHorizontalScrollBarEnabled(false);
        presetScroll.addView(presets);
        page.addView(presetScroll, params(MATCH, dp(58)));

        page.addView(text("DIMENSOES PERSONALIZADAS", 11, MUTED), params(WRAP, dp(42)));
        LinearLayout dimensions = new LinearLayout(this);
        dimensions.setOrientation(LinearLayout.HORIZONTAL);
        widthInput = input("Largura", "800");
        heightInput = input("Altura", "600");
        dpiInput = input("DPI", "480");
        dimensions.addView(widthInput, weightParams(1));
        dimensions.addView(space(dp(8), dp(1)));
        dimensions.addView(heightInput, weightParams(1));
        dimensions.addView(space(dp(8), dp(1)));
        dimensions.addView(dpiInput, weightParams(1));
        page.addView(dimensions, params(MATCH, dp(66)));

        Button apply = button("APLICAR CONFIGURACAO", LIME, BG);
        apply.setOnClickListener(v -> applyConfiguration());
        page.addView(apply, params(MATCH, dp(52)));

        Button restore = button("RESTAURAR CONFIGURACAO ORIGINAL", TEXT, BG);
        restore.setOnClickListener(v -> restoreConfiguration());
        page.addView(restore, params(MATCH, dp(52)));

        page.addView(text("COMANDO ADB", 11, MUTED), params(WRAP, dp(38)));
        commandView = text(getCommand(), 12, LIME);
        commandView.setPadding(dp(12), dp(10), dp(12), dp(10));
        commandView.setBackgroundColor(PANEL);
        page.addView(commandView, params(MATCH, dp(62)));

        Button copy = button("COPIAR COMANDO ADB", TEXT, PANEL_ALT);
        copy.setOnClickListener(v -> copyCommand());
        page.addView(copy, params(MATCH, dp(48)));

        LinearLayout note = card();
        note.addView(text("COMO FUNCIONA", 10, LIME), params(WRAP, dp(24)));
        note.addView(text("O Android bloqueia alteracoes de tela para apps comuns. Use o comando copiado em um computador com ADB autorizado, ou conceda WRITE_SECURE_SETTINGS via ADB. Em aparelhos com root, o botao Aplicar tenta executar automaticamente.", 12, MUTED), params(MATCH, dp(74)));
        page.addView(note, params(MATCH, dp(124)));

        Button settings = button("ABRIR CONFIGURACOES DE TELA", TEXT, PANEL_ALT);
        settings.setOnClickListener(v -> openDisplaySettings());
        page.addView(settings, params(MATCH, dp(48)));

        setContentView(scroll);
    }

    private void addPreset(LinearLayout parent, String label, String dpi, int width, int height, int density) {
        Button preset = button(label + "\n" + dpi + " DPI", TEXT, PANEL_ALT);
        preset.setTextSize(11);
        preset.setAllCaps(false);
        preset.setOnClickListener(v -> {
            widthInput.setText(String.valueOf(width));
            heightInput.setText(String.valueOf(height));
            dpiInput.setText(String.valueOf(density));
            updateCommand();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(122), dp(50));
        lp.setMargins(0, 0, dp(8), 0);
        parent.addView(preset, lp);
    }

    private void applyConfiguration() {
        int[] values = readValues();
        if (values == null) return;
        String command = "wm size " + values[0] + "x" + values[1] + " && wm density " + values[2];
        updateCommand();
        if (applyWithSecureSettings(values[0], values[1], values[2])) {
            toast("Configuracao aplicada");
        } else if (runAsRoot(command)) {
            toast("Configuracao aplicada com root");
        } else {
            copyText(command);
            toast("Sem root: comando copiado para o ADB");
        }
    }

    private void restoreConfiguration() {
        String command = "wm size reset && wm density reset";
        if (restoreWithSecureSettings()) {
            toast("Configuracao original restaurada");
        } else if (runAsRoot(command)) {
            toast("Configuracao original restaurada com root");
        } else {
            copyText(command);
            toast("Sem root: comando de restauracao copiado");
        }
    }

    private boolean applyWithSecureSettings(int width, int height, int density) {
        try {
            boolean sizeChanged = Settings.Global.putString(getContentResolver(), "display_size_forced", width + "," + height);
            boolean densityChanged = Settings.Global.putString(getContentResolver(), "display_density_forced", String.valueOf(density));
            return sizeChanged && densityChanged;
        } catch (SecurityException ignored) {
            return false;
        }
    }

    private boolean restoreWithSecureSettings() {
        try {
            boolean sizeReset = Settings.Global.putString(getContentResolver(), "display_size_forced", null);
            boolean densityReset = Settings.Global.putString(getContentResolver(), "display_density_forced", null);
            return sizeReset && densityReset;
        } catch (SecurityException ignored) {
            return false;
        }
    }

    private int[] readValues() {
        try {
            int width = Integer.parseInt(widthInput.getText().toString().trim());
            int height = Integer.parseInt(heightInput.getText().toString().trim());
            int dpi = Integer.parseInt(dpiInput.getText().toString().trim());
            if (width < 240 || width > 7680 || height < 240 || height > 7680 || dpi < 80 || dpi > 1000) throw new NumberFormatException();
            return new int[]{width, height, dpi};
        } catch (NumberFormatException error) {
            toast("Use dimensoes entre 240 e 7680 e DPI entre 80 e 1000");
            return null;
        }
    }

    private boolean runAsRoot(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void copyCommand() {
        copyText(getCommand());
        toast("Comando ADB copiado");
    }

    private void copyText(String command) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("RTP Display Manager", command));
    }

    private String getCommand() {
        String width = widthInput == null ? "800" : widthInput.getText().toString();
        String height = heightInput == null ? "600" : heightInput.getText().toString();
        String dpi = dpiInput == null ? "480" : dpiInput.getText().toString();
        return "adb shell wm size " + width + "x" + height + " && adb shell wm density " + dpi;
    }

    private void updateCommand() {
        if (commandView != null) commandView.setText(getCommand());
    }

    private String getDisplayInfo() {
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        return String.format(Locale.US, "%d x %d  /  %.0f DPI", metrics.widthPixels, metrics.heightPixels, metrics.density * 160f);
    }

    private void openDisplaySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS));
        } catch (Exception error) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.setBackgroundColor(PANEL);
        return box;
    }

    private EditText input(String hint, String value) {
        EditText field = new EditText(this);
        field.setText(value);
        field.setHint(hint);
        field.setHintTextColor(MUTED);
        field.setTextColor(TEXT);
        field.setTextSize(16);
        field.setSingleLine(true);
        field.setGravity(Gravity.CENTER);
        field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        field.setBackgroundColor(PANEL);
        field.setPadding(dp(4), 0, dp(4), 0);
        field.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) updateCommand(); });
        return field;
    }

    private Button button(String label, int foreground, int background) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(foreground);
        button.setTextSize(12);
        button.setGravity(Gravity.CENTER);
        button.setAllCaps(true);
        button.setBackgroundColor(background);
        button.setPadding(dp(8), 0, dp(8), 0);
        return button;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private View space(int width, int height) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(width, height));
        return view;
    }

    private LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private LinearLayout.LayoutParams weightParams(float weight) {
        return new LinearLayout.LayoutParams(0, MATCH, weight);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
}
