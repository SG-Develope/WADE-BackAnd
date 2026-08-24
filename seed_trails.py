# -*- coding: utf-8 -*-
"""
산책로 코스 시드 스크립트
--------------------------------------------------
엑셀(코스목록정보 1.xlsx)을 읽어 271개 코스의 GPX를 두루누비에서 내려받아
좌표로 파싱한 뒤, 좌표(path_json)까지 포함된 trail.sql(CREATE + INSERT)을 생성한다.

이 스크립트는 "인터넷이 되는 PC"에서 한 번만 실행하면 된다.
생성된 trail.sql 을 DB에 넣으면, 백엔드는 durunubi 를 런타임에 호출하지 않고
DB의 path_json 좌표만 읽어 응답한다.

준비:  pip install openpyxl
실행:  python seed_trails.py "코스목록정보 1.xlsx" trail.sql
"""

import sys, ssl, json, re, time
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from xml.etree import ElementTree as ET
import openpyxl

MAX_POINTS = 600          # 코스당 최대 좌표 수(초과 시 균등 다운샘플)
WORKERS    = 8            # 동시 다운로드 수
RETRY      = 3
TIMEOUT    = 25

# durunubi 는 인증서 체인이 불와전한 경우가 있어 검증 우회
_CTX = ssl.create_default_context()
_CTX.check_hostname = False
_CTX.verify_mode = ssl.CERT_NONE


def fetch_gpx(url: str) -> bytes:
    last = None
    for _ in range(RETRY):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
            return urllib.request.urlopen(req, timeout=TIMEOUT, context=_CTX).read()
        except Exception as e:
            last = e
            time.sleep(1.2)
    raise last


def parse_points(xml: bytes):
    """GPX -> [[lat, lng], ...] (trkpt -> rtept -> wpt 순, 네임스페이스 무시)"""
    try:
        root = ET.fromstring(xml)
    except Exception:
        # 파싱 실패 시 정규식 백업
        pts = re.findall(rb'<(?:trkpt|rtept|wpt)[^>]*?lat="([\d.\-]+)"[^>]*?lon="([\d.\-]+)"', xml)
        return [[round(float(a), 6), round(float(b), 6)] for a, b in pts]

    def local(t): return t.rsplit('}', 1)[-1]
    for want in ("trkpt", "rtept", "wpt"):
        out = []
        for el in root.iter():
            if local(el.tag) == want:
                lat = el.get("lat"); lon = el.get("lon")
                if lat and lon:
                    try:
                        out.append([round(float(lat), 6), round(float(lon), 6)])
                    except ValueError:
                        pass
        if out:
            return out
    return []


def downsample(pts):
    n = len(pts)
    if n <= MAX_POINTS:
        return pts
    step = (n - 1) / (MAX_POINTS - 1)
    return [pts[round(i * step)] for i in range(MAX_POINTS)]


def q(v):
    if v is None or v == "":
        return "NULL"
    return "'" + str(v).replace("'", "''") + "'"


def num(v):
    return "NULL" if v is None or v == "" else str(v)


def main():
    xlsx = sys.argv[1] if len(sys.argv) > 1 else "코스목록정보 1.xlsx"
    out  = sys.argv[2] if len(sys.argv) > 2 else "trail.sql"

    wb = openpyxl.load_workbook(xlsx, data_only=True)
    ws = wb.active
    rows = list(ws.iter_rows(values_only=True))[1:]  # 헤더 제외
    print(f"[i] 코스 {len(rows)}건, GPX 다운로드 시작 (동시 {WORKERS})...")

    # 병렬 다운로드/파싱
    def work(idx_row):
        i, r = idx_row
        gpx_url = r[13]
        try:
            pts = downsample(parse_points(fetch_gpx(gpx_url)))
            return i, pts, None
        except Exception as e:
            return i, [], str(e)

    paths = {}
    fails = []
    done = 0
    with ThreadPoolExecutor(max_workers=WORKERS) as ex:
        for i, pts, err in ex.map(work, list(enumerate(rows))):
            paths[i] = pts
            done += 1
            if err or not pts:
                fails.append((rows[i][1], err or "좌표 없음"))
            if done % 20 == 0 or done == len(rows):
                print(f"    {done}/{len(rows)} 완료")

    # SQL 생성
    cols = ("(course_id, route_id, name, distance_km, duration_min, difficulty, cycle_type, "
            "description, summary, tour_point, traveler_info, region, sido, travel_type, "
            "gpx_url, path_json, created_at, updated_at)")

    lines = []
    for i, r in enumerate(rows):
        route_id, course_id, name, dist, dur, diff, cyc, desc, summ, tour, trav, region, travel, gpx, cre, upd = r
        sido = (region or "").split(" ")[0]
        path_json = json.dumps(paths.get(i, []), ensure_ascii=False, separators=(",", ":"))
        vals = [
            q(course_id), q(route_id), q(name), num(dist), num(dur), num(diff), q(cyc),
            q(desc), q(summ), q(tour), q(trav), q(region), q(sido), q(travel),
            q(gpx), q(path_json), q(cre), q(upd),
        ]
        lines.append("  (" + ", ".join(vals) + ")")

    schema = """-- ============================================================
-- 산책로(코스) 테이블 — 두루누비 코스 271건 (좌표 path_json 포함, 런타임 durunubi 호출 없음)
-- ============================================================
CREATE TABLE IF NOT EXISTS trail (
    course_id     VARCHAR(40)      PRIMARY KEY,
    route_id      VARCHAR(40)      NOT NULL,
    name          VARCHAR(100)     NOT NULL,
    distance_km   DOUBLE PRECISION,
    duration_min  INT,
    difficulty    INT,
    cycle_type    VARCHAR(20),
    description   TEXT,
    summary       TEXT,
    tour_point    TEXT,
    traveler_info TEXT,
    region        VARCHAR(60),
    sido          VARCHAR(20),
    travel_type   VARCHAR(10),
    gpx_url       VARCHAR(500),
    path_json     TEXT,            -- GPX 파싱 좌표 [[위도,경도], ...] JSON
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP
);

"""
    conflict = """
ON CONFLICT (course_id) DO UPDATE SET
    route_id = EXCLUDED.route_id, name = EXCLUDED.name, distance_km = EXCLUDED.distance_km,
    duration_min = EXCLUDED.duration_min, difficulty = EXCLUDED.difficulty, cycle_type = EXCLUDED.cycle_type,
    description = EXCLUDED.description, summary = EXCLUDED.summary, tour_point = EXCLUDED.tour_point,
    traveler_info = EXCLUDED.traveler_info, region = EXCLUDED.region, sido = EXCLUDED.sido,
    travel_type = EXCLUDED.travel_type, gpx_url = EXCLUDED.gpx_url, path_json = EXCLUDED.path_json,
    created_at = EXCLUDED.created_at, updated_at = EXCLUDED.updated_at;
"""
    sql = schema + "INSERT INTO trail " + cols + "\nVALUES\n" + ",\n".join(lines) + conflict

    with open(out, "w", encoding="utf-8") as f:
        f.write(sql)

    ok = len(rows) - len(fails)
    print(f"\n[✓] 완료: {out}")
    print(f"    좌표 있는 코스 {ok} / {len(rows)}")
    if fails:
        print(f"    좌표 실패 {len(fails)}건 (path_json = [] 로 저장됨):")
        for cid, why in fails[:20]:
            print(f"      - {cid}: {why}")
        if len(fails) > 20:
            print(f"      ... 외 {len(fails) - 20}건")


if __name__ == "__main__":
    main()
