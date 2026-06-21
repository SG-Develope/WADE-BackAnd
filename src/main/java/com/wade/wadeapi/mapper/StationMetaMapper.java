package com.wade.wadeapi.mapper;

import com.wade.wadeapi.domain.StationMeta;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StationMetaMapper {
    List<StationMeta> findAll();
    StationMeta findById(@Param("id") String id);
}
