package com.wade.wadeapi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SatelliteImageResponse {
    private String imageUrl;
    private String measuredAt;
}
