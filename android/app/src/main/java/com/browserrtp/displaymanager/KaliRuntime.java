package com.browserrtp.displaymanager;

import android.content.Context;
import android.os.Build;

import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

final class KaliRuntime {
    interface Progress {
        void update(String message, int percent);
        void complete(String message);
        void fail(String message);
    }

    private static final String TERMUX_BASE = "https://packages.termux.dev/apt/termux-main/";
    private static final String KALI_ROOTFS = "https://kali.download/nethunter-images/current/rootfs/kali-nethunter-rootfs-minimal-arm64.tar.xz";
    private final Context context;
    private final File runtimeDir;
    private final File rootfsDir;

    KaliRuntime(Context context) {
        this.context = context.getApplicationContext();
        runtimeDir = new File(context.getFilesDir(), "termux-runtime");
        rootfsDir = new File(context.getFilesDir(), "kali-rootfs");
    }

    boolean isInstalled() {
        return new File(runtimeDir, "data/data/com.termux/files/usr/bin/proot").canExecute()
                && new File(rootfsDir, "bin/bash").exists();
    }

    void install(Progress progress) {
        new Thread(() -> {
            try {
                if (!isArm64()) throw new IOException("Esta primeira imagem standalone suporta ARM64.");
                runtimeDir.mkdirs();
                rootfsDir.mkdirs();
                progress.update("Baixando runtime proot ARM64...", 5);
                Map<String, String> packages = new HashMap<>();
                packages.put("proot", "pool/main/p/proot/proot_5.1.107.92_aarch64.deb");
                packages.put("libtalloc", "pool/main/libt/libtalloc/libtalloc_2.4.3_aarch64.deb");
                packages.put("libandroid-shmem", "pool/main/liba/libandroid-shmem/libandroid-shmem_0.7_aarch64.deb");
                for (Map.Entry<String, String> item : packages.entrySet()) {
                    File deb = new File(context.getCacheDir(), item.getKey() + ".deb");
                    download(TERMUX_BASE + item.getValue(), deb, progress, 5, 20);
                    extractDeb(deb, runtimeDir);
                    deb.delete();
                }
                File archive = new File(context.getCacheDir(), "kali-rootfs.tar.xz");
                progress.update("Baixando rootfs Kali minimal (aprox. 131 MB)...", 25);
                download(KALI_ROOTFS, archive, progress, 25, 75);
                progress.update("Extraindo rootfs Kali...", 76);
                extractTarXz(archive, rootfsDir);
                archive.delete();
                File proot = new File(runtimeDir, "data/data/com.termux/files/usr/bin/proot");
                proot.setExecutable(true);
                new File(rootfsDir, "tmp").mkdirs();
                progress.complete("Kali standalone instalado");
            } catch (Exception error) {
                progress.fail(error.getMessage() == null ? "Falha ao instalar Kali" : error.getMessage());
            }
        }).start();
    }

    Process start(String command) throws IOException {
        if (!isInstalled()) throw new IOException("Instale o Kali standalone primeiro.");
        File proot = new File(runtimeDir, "data/data/com.termux/files/usr/bin/proot");
        ProcessBuilder builder = new ProcessBuilder(
                proot.getAbsolutePath(), "-0", "-r", rootfsDir.getAbsolutePath(),
                "-b", "/dev", "-b", "/proc", "/bin/bash", "-lc", command);
        Map<String, String> environment = builder.environment();
        environment.put("HOME", "/root");
        environment.put("USER", "root");
        environment.put("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin");
        environment.put("TERM", "xterm-256color");
        environment.put("LD_LIBRARY_PATH", new File(runtimeDir, "data/data/com.termux/files/usr/lib").getAbsolutePath());
        return builder.redirectErrorStream(true).start();
    }

    private void extractDeb(File deb, File destination) throws IOException {
        try (ArArchiveInputStream ar = new ArArchiveInputStream(new BufferedInputStream(new FileInputStream(deb)))) {
            ArArchiveEntry entry;
            while ((entry = ar.getNextArEntry()) != null) {
                if (entry.getName().startsWith("data.tar")) {
                    try (InputStream compressed = new XZCompressorInputStream(ar);
                         TarArchiveInputStream tar = new TarArchiveInputStream(compressed)) {
                        extractTar(tar, destination);
                    }
                    return;
                }
            }
        }
        throw new IOException("Pacote Debian sem data archive: " + deb.getName());
    }

    private void extractTarXz(File archive, File destination) throws IOException {
        try (InputStream input = new XZCompressorInputStream(new BufferedInputStream(new FileInputStream(archive)))) {
            extractTar(new TarArchiveInputStream(input), destination);
        }
    }

    private void extractTar(TarArchiveInputStream tar, File destination) throws IOException {
        TarArchiveEntry entry;
        byte[] buffer = new byte[16384];
        while ((entry = tar.getNextTarEntry()) != null) {
            File output = safeFile(destination, entry.getName());
            if (entry.isDirectory()) {
                output.mkdirs();
            } else if (entry.isFile()) {
                File parent = output.getParentFile();
                if (parent != null) parent.mkdirs();
                try (OutputStream file = new BufferedOutputStream(new FileOutputStream(output))) {
                    int count;
                    while ((count = tar.read(buffer)) != -1) file.write(buffer, 0, count);
                }
                output.setExecutable((entry.getMode() & 0100) != 0, false);
            }
        }
    }

    private File safeFile(File destination, String name) throws IOException {
        File output = new File(destination, name);
        String base = destination.getCanonicalPath() + File.separator;
        if (!output.getCanonicalPath().startsWith(base)) throw new IOException("Entrada de arquivo invalida");
        return output;
    }

    private void download(String address, File target, Progress progress, int start, int end) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(60000);
        connection.connect();
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) throw new IOException("Download HTTP " + connection.getResponseCode());
        int total = connection.getContentLength();
        int read = 0;
        byte[] buffer = new byte[32768];
        try (InputStream input = connection.getInputStream(); OutputStream output = new BufferedOutputStream(new FileOutputStream(target))) {
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
                read += count;
                int percent = total > 0 ? start + (read * (end - start) / total) : start;
                progress.update("Baixando... " + (read / 1024 / 1024) + " MB", Math.min(end, percent));
            }
        } finally {
            connection.disconnect();
        }
    }

    private boolean isArm64() {
        for (String abi : Build.SUPPORTED_ABIS) if ("arm64-v8a".equals(abi)) return true;
        return false;
    }
}
