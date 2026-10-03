const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const root = __dirname;
const types = {'.html':'text/html; charset=utf-8','.css':'text/css; charset=utf-8','.js':'text/javascript; charset=utf-8','.svg':'image/svg+xml'};
const server = http.createServer((request,response) => {
  let requested;
  try { requested = decodeURIComponent(new URL(request.url,'http://localhost').pathname); }
  catch (_) { response.writeHead(400); response.end(); return; }
  const file = path.resolve(root, '.' + (requested === '/' ? '/index.html' : requested));
  if (!file.startsWith(root + path.sep) || !types[path.extname(file)]) { response.writeHead(404); response.end(); return; }
  fs.readFile(file,(error,data)=>{ if(error){response.writeHead(404);response.end();return;}response.writeHead(200,{'Content-Type':types[path.extname(file)],'Cache-Control':'no-store'});response.end(data); });
});
server.listen(4173,'127.0.0.1',()=>process.stdout.write('Waterminder preview: http://127.0.0.1:4173\n'));
