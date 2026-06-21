package com.wade.wadeapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherResponse {
    private double temperature;
    private int rainProbability;
    private String skyCondition;
    private int precipitationType;
    private double windSpeed;
    private String windDirection;
    private int humidity;
    private String fcstDate;
    private String fcstTime;
    private String measuredAt;
}
