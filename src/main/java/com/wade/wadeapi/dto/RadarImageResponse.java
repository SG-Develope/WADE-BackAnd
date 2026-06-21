package com.wade.wadeapi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RadarImageResponse {
    private String imageUrl;
    private String measuredAt;
}
