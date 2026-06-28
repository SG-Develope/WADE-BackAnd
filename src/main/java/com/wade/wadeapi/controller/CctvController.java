package com.wade.wadeapi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wade.wadeapi.domain.Cctv;
import com.wade.wadeapi.mapper.CctvMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/cctv")
@RequiredArgsConstructor
public class CctvController {

    private final CctvMapper cctvMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${its.api-key:}")
    private String itsApiKey;

    private static final String ITS_URL =
        "https://openapi.its.go.kr:9443/cctvInfo?apiKey=%s&type=its&cctvType=1" +
        "&minX=%.6f&maxX=%.6f&minY=%.6f&maxY=%.6f&getType=json";

    @GetMapping
    public ResponseEntity<List<Cctv>> getAll() {
        List<Cctv> cctvs = cctvMapper.findAll();
        enrichWithFreshUrl(cctvs);
        return ResponseEntity.ok(cctvs);
    }

    @GetMapping("/station/{stationId}")
    public ResponseEntity<List<Cctv>> getByStation(@PathVariable String stationId) {
        List<Cctv> cctvs = cctvMapper.findByStationId(stationId);
        enrichWithFreshUrl(cctvs);
        return ResponseEntity.ok(cctvs);
    }

    private void enrichWithFreshUrl(List<Cctv> cctvs) {
        if (cctvs.isEmpty() || itsApiKey.isBlank()) return;
        try {
            double minX = cctvs.stream().mapToDouble(Cctv::getLng).min().orElse(128.0) - 0.01;
            double maxX = cctvs.stream().mapToDouble(Cctv::getLng).max().orElse(129.0) + 0.01;
            double minY = cctvs.stream().mapToDouble(Cctv::getLat).min().orElse(35.0) - 0.01;
            double maxY = cctvs.stream().mapToDouble(Cctv::getLat).max().orElse(36.0) + 0.01;

            String url = String.format(ITS_URL, itsApiKey, minX, maxX, minY, maxY);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode data = objectMapper.readTree(response).path("response").path("data");

            if (!data.isArray()) return;

            for (Cctv cctv : cctvs) {
                for (JsonNode item : data) {
                    if (cctv.getName().equals(item.path("cctvname").asText())) {
                        cctv.setStreamUrl(item.path("cctvurl").asText(null));
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("ITS CCTV API 실패, DB URL 사용: {}", e.getMessage());
        }
    }
}
