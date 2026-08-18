package com.wade.wadeapi.domain;

import lombok.Data;
import java.time.LocalDate;

/**
 * 전국 문화 여가 활동 시설(캠핑) — camping_site 테이블 매핑.
 * 한국문화정보원 20221130 데이터. WADE는 구미시·칠곡군만 조회.
 * 컬럼(snake_case) ↔ 필드(camelCase)는 map-underscore-to-camel-case 로 자동 매핑.
 */
@Data
public class CampingSite {

    private Long id;

    // 기본 정보
    private String facilityName;   // 시설명
    private String category1;      // 여가활동
    private String category2;      // 캠핑
    private String category3;      // 글램핑,일반야영장,카라반 등 복수

    // 주소
    private String sido;
    private String sigungu;
    private String eupmyeondong;
    private String ri;
    private String beonji;
    private String roadName;
    private String buildingNo;
    private Double latitude;
    private Double longitude;
    private String zipcode;
    private String roadAddress;
    private String jibunAddress;

    // 연락·운영주체
    private String phone;
    private String homepage;
    private String operator;

    // 운영 여부
    private Boolean openWeekday;
    private Boolean openWeekend;
    private Boolean openSpring;
    private Boolean openSummer;
    private Boolean openFall;
    private Boolean openWinter;

    // 부대시설
    private Boolean amenityElectricity;
    private Boolean amenityHotWater;
    private Boolean amenityWifi;
    private Boolean amenityFirewoodSale;
    private Boolean amenityWalkingTrail;
    private Boolean amenityWaterPlay;
    private Boolean amenityPlayground;
    private Boolean amenityMart;

    // 부대시설 개수
    private Integer restroomCount;
    private Integer showerCount;
    private Integer sinkCount;
    private Integer extinguisherCount;

    // 주변 시설
    private Boolean nearbyFishing;
    private Boolean nearbyWalkingTrail;
    private Boolean nearbyWaterBeach;
    private Boolean nearbyWaterLeisure;
    private Boolean nearbyWaterValley;
    private Boolean nearbyWaterRiver;
    private Boolean nearbyWaterPool;
    private Boolean nearbyYouthFacility;
    private Boolean nearbyRuralExperience;
    private Boolean nearbyChildrenPlay;

    // 글램핑 옵션
    private Boolean glampingBed;
    private Boolean glampingTv;
    private Boolean glampingFridge;
    private Boolean glampingInternet;
    private Boolean glampingInternalRestroom;
    private Boolean glampingAircon;
    private Boolean glampingHeater;
    private Boolean glampingCookingTools;

    // 설명
    private String features;
    private String description;
    private LocalDate lastUpdated;
}
