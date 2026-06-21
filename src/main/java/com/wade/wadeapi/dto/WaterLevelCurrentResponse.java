package com.wade.wadeapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterLevelCurrentResponse {
    private String updatedAt;
    private List<StationWaterLevel> stations;
}
