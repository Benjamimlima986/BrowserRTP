FROM debian:bookworm-slim

ARG BROWSER=chromium
ENV DEBIAN_FRONTEND=noninteractive
ENV DISPLAY=:99

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
      ca-certificates \
      dbus-x11 \
      fluxbox \
      fonts-liberation \
      novnc \
      supervisor \
      websockify \
      wget \
      x11vnc \
      xvfb \
    && if [ "$BROWSER" = "firefox" ]; then apt-get install -y --no-install-recommends firefox-esr; else apt-get install -y --no-install-recommends chromium; fi \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

COPY start-browser.sh /usr/local/bin/start-browser
RUN chmod +x /usr/local/bin/start-browser

EXPOSE 5900 6080
ENTRYPOINT ["/usr/local/bin/start-browser"]
