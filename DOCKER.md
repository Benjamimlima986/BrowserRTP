# BrowserRTP containers

The Compose stack creates isolated Linux browser sessions with a real X display, VNC server, and noVNC web gateway.

## Operating systems

- Ubuntu 24.04: `ubuntu-chrome` and `ubuntu-firefox` templates.
- Debian 12: `chrome` and `firefox` templates.
- Windows 10 and Windows 7 are shown in the UI but require a Windows Docker host with Windows containers enabled. They cannot run as Windows containers on this Linux Docker daemon.

## Start

```bash
docker compose up -d --build
```

Open the site in a browser:

- Dashboard: http://localhost:8080
- Chromium direct: http://localhost:8080/vnc/chrome/vnc.html?autoconnect=1&resize=remote
- Firefox direct: http://localhost:8080/vnc/firefox/vnc.html?autoconnect=1&resize=remote
- Ubuntu Chromium direct: http://localhost:8080/vnc/ubuntu/vnc.html?autoconnect=1&resize=remote

Nginx is the public entry point on port `8080` and proxies the API on port `3001` and noVNC traffic. The VNC ports stay private inside the Compose network. These defaults are intended for local development; add authentication and TLS before exposing them publicly.

## Stop

```bash
docker compose down
```
