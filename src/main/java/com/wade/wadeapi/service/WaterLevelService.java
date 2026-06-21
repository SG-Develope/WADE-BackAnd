package com.wade.wadeapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wade.wadeapi.domain.StationMeta;
import com.wade.wadeapi.dto.*;
import com.wade.wadeapi.mapper.StationMetaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaterLevelService {

    private final RestTemplate restTemplate;
    private final StationMetaMapper stationMetaMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${hrfco.api-key:}")
    private String hrfcoApiKey;

    private static final String HRFCO_BASE = "https://api.hrfco.go.kr";

    public WaterLevelCurrentResponse getCurrent() {
        List<StationMeta> stationList = stationMetaMapper.findAll();
        List<StationWaterLevel> stations = new ArrayList<>();
        for (StationMeta station : stationList) {
            double level = fetchCurrentLevelFromHrfco(station.getWlobscd(), station.getName());
            stations.add(buildStationResponse(station, level));
        }
        return WaterLevelCurrentResponse.builder()
                .updatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .stations(stations)
                .build();
    }

    private double fetchCurrentLevelFromHrfco(String wlobscd, String name) {
        try {
            String url = String.format("%s/%s/waterlevel/list/10M/%s.json",
                    HRFCO_BASE, hrfcoApiKey, wlobscd);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode content = objectMapper.readTree(response).path("content");
            if (content.isArray() && content.size() > 0) {
                String wlStr = content.get(0).path("wl").asText("").trim();
                if (!wlStr.isEmpty() && !wlStr.equals("null")) {
                    return Double.parseDouble(wlStr);
                }
            }
            log.warn("{} content 비어있음 — 기본값 반환", name);
            return -1.0;
        } catch (Exception e) {
            log.error("{} 수위 API 실패: {}", name, e.getMessage());
            return -1.0;
        }
    }

    public WaterLevelHistoryResponse getHistory(String stationId, int hours) {
        StationMeta station = stationMetaMapper.findById(stationId);
        if (station == null) throw new RuntimeException("관측소를 찾을 수 없습니다: " + stationId);
        try {
            return fetchHistoryFromHrfco(station, hours);
        } catch (Exception e) {
            log.error("수위 이력 API 실패 (IP 차단 가능): {}", e.getMessage());
            return WaterLevelHistoryResponse.builder()
                    .stationId(stationId)
                    .history(Collections.emptyList())
                    .build();
        }
    }

    private WaterLevelHistoryResponse fetchHistoryFromHrfco(StationMeta station, int hours) throws Exception {
        LocalDateTime raw = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        String url;
        String ymdhPattern;
        if (hours <= 6) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
            LocalDateTime now = raw.withMinute((raw.getMinute() / 10) * 10).withSecond(0).withNano(0);
            LocalDateTime start = now.minusHours(hours);
            url = String.format("%s/%s/waterlevel/list/10M/%s/%s/%s.json",
                    HRFCO_BASE, hrfcoApiKey, station.getWlobscd(), start.format(fmt), now.format(fmt));
            ymdhPattern = "10M";
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
            LocalDateTime now = raw.withMinute(0).withSecond(0).withNano(0);
            LocalDateTime start = now.minusHours(hours);
            url = String.format("%s/%s/waterlevel/list/1H/%s/%s/%s.json",
                    HRFCO_BASE, hrfcoApiKey, station.getWlobscd(), start.format(fmt), now.format(fmt));
            ymdhPattern = "1H";
        }
        log.info("[히스토리] {}h ({}) URL: {}", hours, ymdhPattern, url);

        String response = restTemplate.getForObject(url, String.class);
        log.info("[히스토리] 응답 앞 200자: {}", response != null ? response.substring(0, Math.min(200, response.length())) : "null");
        JsonNode content = objectMapper.readTree(response).path("content");

        List<WaterLevelHistoryPoint> historyPoints = new ArrayList<>();
        if (content.isArray()) {
            for (JsonNode item : content) {
                String wlStr = item.path("wl").asText("").trim();
                String ymdhm = item.path("ymdhm").asText("");
                if (wlStr.isEmpty() || wlStr.equals("null")) continue;

                double level = Double.parseDouble(wlStr);
                String measuredAt = "";
                if (ymdhm.length() == 12) {
                    // 10M: yyyyMMddHHmm
                    measuredAt = ymdhm.substring(0, 4) + "-" + ymdhm.substring(4, 6) + "-"
                            + ymdhm.substring(6, 8) + "T" + ymdhm.substring(8, 10) + ":"
                            + ymdhm.substring(10, 12) + ":00";
                } else if (ymdhm.length() == 10) {
                    // 1H: yyyyMMddHH
                    measuredAt = ymdhm.substring(0, 4) + "-" + ymdhm.substring(4, 6) + "-"
                            + ymdhm.substring(6, 8) + "T" + ymdhm.substring(8, 10) + ":00:00";
                }
                historyPoints.add(WaterLevelHistoryPoint.builder()
                        .level(level).status(calcStatus(station, level)).measuredAt(measuredAt)
                        .build());
            }
        }
        Collections.reverse(historyPoints);
        return WaterLevelHistoryResponse.builder().stationId(station.getId()).history(historyPoints).build();
    }

    private StationWaterLevel buildStationResponse(StationMeta station, double level) {
        String status = level < 0 ? "unknown" : calcStatus(station, level);
        return StationWaterLevel.builder()
                .id(station.getId())
                .name(station.getName())
                .location(station.getLocation())
                .currentLevel(Math.max(level, 0))
                .status(status)
                .measuredAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .normalLevel(station.getAttwl())
                .cautionLevel(station.getWrnwl())
                .warningLevel(station.getAlmwl())
                .criticalLevel(station.getSrswl())
                .designFloodLevel(station.getPfh())
                .build();
    }

    private String calcStatus(StationMeta station, double level) {
        if (station == null) return "normal";
        if (level >= station.getSrswl()) return "critical";
        if (level >= station.getAlmwl()) return "warning";
        if (level >= station.getAttwl()) return "caution";
        return "normal";
    }
}
