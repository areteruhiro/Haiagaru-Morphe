"""Loopback-only native image cache fixture; never fetches any external URL."""
import base64
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

PNG = base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=')
counts = {}

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/stats':
            data = json.dumps(counts).encode()
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
        elif self.path.split('?')[0] == '/patrolfixture/subject.txt':
            counts[self.path] = counts.get(self.path, 0) + 1
            data = '1792176000.dat<>巡回通しテスト (2)\n'.encode('cp932')
            self.send_response(200)
            self.send_header('Content-Type', 'text/plain; charset=Shift_JIS')
        elif self.path.split('?')[0] == '/patrolfixture/dat/1792176000.dat':
            counts[self.path] = counts.get(self.path, 0) + 1
            body = ('テスト<>sage<>2026/10/17(土) 01:00:00 ID:fixture<>http://127.0.0.1:18769/patrol-e2e.png<>巡回通しテスト\n'
                    'テスト<>sage<>2026/10/17(土) 01:01:00 ID:fixture<>追加レス<>\n')
            data = body.encode('cp932')
            self.send_response(200)
            self.send_header('Content-Type', 'text/plain; charset=Shift_JIS')
        elif self.path.endswith('.png'):
            counts[self.path] = counts.get(self.path, 0) + 1
            data = PNG
            self.send_response(200)
            self.send_header('Content-Type', 'image/png')
        else:
            data = b'not found'
            self.send_response(404)
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)
    def log_message(self, *_):
        pass

if __name__ == '__main__':
    ThreadingHTTPServer(('127.0.0.1', 18769), Handler).serve_forever()
