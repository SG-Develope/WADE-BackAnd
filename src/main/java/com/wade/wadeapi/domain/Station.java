package com.wade.wadeapi.domain;

import lombok.Data;

@Data
public class Station {
    private String id;
    private String obsCode;     // HRFCO 관측소 코드
    private String name;
    private String location;
    private double datum;       // 영점 해발고도(m)
    private double wlAttention; // 관심수위
    private double wlWarning;   // 주의보
    private double wlAlarm;     // 경보
    private double wlSerious;   // 심각
    private double wlFlood;     // 계획홍수위
}
