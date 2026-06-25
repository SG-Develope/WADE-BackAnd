package com.wade.wadeapi.controller;

import com.wade.wadeapi.domain.Station;
import com.wade.wadeapi.mapper.StationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
public class StationController {

    private final StationMapper stationMapper;

    @GetMapping
    public ResponseEntity<List<Station>> getAll() {
        return ResponseEntity.ok(stationMapper.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Station> getById(@PathVariable String id) {
        Station station = stationMapper.findById(id);
        if (station == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(station);
    }
}
