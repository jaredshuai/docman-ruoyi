from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import parse_qs, urlparse
import argparse
import html


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed = urlparse(self.path)
        params = parse_qs(parsed.query)
        src = html.escape(params.get("src", [""])[0])
        mode = html.escape(params.get("mode", [""])[0])
        body = f"""<!doctype html>
<html lang="en">
  <head><meta charset="utf-8"><title>Viewer Placeholder</title></head>
  <body>
    <h1>Viewer Placeholder</h1>
    <div id="mode">{mode}</div>
    <div id="src">{src}</div>
  </body>
</html>""".encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format, *args):
        return


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8012)
    args = parser.parse_args()
    HTTPServer(("127.0.0.1", args.port), Handler).serve_forever()


if __name__ == "__main__":
    main()
