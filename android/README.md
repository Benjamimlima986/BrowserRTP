# RTP Display Manager

Aplicativo Android para preparar e aplicar resolucoes e densidades personalizadas, com foco no preset 800x600.

## Build

1. Instale o Android SDK e defina `ANDROID_HOME`.
2. Instale a plataforma Android 35 e as build-tools correspondentes.
3. Na pasta `android`, rode `gradle assembleDebug`.
4. O APK sera gerado em `android/app/build/outputs/apk/debug/app-debug.apk`.

## Permissoes Android

Um APK comum nao pode alterar `wm size` ou `wm density` silenciosamente. O app tenta usar root com `su`; sem root, copie o comando exibido e rode com um computador conectado por ADB:

```bash
adb shell wm size 800x600
adb shell wm density 480
```

Para conceder a permissao especial em aparelhos compativeis:

```bash
adb shell pm grant com.browserrtp.displaymanager android.permission.WRITE_SECURE_SETTINGS
```

Para desfazer:

```bash
adb shell wm size reset
adb shell wm density reset
```
