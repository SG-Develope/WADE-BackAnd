package com.wade.wadeapi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WeatherAlertItem {
    private String warnVar;    // 호우, 강풍, 대설, 태풍 등
    private String warnLevel;  // 주의보, 경보
    private String area;       // 지역명
    private String title;
    private String content;
    private String issuedAt;   // tmFc
}
