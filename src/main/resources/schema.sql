CREATE TABLE IF NOT EXISTS station_meta (
    id          VARCHAR(20)  PRIMARY KEY,
    wlobscd     VARCHAR(20)  NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    location    VARCHAR(50)  NOT NULL,
    gdt         DOUBLE PRECISION NOT NULL,
    attwl       DOUBLE PRECISION NOT NULL,
    wrnwl       DOUBLE PRECISION NOT NULL,
    almwl       DOUBLE PRECISION NOT NULL,
    srswl       DOUBLE PRECISION NOT NULL,
    pfh         DOUBLE PRECISION NOT NULL
);

CREATE TABLE IF NOT EXISTS cctv (
    id          VARCHAR(20)  PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    location    VARCHAR(50)  NOT NULL,
    station_id  VARCHAR(20)  NOT NULL REFERENCES station_meta(id),
    stream_url  VARCHAR(500)
);
