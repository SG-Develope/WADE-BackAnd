package com.wade.wadeapi.domain;

import lombok.Data;

@Data
public class StationMeta {
    private String id;
    private String wlobscd;
    private String name;
    private String location;
    private double gdt;
    private double attwl;   // 관심수위
    private double wrnwl;   // 경보수위
    private double almwl;   // 경계수위
    private double srswl;   // 심각수위
    private double pfh;     // 계획홍수위
}
