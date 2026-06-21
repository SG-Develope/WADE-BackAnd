package com.wade.wadeapi.controller;

import com.wade.wadeapi.domain.Place;
import com.wade.wadeapi.mapper.PlaceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceMapper placeMapper;

    @GetMapping
    public ResponseEntity<List<Place>> getAll() {
        return ResponseEntity.ok(placeMapper.findAll());
    }
}
