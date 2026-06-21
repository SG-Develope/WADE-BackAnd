package com.wade.wadeapi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TyphoonInfoResponse {
    private boolean active;
    private String imageUrl;
    private String name;
    private String nameEn;
    private String location;
    private Double maxWindSpeed;
    private Double pressure;
    private String direction;
    private Double moveSpeed;
    private String measuredAt;
}
