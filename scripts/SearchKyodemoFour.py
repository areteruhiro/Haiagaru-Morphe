"""Bounded public-page search prototype. Results cover fetched pages, not the whole index.

Example: python SearchKyodemoFour.py Q8ld --half last --seed
https://www.kyodemo.net/sdemo/r/android/1792102288/ --max-pages 10
"""
import argparse
import html
from html.parser import HTMLParser
import json
from pathlib import Path
import re
import sqlite3
import time
from urllib.parse import urljoin, urlsplit
from urllib.request import Request, build_opener, HTTPRedirectHandler
from urllib.error import HTTPError, URLError
from urllib.robotparser import RobotFileParser

ORIGIN = "https://www.kyodemo.net"
AGENT = "HaiagaruSearchPrototype/0.1"
TOKEN = re.compile(r"(?<![A-Za-z0-9])([A-Za-z0-9]{4})-([^\s()（）<>]{4})(?=$|[\s()（）<>])")


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


class Page(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.links = []
        self.headers = []
        self.depth = 0
        self.parts = []

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == "a" and "href" in attrs:
            self.links.append(attrs["href"])
        if tag == "div":
            if self.depth:
                self.depth += 1
            elif "r-head" in attrs.get("class", "").split():
                self.depth = 1
                self.parts = []

    def handle_endtag(self, tag):
        if tag == "div" and self.depth:
            self.depth -= 1
            if not self.depth:
                self.headers.append("".join(self.parts))

    def handle_data(self, data):
        if self.depth:
            self.parts.append(data)


def canonical(url):
    uri = urlsplit(url)
    if uri.scheme != "https" or uri.netloc != "www.kyodemo.net" or uri.query:
        return None
    # Only public category, board and complete thread pages; never actions/searches.
    if not re.fullmatch(r"/sdemo/(?:|k/[A-Za-z0-9_-]+/?|b/[A-Za-z0-9_-]+/(?:\d+)?|r/[A-Za-z0-9_-]+/\d+/)", uri.path):
        return None
    return ORIGIN + uri.path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("four")
    parser.add_argument("--half", choices=["first", "last"], default="last")
    parser.add_argument("--seed", default=ORIGIN + "/sdemo/")
    parser.add_argument("--max-pages", type=int, default=20)
    parser.add_argument("--interval", type=float, default=15)
    parser.add_argument("--cache", type=Path, default=Path("build/verification/kyodemo-search.sqlite"))
    parser.add_argument("--output", type=Path, default=Path("build/verification/kyodemo-search-results.json"))
    args = parser.parse_args()
    if len(args.four) != 4 or any(c.isspace() for c in args.four):
        parser.error("Specify exactly four non-whitespace characters")
    if not 1 <= args.max_pages <= 100 or args.interval < 10:
        parser.error("max-pages must be 1..100 and interval at least 10 seconds")
    seed = canonical(args.seed)
    if not seed:
        parser.error("Seed must be a public Kyodemo category, board or thread URL without a query")
    args.cache.parent.mkdir(parents=True, exist_ok=True)
    db = sqlite3.connect(args.cache)
    db.execute("CREATE TABLE IF NOT EXISTS pages(url TEXT PRIMARY KEY, saved REAL, body BLOB)")
    last_request = 0.0
    network = 0

    def fetch(url, ttl):
        nonlocal last_request, network
        row = db.execute("SELECT saved,body FROM pages WHERE url=?", (url,)).fetchone()
        if row and time.time() - row[0] < ttl:
            return row[1]
        time.sleep(max(0, args.interval - (time.monotonic() - last_request)))
        last_request = time.monotonic()
        network += 1
        with build_opener(NoRedirect()).open(Request(url, headers={"User-Agent": AGENT}), timeout=20) as response:
            if response.geturl() != url:
                raise ValueError("Redirect encountered; stopped")
            body = response.read(2 * 1024 * 1024 + 1)
            if len(body) > 2 * 1024 * 1024:
                raise ValueError("Response exceeds 2 MiB")
        db.execute("INSERT OR REPLACE INTO pages VALUES(?,?,?)", (url, time.time(), body))
        db.commit()
        return body

    queue, seen, matches = [seed], set(), []
    status = "page-limit"
    try:
        robots = RobotFileParser()
        robots.parse(fetch(ORIGIN + "/robots.txt", 86400).decode("utf-8").splitlines())
        args.interval = max(args.interval, robots.crawl_delay(AGENT) or 0)
        while queue and len(seen) < args.max_pages:
            url = queue.pop(0)
            if url in seen:
                continue
            seen.add(url)
            if not robots.can_fetch(AGENT, url):
                continue
            page = Page()
            page.feed(fetch(url, 86400).decode("cp932", errors="replace"))
            for header in page.headers:
                for match in TOKEN.finditer(header):
                    if match.group(1 if args.half == "first" else 2) == args.four:
                        matches.append({"page": url, "header": header, "wacchoi": match.group(0)})
            for link in page.links:
                candidate = canonical(urljoin(url, html.unescape(link)))
                if candidate and candidate not in seen and candidate not in queue and len(queue) < 10000:
                    queue.append(candidate)
            print(f"pages={len(seen)} network={network} matches={len(matches)}", flush=True)
        if not queue:
            status = "discovered-links-exhausted"
    except KeyboardInterrupt:
        status = "cancelled"
    except (HTTPError, URLError, TimeoutError, ValueError, OSError) as error:
        status = f"stopped: {error}"
    finally:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps({"status": status, "complete_site_search": False,
            "pages_considered": len(seen), "network_requests": network,
            "remaining_discovered_pages": len(queue), "matches": matches}, ensure_ascii=False, indent=2), encoding="utf-8")
        db.close()
    print(f"{status}; saved {args.output}; coverage is limited to fetched pages")


if __name__ == "__main__":
    main()
