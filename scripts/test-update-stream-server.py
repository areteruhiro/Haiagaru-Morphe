"""Local-only fixture: a slow but continuously receiving body, plus a fast response."""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import time


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        slow = self.path == "/slow/subject.txt"
        data = b"x" * 100 if slow else b"x" * 4096 if self.path == "/burst/subject.txt" else b"ok"
        self.send_response(200)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        try:
            if slow:
                for byte in data:
                    self.wfile.write(bytes([byte]))
                    self.wfile.flush()
                    time.sleep(0.15)
            else:
                self.wfile.write(data)
        except (BrokenPipeError, ConnectionResetError):
            pass


if __name__ == "__main__":
    print("Local update fixture listening on 127.0.0.1:16881", flush=True)
    ThreadingHTTPServer(("127.0.0.1", 16881), Handler).serve_forever()
