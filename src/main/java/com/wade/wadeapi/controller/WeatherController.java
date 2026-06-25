package com.wade.wadeapi.controller;

import com.wade.wadeapi.dto.*;
import com.wade.wadeapi.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping("/current")
    public ResponseEntity<WeatherResponse> getCurrent(
            @RequestParam(name = "stationId") String stationId) {
        return ResponseEntity.ok(weatherService.getCurrentWeather(stationId));
    }

    @GetMapping("/forecast/short")
    public ResponseEntity<List<ShortForecastItem>> getShortForecast(
            @RequestParam(name = "stationId") String stationId) {
        return ResponseEntity.ok(weatherService.getShortForecast(stationId));
    }

    @GetMapping("/forecast/ultrashort")
    public ResponseEntity<List<UltraShortForecastItem>> getUltraShortForecast(
            @RequestParam(name = "stationId") String stationId) {
        return ResponseEntity.ok(weatherService.getUltraShortForecast(stationId));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<WeatherAlertItem>> getAlerts() {
        return ResponseEntity.ok(weatherService.getWeatherAlerts());
    }

    @GetMapping("/radar/composite")
    public ResponseEntity<RadarImageResponse> getRadarComposite() {
        return ResponseEntity.ok(weatherService.getRadarCompositeImage());
    }

    @GetMapping("/satellite")
    public ResponseEntity<SatelliteImageResponse> getSatellite() {
        return ResponseEntity.ok(weatherService.getSatelliteImage());
    }

    @GetMapping("/typhoon")
    public ResponseEntity<TyphoonInfoResponse> getTyphoon() {
        return ResponseEntity.ok(weatherService.getTyphoonInfo());
    }
}
