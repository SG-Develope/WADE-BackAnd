package com.wade.wadeapi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UltraShortForecastItem {
    private String fcstDate;
    private String fcstTime;
    @Builder.Default private double temperature = 0.0;
    @Builder.Default private String skyCondition = "맑음";
    @Builder.Default private String precipType = "없음";
    @Builder.Default private String precipAmount = "0";
}
