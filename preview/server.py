#!/usr/bin/env python3
"""Lightweight web server that displays Roborazzi screenshots of the YaVa! Android app."""

import hashlib
import html
import json
import os
from http.server import HTTPServer, SimpleHTTPRequestHandler
from pathlib import Path
from urllib.parse import urlparse

REPO_ROOT = Path("/app")
SCREENSHOT_DIRS = [
    REPO_ROOT / "app" / "src" / "test" / "screenshots",
    REPO_ROOT / "app" / "build" / "outputs" / "roborazzi",
]
STATUS_FILE = Path("/tmp/build-status.txt")
LOG_FILE = Path("/tmp/build-log.txt")
METADATA_FILE = REPO_ROOT / "metadata.json"
APK_GLOB = "app/build/outputs/apk/debug/*.apk"
PORT = 3000


def load_metadata():
    if METADATA_FILE.exists():
        with open(METADATA_FILE) as f:
            return json.load(f)
    return {"name": "YaVa!", "description": "Android app preview"}


def load_status():
    if STATUS_FILE.exists():
        return STATUS_FILE.read_text().strip()
    return "building"


def load_log_tail(lines=80):
    if not LOG_FILE.exists():
        return ""
    with open(LOG_FILE, errors="replace") as f:
        all_lines = f.readlines()
    return "".join(all_lines[-lines:])


def find_screenshots():
    found = {}
    for d in SCREENSHOT_DIRS:
        if not d.exists():
            continue
        for p in sorted(d.rglob("*.png")):
            key = hashlib.md5(str(p).encode()).hexdigest()[:8]
            found[key] = p
    return found


def find_apks():
    import glob
    return sorted(glob.glob(str(REPO_ROOT / APK_GLOB)))


def build_index_html():
    meta = load_metadata()
    status = load_status()
    screenshots = find_screenshots()
    apks = find_apks()

    status_colors = {
        "building": "#f59e0b",
        "success": "#10b981",
        "failed": "#ef4444",
    }
    status_bg = status_colors.get(status, "#6b7280")
    status_label = status.upper()

    app_name = html.escape(meta.get("name", "YaVa!"))
    app_desc = html.escape(meta.get("description", ""))

    # Screenshot cards
    cards_html = ""
    if screenshots:
        for key, p in screenshots.items():
            name = html.escape(p.stem)
            cards_html += f"""
            <div class="screenshot-card">
              <a href="/screenshots/{key}" target="_blank">
                <img src="/screenshots/{key}" alt="{name}" loading="lazy"/>
              </a>
              <div class="screenshot-label">{name}</div>
            </div>"""
    else:
        cards_html = '<div class="empty">No screenshots yet — build is in progress.</div>'

    # APK links
    apk_html = ""
    for apk in apks:
        apk_name = html.escape(Path(apk).name)
        apk_key = hashlib.md5(apk.encode()).hexdigest()[:8]
        apk_html += f'<a class="apk-link" href="/apk/{apk_key}">📦 {apk_name}</a>'

    # Build log (only if failed)
    log_html = ""
    if status == "failed":
        log_text = html.escape(load_log_tail())
        if log_text:
            log_html = f'<details class="build-log"><summary>Build log (last 80 lines)</summary><pre>{log_text}</pre></details>'

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<meta http-equiv="refresh" content="10"/>
<title>{app_name} — Preview</title>
<style>
  * {{ margin: 0; padding: 0; box-sizing: border-box; }}
  body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
         background: #0f0f12; color: #f8f8fa; min-height: 100vh; }}
  .header {{ background: linear-gradient(135deg, #18181d, #1e1e26); padding: 32px 24px;
            border-bottom: 3px solid #FFCC00; }}
  .header h1 {{ font-size: 28px; font-weight: 900; }}
  .header h1 .dot {{ display: inline-block; width: 10px; height: 10px; border-radius: 50%;
                    background: #FFCC00; margin-left: 4px; }}
  .header p {{ color: #9e9eaa; margin-top: 6px; font-size: 14px; }}
  .status-bar {{ display: flex; align-items: center; gap: 12px; padding: 16px 24px;
                background: #18181d; border-bottom: 1px solid #2e2e3a; flex-wrap: wrap; }}
  .badge {{ display: inline-flex; align-items: center; gap: 6px; padding: 6px 14px;
           border-radius: 20px; font-size: 12px; font-weight: 700; color: #fff;
           background: {status_bg}; }}
  .badge::before {{ content: ''; width: 8px; height: 8px; border-radius: 50%; background: #fff; }}
  .info {{ font-size: 12px; color: #6e6e7a; }}
  .apk-link {{ display: inline-block; padding: 8px 16px; background: #FFCC00; color: #000;
              border-radius: 8px; text-decoration: none; font-size: 13px; font-weight: 700; }}
  .grid {{ display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
          gap: 20px; padding: 24px; }}
  .screenshot-card {{ background: #18181d; border-radius: 16px; overflow: hidden;
                     border: 1px solid #2e2e3a; transition: transform .2s; }}
  .screenshot-card:hover {{ transform: translateY(-2px); border-color: #FFCC00; }}
  .screenshot-card img {{ width: 100%; display: block; }}
  .screenshot-label {{ padding: 12px 16px; font-size: 13px; font-weight: 600; color: #9e9eaa; }}
  .empty {{ grid-column: 1/-1; text-align: center; padding: 60px; color: #6e6e7a; font-size: 16px; }}
  .build-log {{ margin: 0 24px 24px; border: 1px solid #2e2e3a; border-radius: 12px; overflow: hidden; }}
  .build-log summary {{ padding: 12px 16px; cursor: pointer; font-weight: 600; color: #ef4444;
                       background: #18181d; }}
  .build-log pre {{ padding: 16px; font-size: 12px; overflow-x: auto; max-height: 400px;
                   overflow-y: auto; background: #0f0f12; color: #9e9eaa; }}
  .footer {{ text-align: center; padding: 24px; color: #4e4e5a; font-size: 12px; }}
</style>
</head>
<body>
  <div class="header">
    <h1>{app_name}<span class="dot"></span></h1>
    <p>{app_desc}</p>
  </div>
  <div class="status-bar">
    <span class="badge">{status_label}</span>
    <span class="info">Roborazzi screenshot preview • auto-refreshes every 10s</span>
    {apk_html}
  </div>
  <div class="grid">
    {cards_html}
  </div>
  {log_html}
  <div class="footer">YaVa! Logistics — Native Android (Kotlin + Jetpack Compose) — Base44 Dev Preview</div>
</body>
</html>"""


class Handler(SimpleHTTPRequestHandler):
    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path

        if path == "/" or path == "":
            self.send_response(200)
            self.send_header("Content-Type", "text/html")
            body = build_index_html().encode()
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return

        if path.startswith("/screenshots/"):
            key = path.replace("/screenshots/", "")
            screenshots = find_screenshots()
            if key in screenshots:
                self._serve_file(screenshots[key], "image/png")
            else:
                self._not_found()
            return

        if path.startswith("/apk/"):
            key = path.replace("/apk/", "")
            import hashlib as h
            for apk in find_apks():
                if h.md5(apk.encode()).hexdigest()[:8] == key:
                    self._serve_file(Path(apk), "application/vnd.android.package-archive")
                    return
            self._not_found()
            return

        self._not_found()

    def _serve_file(self, filepath, mime):
        if not filepath.exists():
            self._not_found()
            return
        data = filepath.read_bytes()
        self.send_response(200)
        self.send_header("Content-Type", mime)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def _not_found(self):
        self.send_response(404)
        self.send_header("Content-Type", "text/plain")
        self.end_headers()
        self.wfile.write(b"Not found")

    def log_message(self, *args):
        pass


if __name__ == "__main__":
    server = HTTPServer(("0.0.0.0", PORT), Handler)
    print(f"Preview server running on port {PORT}")
    server.serve_forever()
