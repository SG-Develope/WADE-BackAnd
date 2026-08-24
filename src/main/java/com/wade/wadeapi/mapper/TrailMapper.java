package com.wade.wadeapi.mapper;

import com.wade.wadeapi.domain.Trail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface TrailMapper {

    /** 산책로 코스 목록 (GPX 원문 제외 — 가벼운 메타데이터만) */
    List<Trail> findAll();

    /** 코스의 GPX 원문(XML) — gpx_xml 컬럼에 직접 저장된 값 */
    String findGpxXml(@Param("courseId") String courseId);
}
