package com.wade.wadeapi.mapper;

import com.wade.wadeapi.domain.WeatherGrid;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WeatherGridMapper {
    List<WeatherGrid> findAll();
    WeatherGrid findByStationId(String stationId);
}
