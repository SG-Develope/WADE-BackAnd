package com.wade.wadeapi.mapper;

import com.wade.wadeapi.domain.Station;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StationMapper {
    List<Station> findAll();
    Station findById(@Param("id") String id);
}
