package com.wade.wadeapi.domain;

import lombok.Data;
import java.util.List;

@Data
public class Place {
    private String id;
    private String name;
    private String type;
    private String icon;
    private double lat;
    private double lng;
    private String stationId;
    private double safeWl;
    private double cautionWl;
    private List<String> amenities;
}
