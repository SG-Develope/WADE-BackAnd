INSERT INTO station_meta (id, wlobscd, name, location, gdt, attwl, wrnwl, almwl, srswl, pfh)
VALUES
  ('yangpo', '2011625', '양포교',     '구미시', 27.78,  3.0,  4.0,  5.1,  6.17,  6.17),
  ('hoguk',  '2011650', '호국의다리', '칠곡군', 11.532, 8.46, 11.6, 13.1, 15.81, 15.81)
ON CONFLICT (id) DO UPDATE SET
    attwl = EXCLUDED.attwl,
    wrnwl = EXCLUDED.wrnwl,
    almwl = EXCLUDED.almwl,
    srswl = EXCLUDED.srswl,
    pfh   = EXCLUDED.pfh;

INSERT INTO cctv (id, name, location, station_id, stream_url)
VALUES
  ('yp', '양포교',     '구미시', 'yangpo', NULL),
  ('hk', '호국의다리', '칠곡군', 'hoguk',  NULL)
ON CONFLICT (id) DO UPDATE SET
    name       = EXCLUDED.name,
    location   = EXCLUDED.location,
    station_id = EXCLUDED.station_id,
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

INSERT INTO place_amenity (place_id, amenity)
VALUES
  ('camping', '주차장'), ('camping', '화장실'), ('camping', '샤워실'), ('camping', '취사장'),
  ('fishing', '주차장'), ('fishing', '화장실'),
  ('cycling', '자전거 보관대'), ('cycling', '휴게소'),
  ('park', '주차장'), ('park', '화장실'), ('park', '산책로'), ('park', '운동기구')
ON CONFLICT DO NOTHING;
