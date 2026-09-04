package com.browserrtp.displaymanager;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(9, 14, 19);
    private static final int PANEL = Color.rgb(17, 25, 32);
    private static final int PANEL_ALT = Color.rgb(25, 36, 44);
    private static final int TEXT = Color.rgb(232, 239, 242);
    private static final int MUTED = Color.rgb(137, 157, 163);
    private static final int GREEN = Color.rgb(111, 229, 145);

    private final List<TerminalSession> sessions = new ArrayList<>();
    private LinearLayout tabBar;
    private LinearLayout terminalStack;
    private LinearLayout terminalControls;
    private TextView terminalOutput;
    private TextView bootOutput;
    private EditText commandInput;
    private TextView runtimeState;
    private Button runtimeButton;
    private KaliRuntime kaliRuntime;
    private boolean kaliStarted;
    private int selectedSession;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        kaliRuntime = new KaliRuntime(this);
        sessions.add(new TerminalSession("kali-1"));
        buildScreen();
        selectSession(0);
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(12), dp(12), dp(8));
        TextView brand = text("RTP / KALI", 14, GREEN);
        brand.setTypeface(null, 1);
        header.addView(brand, weighted(1));
        runtimeState = text(kaliRuntime.isInstalled() ? "KALI READY" : "KALI NOT INSTALLED", 10, MUTED);
        header.addView(runtimeState, params(WRAP, dp(36)));
        runtimeButton = button(kaliRuntime.isInstalled() ? "RUN" : "INSTALL", BG, GREEN);
        runtimeButton.setOnClickListener(v -> {
            if (kaliRuntime.isInstalled()) startKali();
            else installKali();
        });
        header.addView(runtimeButton, params(dp(88), dp(40)));
        root.addView(header, params(MATCH, dp(54)));

        HorizontalScrollView tabsScroll = new HorizontalScrollView(this);
        tabsScroll.setHorizontalScrollBarEnabled(false);
        tabBar = new LinearLayout(this);
        tabBar.setPadding(dp(12), 0, dp(12), dp(5));
        tabsScroll.addView(tabBar);
        root.addView(tabsScroll, params(MATCH, dp(47)));

        terminalStack = new LinearLayout(this);
        terminalStack.setOrientation(LinearLayout.VERTICAL);
        bootOutput = text("NetHunter Standalone ainda nao foi iniciado.\nToque em INSTALL e depois em RUN.\n", 13, GREEN);
        bootOutput.setPadding(dp(14), dp(12), dp(14), dp(12));
        ScrollView bootScroll = new ScrollView(this);
        bootScroll.setBackgroundColor(Color.BLACK);
        bootScroll.addView(bootOutput);
        terminalStack.addView(bootScroll, params(MATCH, MATCH));
        root.addView(terminalStack, weighted(1));

        LinearLayout quickBar = new LinearLayout(this);
        quickBar.setPadding(dp(10), dp(6), dp(10), dp(6));
        addQuickCommand(quickBar, "apt update");
        addQuickCommand(quickBar, "apt install ");
        addQuickCommand(quickBar, "pkg install ");
        Button copy = button("COPY", TEXT, PANEL_ALT);
        copy.setOnClickListener(v -> copyText(terminalOutput.getText().toString()));
        quickBar.addView(copy, params(dp(72), dp(42)));
        root.addView(quickBar, params(MATCH, dp(54)));

        LinearLayout commandBar = new LinearLayout(this);
        commandBar.setPadding(dp(10), dp(5), dp(10), dp(10));
        commandInput = new EditText(this);
        commandInput.setSingleLine(true);
        commandInput.setHint("comando...");
        commandInput.setHintTextColor(MUTED);
        commandInput.setTextColor(TEXT);
        commandInput.setTextSize(14);
        commandInput.setInputType(InputType.TYPE_CLASS_TEXT);
        commandInput.setBackgroundColor(PANEL);
        commandInput.setPadding(dp(12), 0, dp(12), 0);
        commandInput.setOnEditorActionListener((v, action, event) -> { runCommand(); return true; });
        commandBar.addView(commandInput, weighted(1));
        Button send = button("RUN", BG, GREEN);
        send.setOnClickListener(v -> runCommand());
        commandBar.addView(send, params(dp(78), dp(50)));
        root.addView(commandBar, params(MATCH, dp(66)));
        terminalControls = quickBar;
        terminalControls.setVisibility(LinearLayout.GONE);
        commandBar.setVisibility(LinearLayout.GONE);

        setContentView(root);
    }

    private void selectSession(int index) {
        selectedSession = index;
        TerminalSession session = sessions.get(index);
        terminalOutput = session.output;
        if (kaliStarted) renderTerminal(session);
        updateTabs();
    }

    private void renderTerminal(TerminalSession session) {
        terminalStack.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK);
        TextView output = session.output;
        output.setPadding(dp(14), dp(12), dp(14), dp(12));
        output.setTextIsSelectable(true);
        scroll.addView(output);
        terminalStack.addView(scroll, params(MATCH, MATCH));
    }

    private void updateTabs() {
        tabBar.removeAllViews();
        for (int i = 0; i < sessions.size(); i++) {
            final int index = i;
            TerminalSession session = sessions.get(i);
            Button tab = button(session.name + (i == selectedSession ? "  x" : ""), TEXT, i == selectedSession ? PANEL_ALT : BG);
            tab.setAllCaps(false);
            tab.setOnClickListener(v -> {
                if (index == selectedSession && sessions.size() > 1) {
                    session.close();
                    sessions.remove(index);
                    selectSession(Math.max(0, index - 1));
                } else {
                    selectSession(index);
                }
            });
            tabBar.addView(tab, params(dp(116), dp(40)));
        }
        Button add = button("+ TAB", GREEN, BG);
        add.setOnClickListener(v -> {
            sessions.add(new TerminalSession("kali-" + (sessions.size() + 1)));
            selectSession(sessions.size() - 1);
        });
        tabBar.addView(add, params(dp(86), dp(40)));
    }

    private void runCommand() {
        String command = commandInput.getText().toString().trim();
        if (command.length() == 0) return;
        commandInput.setText("");
        TerminalSession session = sessions.get(selectedSession);
        session.append("\n$ " + command + "\n");
        session.run(command);
    }

    private void installKali() {
        runtimeButton.setEnabled(false);
        runtimeButton.setText("WAIT");
        runtimeState.setText("DOWNLOADING KALI");
        kaliRuntime.install(new KaliRuntime.Progress() {
            @Override public void update(String message, int percent) {
                runOnUiThread(() -> runtimeState.setText(message));
            }

            @Override public void complete(String message) {
                runOnUiThread(() -> {
                    runtimeState.setText("KALI READY");
                    runtimeButton.setEnabled(true);
                    runtimeButton.setText("RUN");
                    toast(message);
                });
            }

            @Override public void fail(String message) {
                runOnUiThread(() -> {
                    runtimeState.setText("KALI FAILED");
                    runtimeButton.setEnabled(true);
                    runtimeButton.setText("RETRY");
                    toast(message);
                });
            }
        });
    }

    private void startKali() {
        runtimeButton.setEnabled(false);
        runtimeButton.setText("BOOT");
        runtimeState.setText("STARTING NETHUNTER");
        bootOutput.setText("[RTP] Starting Kali NetHunter Standalone...\n");
        new Thread(() -> {
            try {
                Process boot = kaliRuntime.start(""
                        + "echo '[KALI] Preparing root filesystem'; "
                        + "echo '[KALI] Kernel: '$(uname -r); "
                        + "echo '[KALI] Identity: '$(id); "
                        + "echo '[KALI] Release:'; cat /etc/os-release; "
                        + "echo '[KALI] Checking shell: '$(command -v bash); "
                        + "echo '[KALI] NetHunter userspace ready'");
                BufferedReader reader = new BufferedReader(new InputStreamReader(boot.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) appendBoot(line + "\n");
                int exit = boot.waitFor();
                if (exit != 0) throw new IOException("Kali terminou com codigo " + exit);
                runOnUiThread(() -> {
                    runtimeState.setText("KALI RUNNING");
                    runtimeButton.setEnabled(true);
                    runtimeButton.setText("RUNNING");
                    kaliStarted = true;
                    terminalControls.setVisibility(LinearLayout.VISIBLE);
                    terminalStack.removeAllViews();
                    selectSession(selectedSession);
                });
            } catch (Exception error) {
                appendBoot("[ERROR] " + error.getMessage() + "\n");
                runOnUiThread(() -> {
                    runtimeState.setText("KALI FAILED");
                    runtimeButton.setEnabled(true);
                    runtimeButton.setText("RUN");
                });
            }
        }).start();
    }

    private void appendBoot(String message) {
        runOnUiThread(() -> {
            bootOutput.append(message);
            bootOutput.invalidate();
        });
    }

    private void addQuickCommand(LinearLayout parent, String command) {
        Button quick = button(command, TEXT, PANEL_ALT);
        quick.setAllCaps(false);
        quick.setTextSize(10);
        quick.setOnClickListener(v -> { commandInput.setText(command); commandInput.requestFocus(); });
        parent.addView(quick, params(dp(92), dp(42)));
    }

    private void copyText(String content) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("RTP Kali Terminal", content));
        toast("Terminal copiado");
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private Button button(String label, int foreground, int background) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(foreground);
        button.setTextSize(11);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundColor(background);
        button.setPadding(dp(5), 0, dp(5), 0);
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

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private LinearLayout.LayoutParams params(int width, int height) { return new LinearLayout.LayoutParams(width, height); }
    private LinearLayout.LayoutParams weighted(float weight) { return new LinearLayout.LayoutParams(0, MATCH, weight); }
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final class TerminalSession {
        private final String name;
        private final TextView output = text("RTP Kali Terminal\nAndroid kernel session\nType a command below.\n", 13, GREEN);
        private Process process;

        private TerminalSession(String name) { this.name = name; }

        private void append(String value) {
            runOnUiThread(() -> { output.append(value); output.invalidate(); });
        }

        private void run(String command) {
            new Thread(() -> {
                try {
                        process = kaliRuntime.isInstalled()
                            ? kaliRuntime.start(command)
                            : new ProcessBuilder("/system/bin/sh", "-c", command)
                            .redirectErrorStream(true).start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) append(line + "\n");
                    int exit = process.waitFor();
                    append("[exit " + exit + "]\n");
                } catch (IOException error) {
                    append("Erro ao executar: " + error.getMessage() + "\n");
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    append("Processo interrompido\n");
                }
            }).start();
        }

        private void close() {
            if (process != null) process.destroy();
        }
    }
}
