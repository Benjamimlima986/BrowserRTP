# BrowserRTP containers

The Compose stack creates isolated Linux browser sessions with a real X display, VNC server, and noVNC web gateway.

## Start

```bash
docker compose up -d --build
```

Open the site in a browser:

- Dashboard: http://localhost:3000
- Chromium direct: http://localhost:3000/vnc/chrome/vnc.html?autoconnect=1&resize=remote
- Firefox direct: http://localhost:3000/vnc/firefox/vnc.html?autoconnect=1&resize=remote

Nginx is the public entry point on port `3000` and proxies the API on port `3001` and noVNC traffic. The VNC ports stay private inside the Compose network. These defaults are intended for local development; add authentication and TLS before exposing them publicly.

## Stop

```bash
docker compose down
```
