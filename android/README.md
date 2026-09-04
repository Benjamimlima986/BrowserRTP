# RTP Kali Terminal

Aplicativo Android standalone com abas de terminal e um userspace Kali executado sobre o kernel Android.

## O que esta versao faz

- Cria varias abas de terminal independentes.
- Executa comandos no shell Android local enquanto o Kali ainda nao foi instalado.
- Baixa e extrai o runtime `proot` e o rootfs Kali minimal diretamente pelo APK.
- Executa `/bin/bash` dentro do rootfs Kali sem exigir o app Termux.
- Oferece atalhos para `apt update`, `apt install` e `pkg install`.

O app usa o kernel Android. O Kali e um userspace real executado com `proot`, no modelo Kali NetHunter Rootless; o APK nao substitui o kernel Android.

## Instalar Kali standalone

Toque em `INSTALL` no app. Em aparelhos ARM64, ele baixa os componentes oficiais do Termux necessarios para o `proot` e o rootfs Kali minimal oficial. O download inicial tem aproximadamente 131 MB e a extracao precisa de espaco adicional.

Depois da instalacao, o estado muda para `KALI READY` e cada aba executa comandos dentro do rootfs. Comandos como estes passam a funcionar dentro do Kali:

```bash
apt update
apt install nmap git python3
```

Esta versao inicial suporta o rootfs ARM64. A imagem completa do Kali nao e embutida no APK porque teria gigabytes e deve ser baixada para a arquitetura do aparelho.

## Fallback NetHunter

Em aparelhos compativeis, o NetHunter Rootless ou o NetHunter completo pode ser usado quando o modo standalone nao for adequado. O NetHunter completo exige root e, para recursos avancados de hardware, um kernel NetHunter compativel com o modelo do aparelho.

O Termux e um projeto separado, licenciado sob GPLv3. Este app reutiliza componentes distribuidos pelo repositorio de pacotes do Termux e deve manter os avisos e os termos da GPLv3 junto da distribuicao.

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
