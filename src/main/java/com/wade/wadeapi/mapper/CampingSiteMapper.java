package com.wade.wadeapi.mapper;

import com.wade.wadeapi.domain.CampingSite;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface CampingSiteMapper {

    /** 구미시·칠곡군 캠핑장 전체 조회 */
    List<CampingSite> findAll();
}
