package com.wade.wadeapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wade.wadeapi.domain.Station;
import com.wade.wadeapi.dto.AiGuideResponse;
import com.wade.wadeapi.mapper.StationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiGuideService {

    private final RestTemplate restTemplate;
    private final StationMapper stationMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    public String buildCacheKey(Map<String, Double> levels) {
        if (levels == null || levels.isEmpty()) return "empty";
        return levels.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + ":" + Math.round(e.getValue() * 10))
                .reduce("", (a, b) -> a + "|" + b);
    }

    @Cacheable(value = "aiGuide", key = "#root.target.buildCacheKey(#stationLevels)")
    public AiGuideResponse getSafetyGuide(Map<String, Double> stationLevels) {
        double yangpo = stationLevels.getOrDefault("yangpo", 0.0);
        double hoguk  = stationLevels.getOrDefault("hoguk",  0.0);

        Station yStation = stationMapper.findById("yangpo");
        Station hStation = stationMapper.findById("hoguk");

        try {
            if (geminiApiKey != null && !geminiApiKey.isBlank()) {
                return callGeminiApi(yangpo, hoguk, yStation, hStation);
            }
        } catch (Exception e) {
            log.warn("Gemini API 호출 실패, rule-based 메시지 사용: {}", e.getMessage());
        }

        return AiGuideResponse.builder()
                .message(generateRuleBasedMessage(yangpo, hoguk, yStation, hStation))
                .generatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .cached(false)
                .build();
    }

    private AiGuideResponse callGeminiApi(double yangpo, double hoguk,
                                          Station y, Station h) throws Exception {
        String yStatus = calcStatus(yangpo, y);
        String hStatus = calcStatus(hoguk,  h);
        double yDiff   = yangpo - y.getWlAttention();
        double hDiff   = hoguk  - h.getWlAttention();

        String prompt =
            "낙동강 실시간 수위 현황을 바탕으로 강변 여가객에게 상세한 안전 브리핑을 작성해줘.\n\n" +
            "[양포교 — 구미시]\n" +
            "현재: " + String.format("%.2f", yangpo) + "m | 단계: " + yStatus + " | 체감: " + heightLabel(yangpo) + "\n" +
            "기준: 관심 " + y.getWlAttention() + "m / 주의보 " + y.getWlWarning() +
                 "m / 경보 " + y.getWlAlarm()    + "m / 심각 " + y.getWlSerious() + "m\n" +
            "관심수위 대비: " + String.format("%+.2f", yDiff) + "m\n\n" +
            "[호국의다리 — 칠곡군]\n" +
            "현재: " + String.format("%.2f", hoguk) + "m | 단계: " + hStatus + " | 체감: " + heightLabel(hoguk) + "\n" +
            "기준: 관심 " + h.getWlAttention() + "m / 주의보 " + h.getWlWarning() +
                 "m / 경보 " + h.getWlAlarm()    + "m / 심각 " + h.getWlSerious() + "m\n" +
            "관심수위 대비: " + String.format("%+.2f", hDiff) + "m\n\n" +
            "아래 JSON 형식으로만 응답해. 마크다운이나 다른 텍스트 없이 JSON만.\n" +
            "{\n" +
            "  \"message\": \"3~5문장. 각 관측소 수위와 관심수위 대비 차이를 구체적으로 언급. 체감 높이로 위험도 설명. 산책/낚시/자전거/캠핑 활동 가능 여부 명확히 안내. 친근한 한국어, 이모지 2~3개.\",\n" +
            "  \"activities\": { \"walking\": true, \"fishing\": true, \"cycling\": true, \"camping\": true }\n" +
            "}";

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "contents", new Object[]{
                    Map.of("parts", new Object[]{Map.of("text", prompt)})
                },
                "generationConfig", Map.of("maxOutputTokens", 800)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                GEMINI_API_URL + geminiApiKey, HttpMethod.POST,
                new HttpEntity<>(requestBody, headers), String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        String text = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText().trim();

        if (text.startsWith("```")) {
            text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        }
        if (text.contains("{")) {
            text = text.substring(text.indexOf("{"), text.lastIndexOf("}") + 1);
        }

        JsonNode parsed  = objectMapper.readTree(text);
        JsonNode actNode = parsed.path("activities");
        Map<String, Boolean> activities = new LinkedHashMap<>();
        activities.put("walking", actNode.path("walking").asBoolean(true));
        activities.put("fishing", actNode.path("fishing").asBoolean(true));
        activities.put("cycling", actNode.path("cycling").asBoolean(true));
        activities.put("camping", actNode.path("camping").asBoolean(true));

        return AiGuideResponse.builder()
                .message(parsed.path("message").asText())
                .activities(activities)
                .generatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .cached(false)
                .build();
    }

    private String heightLabel(double level) {
        if (level < 0.3)  return "발목 아래";
        if (level < 0.6)  return "성인 무릎 높이";
        if (level < 1.0)  return "성인 허리 높이";
        if (level < 1.2)  return "초등학생 키 수준";
        if (level < 1.6)  return "중학생 키 수준";
        if (level < 1.75) return "성인 여성 키 수준";
        if (level < 2.0)  return "성인 남성 키 수준";
        if (level < 2.5)  return "성인 머리 위";
        if (level < 3.5)  return "버스 지붕 높이";
        if (level < 5.0)  return "2층 건물 높이";
        return "아파트 2~3층 높이";
    }

    private String generateRuleBasedMessage(double yangpo, double hoguk,
                                             Station y, Station h) {
        String yStatus   = calcStatus(yangpo, y);
        String hStatus   = calcStatus(hoguk,  h);
        boolean critical = "critical".equals(yStatus) || "critical".equals(hStatus);
        boolean warning  = "warning".equals(yStatus)  || "warning".equals(hStatus);
        boolean caution  = "caution".equals(yStatus)  || "caution".equals(hStatus);
        double yDiff = yangpo - y.getWlAttention();
        double hDiff = hoguk  - h.getWlAttention();

        if (critical) {
            return String.format(
                "🚨 낙동강 심각 단계 발령! 양포교 현재 수위 %.2fm(관심수위 대비 %+.2fm, %s), " +
                "호국의다리 %.2fm(관심수위 대비 %+.2fm, %s)으로 매우 위험한 상황입니다. " +
                "강변 모든 여가 활동(산책·낚시·자전거·캠핑)을 즉시 중단하고 안전한 고지대로 대피하세요. " +
                "수위가 빠르게 상승 중이니 강변 접근은 절대 금지입니다.",
                yangpo, yDiff, heightLabel(yangpo), hoguk, hDiff, heightLabel(hoguk));
        } else if (warning) {
            return String.format(
                "🔴 낙동강 경보 단계 발령. 양포교 %.2fm(관심수위 대비 %+.2fm, %s), " +
                "호국의다리 %.2fm(관심수위 대비 %+.2fm, %s)으로 위험 수위에 도달했습니다. " +
                "캠핑·낚시·수상 활동은 즉시 중단하고 강변에서 벗어나주세요. " +
                "산책로 및 제방 근처 접근도 삼가고, 수위 변화를 지속적으로 확인하시기 바랍니다.",
                yangpo, yDiff, heightLabel(yangpo), hoguk, hDiff, heightLabel(hoguk));
        } else if (caution) {
            return String.format(
                "⚠️ 낙동강 주의보 단계. 양포교 %.2fm(관심수위 대비 %+.2fm, %s), " +
                "호국의다리 %.2fm(관심수위 대비 %+.2fm, %s)입니다. " +
                "낚시·캠핑은 자제하고, 산책이나 자전거는 강변 저지대를 피해 고지대 코스만 이용해주세요. " +
                "수위가 추가 상승할 수 있으므로 강변에 오래 머무르지 않도록 주의하세요.",
                yangpo, yDiff, heightLabel(yangpo), hoguk, hDiff, heightLabel(hoguk));
        } else {
            return String.format(
                "🌿 낙동강 전 구간 정상 수위입니다. 양포교 %.2fm(관심수위 대비 %+.2fm, %s), " +
                "호국의다리 %.2fm(관심수위 대비 %+.2fm, %s)으로 안전한 상태입니다. " +
                "산책·자전거·낚시·캠핑 모두 이용 가능하며, 강변 여가 활동을 즐기기 좋은 날입니다. " +
                "다만 날씨 변화나 상류 방류 상황에 따라 수위가 변동될 수 있으니 WADE 앱으로 수시 확인하세요!",
                yangpo, yDiff, heightLabel(yangpo), hoguk, hDiff, heightLabel(hoguk));
        }
    }

    private String calcStatus(double level, Station station) {
        if (station == null)                       return "normal";
        if (level >= station.getWlSerious())       return "critical";
        if (level >= station.getWlAlarm())         return "warning";
        if (level >= station.getWlWarning())       return "caution";
        return "normal";
    }
}
