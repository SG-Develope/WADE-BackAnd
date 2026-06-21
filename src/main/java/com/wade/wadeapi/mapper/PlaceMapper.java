package com.wade.wadeapi.mapper;

import com.wade.wadeapi.domain.Place;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface PlaceMapper {
    List<Place> findAll();
}
