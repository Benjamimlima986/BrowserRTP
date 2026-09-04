# RTP Kali Terminal

Aplicativo Android com abas de terminal, execucao de comandos no shell Android e ponte opcional para o Termux.

## O que esta versao faz

- Cria varias abas de terminal independentes.
- Executa comandos no shell Android local.
- Envia comandos para o Termux instalado pelo F-Droid usando `RUN_COMMAND`.
- Oferece atalhos para `apt update`, `apt install` e `pkg install`.

O app usa o kernel Android. Para ter um ambiente Kali userspace, instale o Termux e configure o `proot-distro` dentro dele. Isso e o modelo Kali NetHunter Rootless; nao substitui o kernel Android.

## Configurar Kali no Termux

No Termux, execute:

```bash
pkg update
pkg install proot-distro git
proot-distro install debian
proot-distro login debian
apt update
apt install kali-archive-keyring
```

O botao `TERMUX` envia os comandos digitados para o Termux. A imagem Kali completa nao e embutida no APK porque teria centenas de megabytes e deve ser baixada para a arquitetura do aparelho.

O Termux e um projeto separado, licenciado sob GPLv3. O app usa a interface publica de comandos; uma distribuicao standalone baseada diretamente no source do Termux exigira incluir os avisos e o codigo correspondente da GPLv3.

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
