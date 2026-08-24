package com.wade.wadeapi.domain;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 산책로(코스) — trail 테이블 매핑.
 * 두루누비(코리아둘레길) 코스 목록 데이터. 좌표는 gpx_xml(GPX 원문)에서 파싱.
 * 컬럼(snake_case) ↔ 필드(camelCase)는 map-underscore-to-camel-case 로 자동 매핑.
 */
@Data
public class Trail {

    private String courseId;      // 코스 고유번호 (PK)
    private String routeId;       // 길 고유번호
    private String name;          // 코스 명
    private Double distanceKm;    // 코스 길이(km)
    private Integer durationMin;  // 총 소요시간(분)
    private Integer difficulty;   // 난이도 (1=쉬움, 2=보통, 3=어려움)
    private String cycleType;     // 순환형태 (순환형/비순환형)
    private String description;   // 코스 설명
    private String summary;       // 코스 개요
    private String tourPoint;     // 관광 포인트
    private String travelerInfo;  // 여행자정보
    private String region;        // 행정구역 (예: 부산 중구)
    private String sido;          // 시/도 (지역 필터용)
    private String travelType;    // 걷기/자전거 구분 (DNWW=걷기)
    private LocalDateTime createdAt; // 등록일
    private LocalDateTime updatedAt; // 수정일
}
