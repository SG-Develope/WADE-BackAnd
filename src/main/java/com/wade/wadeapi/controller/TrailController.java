package com.wade.wadeapi.controller;

import com.wade.wadeapi.domain.Trail;
import com.wade.wadeapi.mapper.TrailMapper;
import com.wade.wadeapi.service.TrailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trails")
@RequiredArgsConstructor
public class TrailController {

    private final TrailMapper trailMapper;
    private final TrailService trailService;

    /** GET /api/trails — 산책로 코스 목록(전국). 지역 필터는 프론트에서 sido 로 처리 */
    @GetMapping
    public ResponseEntity<List<Trail>> getAll() {
        return ResponseEntity.ok(trailMapper.findAll());
    }

    /**
     * GET /api/trails/{courseId}/path — 코스 GPX 경로 좌표
     * 응답: [[위도, 경도], ...]  (DB에 저장된 GPX 원문 파싱 + 캐시)
     */
    @GetMapping("/{courseId}/path")
    public ResponseEntity<List<double[]>> getPath(@PathVariable("courseId") String courseId) {
        return ResponseEntity.ok(trailService.getPath(courseId));
    }
}
