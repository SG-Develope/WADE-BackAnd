package com.wade.wadeapi.controller;

import com.wade.wadeapi.domain.StationMeta;
import com.wade.wadeapi.dto.WaterLevelCurrentResponse;
import com.wade.wadeapi.dto.WaterLevelHistoryResponse;
import com.wade.wadeapi.mapper.StationMetaMapper;
import com.wade.wadeapi.service.WaterLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/water-levels")
@RequiredArgsConstructor
public class WaterLevelController {

    private final WaterLevelService waterLevelService;
    private final StationMetaMapper stationMetaMapper;

    @GetMapping("/meta")
    public ResponseEntity<List<StationMeta>> getMeta() {
        return ResponseEntity.ok(stationMetaMapper.findAll());
    }

    @GetMapping("/current")
    public ResponseEntity<WaterLevelCurrentResponse> getCurrent() {
        return ResponseEntity.ok(waterLevelService.getCurrent());
    }

    @GetMapping("/history/{stationId}")
    public ResponseEntity<WaterLevelHistoryResponse> getHistory(
            @PathVariable("stationId") String stationId,
            @RequestParam(name = "hours", defaultValue = "24") int hours) {
        return ResponseEntity.ok(waterLevelService.getHistory(stationId, hours));
    }
}
