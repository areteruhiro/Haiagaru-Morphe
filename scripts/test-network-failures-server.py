"""Loopback-only TLS/HTTP failure fixtures; never forwards requests elsewhere."""
import datetime
import ipaddress
from pathlib import Path
import socketserver
import ssl
import tempfile
import threading
import time
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID


class Handler(socketserver.BaseRequestHandler):
    def handle(self):
        sock = self.request
        sock.settimeout(5)
        try:
            if self.server.mode == "silent-tls":
                sock.recv(8192)  # accept ClientHello but never send ServerHello
                time.sleep(23)
                return
            if self.server.mode == "untrusted-tls":
                try:
                    with self.server.tls.wrap_socket(sock, server_side=True):
                        pass
                except (ssl.SSLError, OSError):
                    pass  # expected rejection by the unmodified app trust manager
                return
            request = sock.recv(8192)
            if b"/headers/" in request:
                time.sleep(23)
            elif b"/body/" in request:
                sock.sendall(b"HTTP/1.1 200 OK\r\nContent-Length: 100\r\nConnection: close\r\n\r\nx")
                time.sleep(23)
            else:
                sock.sendall(b"HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nok")
        except OSError:
            pass


class Server(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True


if __name__ == "__main__":
    with tempfile.TemporaryDirectory(prefix="haiagaru-tls-fixture-") as cert_dir:
        key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
        name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "localhost")])
        now = datetime.datetime.now(datetime.timezone.utc)
        cert = (x509.CertificateBuilder().subject_name(name).issuer_name(name)
                .public_key(key.public_key()).serial_number(x509.random_serial_number())
                .not_valid_before(now - datetime.timedelta(days=1))
                .not_valid_after(now + datetime.timedelta(days=2))
                .add_extension(x509.SubjectAlternativeName([
                    x509.IPAddress(ipaddress.ip_address("127.0.0.1")),
                    x509.DNSName("localhost")]), critical=False)
                .sign(key, hashes.SHA256()))
        cert_path = Path(cert_dir) / "cert.pem"
        key_path = Path(cert_dir) / "key.pem"
        cert_path.write_bytes(cert.public_bytes(serialization.Encoding.PEM))
        key_path.write_bytes(key.private_bytes(serialization.Encoding.PEM,
            serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
        tls = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        tls.load_cert_chain(cert_path, key_path)
        servers = []
        try:
            for port, mode in [(16882, "http"), (16883, "untrusted-tls"), (16884, "silent-tls")]:
                server = Server(("127.0.0.1", port), Handler)
                server.mode, server.tls = mode, tls
                servers.append(server)
                threading.Thread(target=server.serve_forever, daemon=True).start()
                print(f"READY {mode} 127.0.0.1:{port}", flush=True)
            threading.Event().wait()
        except KeyboardInterrupt:
            pass
        finally:
            for server in servers:
                server.shutdown()
                server.server_close()
