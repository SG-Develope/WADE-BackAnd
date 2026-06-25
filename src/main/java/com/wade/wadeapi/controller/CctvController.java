package com.wade.wadeapi.controller;

import com.wade.wadeapi.domain.Cctv;
import com.wade.wadeapi.mapper.CctvMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cctv")
@RequiredArgsConstructor
public class CctvController {

    private final CctvMapper cctvMapper;

    @GetMapping
    public ResponseEntity<List<Cctv>> getAll() {
        return ResponseEntity.ok(cctvMapper.findAll());
    }

    @GetMapping("/station/{stationId}")
    public ResponseEntity<List<Cctv>> getByStation(@PathVariable String stationId) {
        return ResponseEntity.ok(cctvMapper.findByStationId(stationId));
    }
}
