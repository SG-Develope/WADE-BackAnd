package com.wade.wadeapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationWaterLevel {
    // 프론트엔드 Station 타입과 필드명 일치
    private String id;
    private String name;
    private String location;
    private double currentLevel;
    private String status;
    private String measuredAt;
    private double normalLevel;
    private double cautionLevel;
    private double warningLevel;
    private double criticalLevel;
    private double designFloodLevel;
}
