package com.wade.wadeapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wade.wadeapi.domain.Station;
import com.wade.wadeapi.dto.*;
import com.wade.wadeapi.mapper.StationMapper;
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
    private final StationMapper stationMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${hrfco.api-key:}")
    private String hrfcoApiKey;

    private static final String HRFCO_BASE = "https://api.hrfco.go.kr";

    public WaterLevelCurrentResponse getCurrent() {
        List<Station> stationList = stationMapper.findAll();
        List<StationWaterLevel> stations = new ArrayList<>();
        for (Station station : stationList) {
            double level = fetchCurrentLevel(station.getObsCode(), station.getName());
            stations.add(buildStationResponse(station, level));
        }
        return WaterLevelCurrentResponse.builder()
                .updatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .stations(stations)
                .build();
    }

    private double fetchCurrentLevel(String obsCode, String name) {
        try {
            String url = String.format("%s/%s/waterlevel/list/10M/%s.json",
                    HRFCO_BASE, hrfcoApiKey, obsCode);
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
        Station station = stationMapper.findById(stationId);
        if (station == null) throw new RuntimeException("관측소를 찾을 수 없습니다: " + stationId);
        try {
            return fetchHistory(station, hours);
        } catch (Exception e) {
            log.error("수위 이력 API 실패 (IP 차단 가능): {}", e.getMessage());
            return WaterLevelHistoryResponse.builder()
                    .stationId(stationId)
                    .history(Collections.emptyList())
                    .build();
        }
    }

    private WaterLevelHistoryResponse fetchHistory(Station station, int hours) throws Exception {
        LocalDateTime raw = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

        String url;
        String interval;
        if (hours <= 6) {
            LocalDateTime now   = raw.withMinute((raw.getMinute() / 10) * 10).withSecond(0).withNano(0);
            LocalDateTime start = now.minusHours(hours);
            url      = String.format("%s/%s/waterlevel/list/10M/%s/%s/%s.json",
                    HRFCO_BASE, hrfcoApiKey, station.getObsCode(), start.format(fmt), now.format(fmt));
            interval = "10M";
        } else {
            LocalDateTime now   = raw.withMinute(0).withSecond(0).withNano(0);
            LocalDateTime start = now.minusHours(hours);
            url      = String.format("%s/%s/waterlevel/list/1H/%s/%s/%s.json",
                    HRFCO_BASE, hrfcoApiKey, station.getObsCode(), start.format(fmt), now.format(fmt));
            interval = "1H";
        }
        log.info("[히스토리] {}h ({}) URL: {}", hours, interval, url);

        String response = restTemplate.getForObject(url, String.class);
        log.info("[히스토리] 응답 앞 200자: {}",
                response != null ? response.substring(0, Math.min(200, response.length())) : "null");
        JsonNode content = objectMapper.readTree(response).path("content");

        List<WaterLevelHistoryPoint> points = new ArrayList<>();
        if (content.isArray()) {
            for (JsonNode item : content) {
                String wlStr  = item.path("wl").asText("").trim();
                String ymdhm  = item.path("ymdhm").asText("");
                if (wlStr.isEmpty() || wlStr.equals("null")) continue;

                double level = Double.parseDouble(wlStr);
                String measuredAt = parseYmdhm(ymdhm);
                points.add(WaterLevelHistoryPoint.builder()
                        .level(level)
                        .status(calcStatus(station, level))
                        .measuredAt(measuredAt)
                        .build());
            }
        }
        Collections.reverse(points);
        return WaterLevelHistoryResponse.builder()
                .stationId(station.getId())
                .history(points)
                .build();
    }

    private String parseYmdhm(String ymdhm) {
        if (ymdhm.length() == 12) {
            return ymdhm.substring(0, 4) + "-" + ymdhm.substring(4, 6) + "-"
                    + ymdhm.substring(6, 8) + "T" + ymdhm.substring(8, 10) + ":"
                    + ymdhm.substring(10, 12) + ":00";
        } else if (ymdhm.length() == 10) {
            return ymdhm.substring(0, 4) + "-" + ymdhm.substring(4, 6) + "-"
                    + ymdhm.substring(6, 8) + "T" + ymdhm.substring(8, 10) + ":00:00";
        }
        return "";
    }

    private StationWaterLevel buildStationResponse(Station station, double level) {
        String status = level < 0 ? "unknown" : calcStatus(station, level);
        return StationWaterLevel.builder()
                .id(station.getId())
                .name(station.getName())
                .location(station.getLocation())
                .currentLevel(Math.max(level, 0))
                .status(status)
                .measuredAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .normalLevel(station.getWlAttention())
                .cautionLevel(station.getWlWarning())
                .warningLevel(station.getWlAlarm())
                .criticalLevel(station.getWlSerious())
                .designFloodLevel(station.getWlFlood())
                .build();
    }

    private String calcStatus(Station station, double level) {
        if (station == null) return "normal";
        if (level >= station.getWlSerious()) return "critical";
        if (level >= station.getWlAlarm())   return "warning";
        if (level >= station.getWlAttention()) return "caution";
        return "normal";
    }
}
