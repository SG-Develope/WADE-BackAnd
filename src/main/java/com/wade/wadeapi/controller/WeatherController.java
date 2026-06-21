package com.wade.wadeapi.controller;

import com.wade.wadeapi.dto.*;
import com.wade.wadeapi.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @Value("${wade.weather.gumi.nx:86}")
    private int defaultNx;

    @Value("${wade.weather.gumi.ny:96}")
    private int defaultNy;

    @GetMapping("/current")
    public ResponseEntity<WeatherResponse> getCurrent(
            @RequestParam(name = "nx", defaultValue = "0") int nx,
            @RequestParam(name = "ny", defaultValue = "0") int ny) {
        int resolvedNx = nx > 0 ? nx : defaultNx;
        int resolvedNy = ny > 0 ? ny : defaultNy;
        return ResponseEntity.ok(weatherService.getCurrentWeather(resolvedNx, resolvedNy));
    }

    @GetMapping("/forecast/short")
    public ResponseEntity<List<ShortForecastItem>> getShortForecast(
            @RequestParam(name = "nx", defaultValue = "0") int nx,
            @RequestParam(name = "ny", defaultValue = "0") int ny) {
        int resolvedNx = nx > 0 ? nx : defaultNx;
        int resolvedNy = ny > 0 ? ny : defaultNy;
        return ResponseEntity.ok(weatherService.getShortForecast(resolvedNx, resolvedNy));
    }

    @GetMapping("/forecast/ultrashort")
    public ResponseEntity<List<UltraShortForecastItem>> getUltraShortForecast(
            @RequestParam(name = "nx", defaultValue = "0") int nx,
            @RequestParam(name = "ny", defaultValue = "0") int ny) {
        int resolvedNx = nx > 0 ? nx : defaultNx;
        int resolvedNy = ny > 0 ? ny : defaultNy;
        return ResponseEntity.ok(weatherService.getUltraShortForecast(resolvedNx, resolvedNy));
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
