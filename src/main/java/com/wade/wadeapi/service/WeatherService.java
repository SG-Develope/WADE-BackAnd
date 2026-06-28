package com.wade.wadeapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wade.wadeapi.domain.WeatherGrid;
import com.wade.wadeapi.dto.*;
import org.springframework.web.client.HttpClientErrorException;
import com.wade.wadeapi.mapper.WeatherGridMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final RestTemplate restTemplate;
    private final WeatherGridMapper weatherGridMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${public.data.api-key:}")
    private String apiKey;

    private static final int DEFAULT_NX = 86;
    private static final int DEFAULT_NY = 96;

    private WeatherGrid resolveGrid(String stationId) {
        WeatherGrid grid = weatherGridMapper.findByStationId(stationId);
        if (grid != null) return grid;
        WeatherGrid fallback = new WeatherGrid();
        fallback.setNx(DEFAULT_NX);
        fallback.setNy(DEFAULT_NY);
        return fallback;
    }

    private static final String ULTRA_NCST_URL =
            "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst";
    private static final String ULTRA_SRT_FCST_URL =
            "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtFcst";
    private static final String VILAGE_FCST_URL =
            "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst";
    private static final String WEATHER_ALERT_URL =
            "https://apis.data.go.kr/1360000/WthrWrnInfoService/getWthrWrnMsg";
    private static final String RADAR_IMG_URL =
            "https://apis.data.go.kr/1360000/RadarImgInfoService/getCmpImg";
    private static final String SATELLITE_IMG_URL =
            "https://apis.data.go.kr/1360000/SatlitImgInfoService/getInsightSatlit";
    private static final String TYPHOON_INFO_URL =
            "https://apis.data.go.kr/1360000/TyphoonInfoService/getTyphoonInfo";

    // ── 현재 날씨 ─────────────────────────────────────────────────────────────
    public WeatherResponse getCurrentWeather(String stationId) {
        try {
            WeatherGrid grid = resolveGrid(stationId);
            return fetchWeatherFromApi(grid.getNx(), grid.getNy());
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("날씨 API 429 — 일일 요청 한도 초과");
            return WeatherResponse.builder()
                    .message("오늘 날씨 API 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.")
                    .build();
        } catch (Exception e) {
            log.error("날씨 API 실패: {}", e.getMessage());
            throw new RuntimeException("날씨 데이터를 가져올 수 없습니다: " + e.getMessage());
        }
    }

    private WeatherResponse fetchWeatherFromApi(int nx, int ny) throws Exception {
        WeatherResponse.WeatherResponseBuilder builder = WeatherResponse.builder()
                .measuredAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        String ncstDate = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String ncstTime = calcNcstBaseTime();

        String ncstUrl = UriComponentsBuilder.fromUriString(ULTRA_NCST_URL)
                .queryParam("serviceKey", apiKey)
                .queryParam("numOfRows", 10)
                .queryParam("pageNo", 1)
                .queryParam("dataType", "JSON")
                .queryParam("base_date", ncstDate)
                .queryParam("base_time", ncstTime)
                .queryParam("nx", nx)
                .queryParam("ny", ny)
                .toUriString();

        String ncstRes = restTemplate.getForObject(ncstUrl, String.class);
        JsonNode ncstItems = objectMapper.readTree(ncstRes)
                .path("response").path("body").path("items").path("item");

        if (ncstItems.isArray()) {
            for (JsonNode item : ncstItems) {
                String cat = item.path("category").asText();
                String val = item.path("obsrValue").asText();
                switch (cat) {
                    case "T1H" -> builder.temperature(Double.parseDouble(val));
                    case "REH" -> builder.humidity(Integer.parseInt(val));
                    case "WSD" -> builder.windSpeed(Double.parseDouble(val));
                    case "VEC" -> builder.windDirection(parseWindDir(val));
                    case "PTY" -> builder.precipitationType(Integer.parseInt(val));
                }
            }
        }
        builder.fcstDate(ncstDate).fcstTime(ncstTime);

        String fcstBaseTime = calcVilageFcstBaseTime();
        String fcstUrl = UriComponentsBuilder.fromUriString(VILAGE_FCST_URL)
                .queryParam("serviceKey", apiKey)
                .queryParam("numOfRows", 200)
                .queryParam("pageNo", 1)
                .queryParam("dataType", "JSON")
                .queryParam("base_date", ncstDate)
                .queryParam("base_time", fcstBaseTime)
                .queryParam("nx", nx)
                .queryParam("ny", ny)
                .toUriString();

        String fcstRes = restTemplate.getForObject(fcstUrl, String.class);
        JsonNode fcstItems = objectMapper.readTree(fcstRes)
                .path("response").path("body").path("items").path("item");

        String targetFcstTime = String.format("%02d00", LocalDateTime.now().getHour());
        if (fcstItems.isArray()) {
            for (JsonNode item : fcstItems) {
                String cat = item.path("category").asText();
                String val = item.path("fcstValue").asText();
                String itemTime = item.path("fcstTime").asText();
                if (!targetFcstTime.equals(itemTime)) continue;
                switch (cat) {
                    case "POP" -> builder.rainProbability(Integer.parseInt(val));
                    case "SKY" -> builder.skyCondition(parseSky(val));
                }
            }
        }

        return builder.build();
    }

    // ── 단기예보 ──────────────────────────────────────────────────────────────
    public List<ShortForecastItem> getShortForecast(String stationId) {
        WeatherGrid grid = resolveGrid(stationId);
        int nx = grid.getNx(), ny = grid.getNy();
        try {
            String baseDate = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            String baseTime = calcVilageFcstBaseTime();

            String url = UriComponentsBuilder.fromUriString(VILAGE_FCST_URL)
                    .queryParam("serviceKey", apiKey)
                    .queryParam("numOfRows", 300)
                    .queryParam("pageNo", 1)
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", nx)
                    .queryParam("ny", ny)
                    .toUriString();

            String res = restTemplate.getForObject(url, String.class);
            JsonNode items = objectMapper.readTree(res)
                    .path("response").path("body").path("items").path("item");

            Map<String, ShortForecastItem.ShortForecastItemBuilder> map = new LinkedHashMap<>();

            if (items.isArray()) {
                for (JsonNode item : items) {
                    String date = item.path("fcstDate").asText();
                    String time = item.path("fcstTime").asText();
                    String key  = date + time;
                    String cat  = item.path("category").asText();
                    String val  = item.path("fcstValue").asText();

                    map.computeIfAbsent(key, k -> ShortForecastItem.builder()
                            .fcstDate(date).fcstTime(time));

                    ShortForecastItem.ShortForecastItemBuilder b = map.get(key);
                    switch (cat) {
                        case "TMP" -> b.temperature(Double.parseDouble(val));
                        case "SKY" -> b.skyCondition(parseSky(val));
                        case "POP" -> b.rainProbability(Integer.parseInt(val));
                        case "PTY" -> b.precipType(parsePrecipType(val));
                    }
                }
            }

            return map.values().stream()
                    .map(ShortForecastItem.ShortForecastItemBuilder::build)
                    .limit(16)
                    .toList();
        } catch (Exception e) {
            log.error("단기예보 API 실패: {}", e.getMessage());
            return List.of();
        }
    }

    // ── 초단기예보 ────────────────────────────────────────────────────────────
    public List<UltraShortForecastItem> getUltraShortForecast(String stationId) {
        WeatherGrid grid = resolveGrid(stationId);
        int nx = grid.getNx(), ny = grid.getNy();
        try {
            String baseDate = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            String baseTime = calcUltraSrtFcstBaseTime();

            String url = UriComponentsBuilder.fromUriString(ULTRA_SRT_FCST_URL)
                    .queryParam("serviceKey", apiKey)
                    .queryParam("numOfRows", 60)
                    .queryParam("pageNo", 1)
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", nx)
                    .queryParam("ny", ny)
                    .toUriString();

            String res = restTemplate.getForObject(url, String.class);
            JsonNode items = objectMapper.readTree(res)
                    .path("response").path("body").path("items").path("item");

            Map<String, UltraShortForecastItem.UltraShortForecastItemBuilder> map = new LinkedHashMap<>();

            if (items.isArray()) {
                for (JsonNode item : items) {
                    String date = item.path("fcstDate").asText();
                    String time = item.path("fcstTime").asText();
                    String key  = date + time;
                    String cat  = item.path("category").asText();
                    String val  = item.path("fcstValue").asText();

                    map.computeIfAbsent(key, k -> UltraShortForecastItem.builder()
                            .fcstDate(date).fcstTime(time));

                    UltraShortForecastItem.UltraShortForecastItemBuilder b = map.get(key);
                    switch (cat) {
                        case "T1H" -> b.temperature(Double.parseDouble(val));
                        case "SKY" -> b.skyCondition(parseSky(val));
                        case "PTY" -> b.precipType(parsePrecipType(val));
                        case "RN1" -> b.precipAmount(val);
                    }
                }
            }

            return map.values().stream()
                    .map(UltraShortForecastItem.UltraShortForecastItemBuilder::build)
                    .limit(6)
                    .toList();
        } catch (Exception e) {
            log.error("초단기예보 API 실패: {}", e.getMessage());
            return List.of();
        }
    }

    // ── 발령특보 ──────────────────────────────────────────────────────────────
    public List<WeatherAlertItem> getWeatherAlerts() {
        try {
            LocalDateTime now = LocalDateTime.now();
            String tmFc = now.format(DateTimeFormatter.ofPattern("yyyyMMddHH")) + "00";

            String url = UriComponentsBuilder.fromUriString(WEATHER_ALERT_URL)
                    .queryParam("serviceKey", apiKey)
                    .queryParam("numOfRows", 20)
                    .queryParam("pageNo", 1)
                    .queryParam("dataType", "JSON")
                    .queryParam("tmFc", tmFc)
                    .toUriString();

            String res = restTemplate.getForObject(url, String.class);
            JsonNode body = objectMapper.readTree(res).path("response").path("body");
            int totalCount = body.path("totalCount").asInt(0);
            if (totalCount == 0) return List.of();

            JsonNode items = body.path("items").path("item");
            List<WeatherAlertItem> result = new ArrayList<>();

            if (items.isArray()) {
                for (JsonNode item : items) {
                    result.add(WeatherAlertItem.builder()
                            .warnVar(item.path("warnVar").asText())
                            .warnLevel(item.path("warnLevel").asText())
                            .area(item.path("area").asText())
                            .title(item.path("title").asText())
                            .content(item.path("content").asText())
                            .issuedAt(item.path("tmFc").asText())
                            .build());
                }
            } else if (items.isObject()) {
                result.add(WeatherAlertItem.builder()
                        .warnVar(items.path("warnVar").asText())
                        .warnLevel(items.path("warnLevel").asText())
                        .area(items.path("area").asText())
                        .title(items.path("title").asText())
                        .content(items.path("content").asText())
                        .issuedAt(items.path("tmFc").asText())
                        .build());
            }
            return result;
        } catch (Exception e) {
            log.error("발령특보 API 실패: {}", e.getMessage());
            return List.of();
        }
    }

    // ── 레이더 합성영상 ───────────────────────────────────────────────────────
    public RadarImageResponse getRadarCompositeImage() {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

            String url = UriComponentsBuilder.fromUriString(RADAR_IMG_URL)
                    .queryParam("serviceKey", apiKey)
                    .queryParam("numOfRows", 1)
                    .queryParam("pageNo", 1)
                    .queryParam("dataType", "JSON")
                    .queryParam("data", "CMP_WRC")
                    .queryParam("time", today)
                    .toUriString();

            String res = restTemplate.getForObject(url, String.class);
            JsonNode body = objectMapper.readTree(res).path("response").path("body");
            JsonNode items = body.path("items").path("item");

            String imageUrl = extractSingleStringField(items, "rdr-img-file");
            if (imageUrl == null || imageUrl.isBlank()) {
                return RadarImageResponse.builder()
                        .measuredAt(today)
                        .build();
            }

            return RadarImageResponse.builder()
                    .imageUrl(imageUrl.trim())
                    .measuredAt(today)
                    .build();
        } catch (Exception e) {
            log.error("레이더 합성영상 API 실패: {}", e.getMessage());
            return RadarImageResponse.builder().build();
        }
    }

    // ── 위성영상 ──────────────────────────────────────────────────────────────
    public SatelliteImageResponse getSatelliteImage() {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

            String url = UriComponentsBuilder.fromUriString(SATELLITE_IMG_URL)
                    .queryParam("serviceKey", apiKey)
                    .queryParam("numOfRows", 1)
                    .queryParam("pageNo", 1)
                    .queryParam("dataType", "JSON")
                    .queryParam("sat", "G2")
                    .queryParam("data", "ir105")
                    .queryParam("area", "ko")
                    .queryParam("time", today)
                    .toUriString();

            String res = restTemplate.getForObject(url, String.class);
            JsonNode body = objectMapper.readTree(res).path("response").path("body");
            JsonNode items = body.path("items").path("item");

            String imageUrl = extractSingleStringField(items, "satImgC-file");
            if (imageUrl == null || imageUrl.isBlank()) {
                return SatelliteImageResponse.builder()
                        .measuredAt(today)
                        .build();
            }

            return SatelliteImageResponse.builder()
                    .imageUrl(imageUrl.trim())
                    .measuredAt(today)
                    .build();
        } catch (Exception e) {
            log.error("위성영상 API 실패: {}", e.getMessage());
            return SatelliteImageResponse.builder().build();
        }
    }

    // ── 태풍 정보 ─────────────────────────────────────────────────────────────
    public TyphoonInfoResponse getTyphoonInfo() {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

            String url = UriComponentsBuilder.fromUriString(TYPHOON_INFO_URL)
                    .queryParam("serviceKey", apiKey)
                    .queryParam("numOfRows", 1)
                    .queryParam("pageNo", 1)
                    .queryParam("dataType", "JSON")
                    .queryParam("fromTmFc", today)
                    .queryParam("toTmFc", today)
                    .toUriString();

            String res = restTemplate.getForObject(url, String.class);
            JsonNode body = objectMapper.readTree(res).path("response").path("body");
            int totalCount = body.path("totalCount").asInt(0);

            if (totalCount == 0) {
                return TyphoonInfoResponse.builder().active(false).build();
            }

            JsonNode items = body.path("items").path("item");
            JsonNode item = items.isArray() ? items.get(0) : items;

            return TyphoonInfoResponse.builder()
                    .active(true)
                    .imageUrl(item.path("img").asText())
                    .name(item.path("typName").asText())
                    .nameEn(item.path("typEn").asText())
                    .location(item.path("typLoc").asText())
                    .maxWindSpeed(parseDouble(item.path("typWs").asText()))
                    .pressure(parseDouble(item.path("typPs").asText()))
                    .direction(item.path("typDir").asText())
                    .moveSpeed(parseDouble(item.path("typSp").asText()))
                    .measuredAt(item.path("tmFc").asText())
                    .build();
        } catch (Exception e) {
            log.error("태풍 정보 API 실패: {}", e.getMessage());
            return TyphoonInfoResponse.builder().active(false).build();
        }
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────
    private String extractSingleStringField(JsonNode items, String fieldName) {
        if (items.isArray() && items.size() > 0) {
            JsonNode field = items.get(0).path(fieldName);
            if (field.isArray() && field.size() > 0) {
                return field.get(field.size() - 1).asText();
            }
            return field.asText();
        } else if (items.isObject()) {
            JsonNode field = items.path(fieldName);
            if (field.isArray() && field.size() > 0) {
                return field.get(field.size() - 1).asText();
            }
            return field.asText();
        }
        return null;
    }

    private Double parseDouble(String val) {
        try { return Double.parseDouble(val); } catch (Exception e) { return null; }
    }

    private String calcNcstBaseTime() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        if (now.getMinute() < 10) hour = (hour == 0) ? 23 : hour - 1;
        return String.format("%02d00", hour);
    }

    // getUltraSrtFcst: 매시간 30분 발표, 45분 이후 조회 가능
    private String calcUltraSrtFcstBaseTime() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        // 45분 이전이면 이전 시간대 사용
        if (now.getMinute() < 45) hour = (hour == 0) ? 23 : hour - 1;
        return String.format("%02d30", hour);
    }

    private String calcVilageFcstBaseTime() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();
        int[] bases = {2, 5, 8, 11, 14, 17, 20, 23};
        int selected = 23;
        for (int i = bases.length - 1; i >= 0; i--) {
            if (hour > bases[i] || (hour == bases[i] && minute >= 10)) {
                selected = bases[i];
                break;
            }
        }
        return String.format("%02d00", selected);
    }

    private String parseSky(String code) {
        return switch (code) {
            case "1" -> "맑음";
            case "3" -> "구름많음";
            case "4" -> "흐림";
            default  -> "맑음";
        };
    }

    private String parsePrecipType(String code) {
        return switch (code) {
            case "0" -> "없음";
            case "1" -> "비";
            case "2" -> "비·눈";
            case "3" -> "눈";
            case "4" -> "소나기";
            case "5" -> "빗방울";
            case "6" -> "빗방울눈날림";
            case "7" -> "눈날림";
            default  -> "없음";
        };
    }

    private String parseWindDir(String value) {
        try {
            int deg = (int) Double.parseDouble(value);
            String[] dirs = {"북", "북동", "동", "남동", "남", "남서", "서", "북서"};
            return dirs[((deg + 22) % 360) / 45];
        } catch (Exception e) {
            return "북";
        }
    }
}
