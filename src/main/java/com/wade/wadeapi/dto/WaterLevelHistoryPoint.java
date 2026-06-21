package com.wade.wadeapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterLevelHistoryPoint {
    private double level;
    private String status;
    private String measuredAt;
}
