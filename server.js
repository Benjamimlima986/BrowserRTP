const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const { execFile } = require('node:child_process');

const root = __dirname;
const port = Number(process.env.PORT || 3001);
const services = {
  chrome: { service: 'chrome', browser: 'Chrome', vncPath: 'chrome' },
  firefox: { service: 'firefox', browser: 'Firefox', vncPath: 'firefox' },
  brave: { service: 'chrome', browser: 'Brave', vncPath: 'chrome' }
};
const contentTypes = { '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css', '.json': 'application/json' };

function sendJson(response, status, body) {
  response.writeHead(status, { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' });
  response.end(JSON.stringify(body));
}

function startService(browser, name, response) {
  const target = services[browser];
  if (!target) return sendJson(response, 400, { error: 'Unsupported browser' });
  const safeName = String(name || `${target.browser} session`).replace(/[^a-zA-Z0-9 _/-]/g, '').slice(0, 60);
  execFile('docker', ['compose', 'up', '-d', target.service], { cwd: root, timeout: 120000 }, (error, stdout, stderr) => {
    if (error) {
      console.error(stderr || error.message);
      return sendJson(response, 502, { error: 'Could not start the browser container' });
    }
    sendJson(response, 200, {
      id: target.service,
      name: safeName,
      browser: target.browser,
      status: 'running',
      vncUrl: `/vnc/${target.vncPath}/vnc.html?autoconnect=1&resize=remote&path=websockify`
    });
  });
}

function serveFile(request, response) {
  const requested = request.url === '/' ? '/index.html' : request.url.split('?')[0];
  const filePath = path.normalize(path.join(root, requested));
  if (!filePath.startsWith(root) || !fs.existsSync(filePath) || !fs.statSync(filePath).isFile()) return sendJson(response, 404, { error: 'Not found' });
  const extension = path.extname(filePath);
  response.writeHead(200, { 'Content-Type': contentTypes[extension] || 'application/octet-stream' });
  fs.createReadStream(filePath).pipe(response);
}

const server = http.createServer((request, response) => {
  if (request.method === 'GET' && request.url.startsWith('/api/health')) return sendJson(response, 200, { ok: true, docker: true });
  if (request.method === 'POST' && request.url === '/api/machines') {
    let data = '';
    request.on('data', (chunk) => { data += chunk; });
    request.on('end', () => {
      try {
        const body = JSON.parse(data || '{}');
        startService(String(body.browser || '').toLowerCase(), body.name, response);
      } catch { sendJson(response, 400, { error: 'Invalid JSON' }); }
    });
    return;
  }
  serveFile(request, response);
});

server.listen(port, () => console.log(`BrowserRTP running at http://localhost:${port}`));
