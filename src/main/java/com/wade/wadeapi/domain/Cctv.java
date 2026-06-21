package com.wade.wadeapi.domain;

import lombok.Data;

@Data
public class Cctv {
    private String id;
    private String name;
    private String location;
    private String stationId;
    private String streamUrl;
}
