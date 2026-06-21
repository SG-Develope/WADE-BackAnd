package com.wade.wadeapi.controller;

import com.wade.wadeapi.dto.AiGuideRequest;
import com.wade.wadeapi.dto.AiGuideResponse;
import com.wade.wadeapi.service.AiGuideService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiGuideController {

    private final AiGuideService aiGuideService;

    @PostMapping("/safety-guide")
    public ResponseEntity<AiGuideResponse> getSafetyGuide(@RequestBody AiGuideRequest request) {
        return ResponseEntity.ok(aiGuideService.getSafetyGuide(request.getStationLevels()));
    }
}
