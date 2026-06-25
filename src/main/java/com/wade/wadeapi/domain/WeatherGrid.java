package com.wade.wadeapi.domain;

import lombok.Data;

@Data
public class WeatherGrid {
    private String stationId;
    private int nx;
    private int ny;
}
