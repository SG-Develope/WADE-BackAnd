package com.wade.wadeapi.controller;

import com.wade.wadeapi.domain.CampingSite;
import com.wade.wadeapi.mapper.CampingSiteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/camping-sites")
@RequiredArgsConstructor
public class CampingSiteController {

    private final CampingSiteMapper campingSiteMapper;

    /** GET /api/camping-sites — 구미·칠곡 캠핑장 목록 */
    @GetMapping
    public ResponseEntity<List<CampingSite>> getAll() {
        return ResponseEntity.ok(campingSiteMapper.findAll());
    }
}
