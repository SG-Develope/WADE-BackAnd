CREATE TABLE IF NOT EXISTS station (
    id           VARCHAR(20)      PRIMARY KEY,
    obs_code     VARCHAR(20)      NOT NULL UNIQUE,
    name         VARCHAR(50)      NOT NULL,
    location     VARCHAR(50)      NOT NULL,
    datum        DOUBLE PRECISION NOT NULL,
    wl_attention DOUBLE PRECISION NOT NULL,
    wl_warning   DOUBLE PRECISION NOT NULL,
    wl_alarm     DOUBLE PRECISION NOT NULL,
    wl_serious   DOUBLE PRECISION NOT NULL,
    wl_flood     DOUBLE PRECISION NOT NULL,
    min_x        DOUBLE PRECISION,
    max_x        DOUBLE PRECISION,
    min_y        DOUBLE PRECISION,
    max_y        DOUBLE PRECISION
);

-- 이미 생성된 DB(Neon 등) 대비: 컬럼 없으면 추가
ALTER TABLE station ADD COLUMN IF NOT EXISTS min_x DOUBLE PRECISION;
ALTER TABLE station ADD COLUMN IF NOT EXISTS max_x DOUBLE PRECISION;
ALTER TABLE station ADD COLUMN IF NOT EXISTS min_y DOUBLE PRECISION;
ALTER TABLE station ADD COLUMN IF NOT EXISTS max_y DOUBLE PRECISION;

CREATE TABLE IF NOT EXISTS cctv (
    id         VARCHAR(30)      PRIMARY KEY,
    name       VARCHAR(100)     NOT NULL,
    station_id VARCHAR(20)      NOT NULL REFERENCES station(id),
    lat        DOUBLE PRECISION NOT NULL,
    lng        DOUBLE PRECISION NOT NULL,
    format     VARCHAR(10)      NOT NULL DEFAULT 'HLS',
    stream_url VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS place (
    id         VARCHAR(30)      PRIMARY KEY,
    name       VARCHAR(100)     NOT NULL,
    type       VARCHAR(20)      NOT NULL,
    icon       VARCHAR(10),
    lat        DOUBLE PRECISION NOT NULL,
    lng        DOUBLE PRECISION NOT NULL,
    station_id VARCHAR(20)      NOT NULL REFERENCES station(id),
    safe_wl    DOUBLE PRECISION NOT NULL,
    caution_wl DOUBLE PRECISION NOT NULL
);

CREATE TABLE IF NOT EXISTS place_amenity (
    place_id VARCHAR(30) NOT NULL REFERENCES place(id) ON DELETE CASCADE,
    amenity  VARCHAR(50) NOT NULL,
    PRIMARY KEY (place_id, amenity)
);

-- 기상청 격자 좌표 (관측소별 날씨 API nx/ny 매핑)
CREATE TABLE IF NOT EXISTS weather_grid (
    station_id VARCHAR(20)  PRIMARY KEY REFERENCES station(id),
    nx         INT          NOT NULL,
    ny         INT          NOT NULL
);
