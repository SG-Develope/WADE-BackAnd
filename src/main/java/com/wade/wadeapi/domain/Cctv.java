package com.wade.wadeapi.domain;

import lombok.Data;

@Data
public class Cctv {
    private String id;
    private String name;
    private String location;    // station JOIN
    private String stationId;
    private String stationName;
    private double lat;
    private double lng;
    private String format;
    private String streamUrl;
}
