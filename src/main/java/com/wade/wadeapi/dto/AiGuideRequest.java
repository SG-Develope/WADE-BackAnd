package com.wade.wadeapi.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AiGuideRequest {
    private Map<String, Double> stationLevels;
}
