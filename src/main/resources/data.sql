INSERT INTO station (id, obs_code, name, location, datum, wl_attention, wl_warning, wl_alarm, wl_serious, wl_flood)
VALUES
  ('yangpo', '2011625', '양포교',     '구미시', 27.78,  3.0,  4.0,  5.1,  6.17,  6.17),
  ('hoguk',  '2011650', '호국의다리', '칠곡군', 11.532, 8.46, 11.6, 13.1, 15.81, 15.81)
ON CONFLICT (id) DO UPDATE SET
    obs_code     = EXCLUDED.obs_code,
    wl_attention = EXCLUDED.wl_attention,
    wl_warning   = EXCLUDED.wl_warning,
    wl_alarm     = EXCLUDED.wl_alarm,
    wl_serious   = EXCLUDED.wl_serious,
    wl_flood     = EXCLUDED.wl_flood;

INSERT INTO cctv (id, name, station_id, lat, lng, format, stream_url)
VALUES
  ('hk_01', '[국도4] 칠곡 왜관교',        'hoguk', 35.98151,  128.38559,  'HLS', 'http://cctvsec.ktict.co.kr/4047/iF2+Z55k+RLihnli+gOiXVnSXDYMKyLM+gnED1hFH+qK6oqIUQMfFJlIrX751lRvdLYliO/SlAHhxAEtxwznopVrtW16hu/BUsRk9GV+5Ck='),
  ('hk_02', '[위임국도67] 칠곡 제2왜관교', 'hoguk', 35.977746, 128.394771, 'HLS', 'http://cctvsec.ktict.co.kr/41380/dd7nFJiAE64Cv/TTGN2IkIr5hZY+AZQ0iiEVTZQ44ELfYCqpn3cY9n9q5OjSKrqgyYxrEF9LBVVGHggrUWIS6p+TTDl20q66PK8hu11+V2w='),
  ('hk_03', '[위임국도67] 칠곡 왜관교차로', 'hoguk', 35.988864, 128.39478,  'HLS', 'http://cctvsec.ktict.co.kr/41381/1ZT76K2PTvNK6LRlzxrQNx/0RSawA6JJk5yw/p/YdPCB99ykTTJa90wKOMyO3ugC7DVLqDiWea5CNozjQ+maLPNpin0sSn8923sAipEzdA8=')
ON CONFLICT (id) DO UPDATE SET
    name       = EXCLUDED.name,
    station_id = EXCLUDED.station_id,
    lat        = EXCLUDED.lat,
    lng        = EXCLUDED.lng,
    format     = EXCLUDED.format,
    stream_url = EXCLUDED.stream_url;

INSERT INTO place (id, name, type, icon, lat, lng, station_id, safe_wl, caution_wl)
VALUES
  ('camping', '구미 낙동강 오토캠핑장', 'camping', '🏕️', 36.1280, 128.3570, 'yangpo', 3.0, 4.5),
  ('fishing', '양포교 낚시터',          'fishing', '🎣', 36.1327, 128.3614, 'yangpo', 2.5, 3.5),
  ('cycling', '낙동강 자전거길 4코스',  'cycling', '🚴', 36.1200, 128.3500, 'yangpo', 3.5, 5.0),
  ('park',    '칠곡 낙동강 둔치공원',   'walking', '🚶', 35.9963, 128.4022, 'hoguk',  3.0, 4.5)
ON CONFLICT (id) DO UPDATE SET
    name       = EXCLUDED.name,
    safe_wl    = EXCLUDED.safe_wl,
    caution_wl = EXCLUDED.caution_wl;

-- 기상청 격자 좌표 (https://www.kma.go.kr/kma/jsp/stn/kma/obsGrid.do 참조)
INSERT INTO weather_grid (station_id, nx, ny)
VALUES
  ('yangpo', 86, 96),  -- 구미시
  ('hoguk',  85, 93)   -- 칠곡군 왜관
ON CONFLICT (station_id) DO UPDATE SET
    nx = EXCLUDED.nx,
    ny = EXCLUDED.ny;

INSERT INTO place_amenity (place_id, amenity)
VALUES
  ('camping', '주차장'), ('camping', '화장실'), ('camping', '샤워실'), ('camping', '취사장'),
  ('fishing', '주차장'), ('fishing', '화장실'),
  ('cycling', '자전거 보관대'), ('cycling', '휴게소'),
  ('park', '주차장'), ('park', '화장실'), ('park', '산책로'), ('park', '운동기구')
ON CONFLICT DO NOTHING;
