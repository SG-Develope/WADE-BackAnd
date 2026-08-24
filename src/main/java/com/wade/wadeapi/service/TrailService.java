package com.wade.wadeapi.service;

import com.wade.wadeapi.mapper.TrailMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 산책로 경로 좌표 서비스.
 * durunubi 를 런타임에 호출하지 않고, DB(gpx_xml)에 직접 저장된 GPX 원문(XML)을
 * 파싱해 좌표 배열로 반환한다. 파싱 결과는 Caffeine("trailPath")에 캐시.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrailService {

    private final TrailMapper trailMapper;

    /** 화면 렌더링용 최대 좌표 수 (초과 시 균등 다운샘플링) */
    private static final int MAX_POINTS = 800;

    /** courseId 의 GPX 경로를 [[위도, 경도], ...] 로 반환. 없으면 빈 리스트. */
    @Cacheable(value = "trailPath", key = "#p0")
    public List<double[]> getPath(String courseId) {
        String xml = trailMapper.findGpxXml(courseId);
        if (xml == null || xml.isBlank()) {
            log.warn("GPX 원문 없음: {}", courseId);
            return List.of();
        }
        try {
            return parseGpx(xml.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("GPX 파싱 실패 [{}]: {}", courseId, e.getMessage());
            return List.of();
        }
    }

    /** GPX(XML) → 좌표 리스트. trkpt → rtept → wpt 순으로 탐색. */
    private List<double[]> parseGpx(byte[] xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        // XXE 방지
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml));

        NodeList pts = doc.getElementsByTagNameNS("*", "trkpt");
        if (pts.getLength() == 0) pts = doc.getElementsByTagNameNS("*", "rtept");
        if (pts.getLength() == 0) pts = doc.getElementsByTagNameNS("*", "wpt");

        List<double[]> raw = new ArrayList<>();
        for (int i = 0; i < pts.getLength(); i++) {
            Element el = (Element) pts.item(i);
            String lat = el.getAttribute("lat");
            String lon = el.getAttribute("lon");
            if (lat.isBlank() || lon.isBlank()) continue;
            try {
                raw.add(new double[]{ Double.parseDouble(lat), Double.parseDouble(lon) });
            } catch (NumberFormatException ignored) { }
        }
        return downsample(raw);
    }

    /** 좌표가 너무 많으면 균등 간격으로 솎아낸다(양 끝점 유지). */
    private List<double[]> downsample(List<double[]> pts) {
        int n = pts.size();
        if (n <= MAX_POINTS) return pts;
        List<double[]> out = new ArrayList<>(MAX_POINTS);
        double step = (double) (n - 1) / (MAX_POINTS - 1);
        for (int i = 0; i < MAX_POINTS; i++) {
            out.add(pts.get((int) Math.round(i * step)));
        }
        return out;
    }
}
