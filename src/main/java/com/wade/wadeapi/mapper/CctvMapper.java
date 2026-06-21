package com.wade.wadeapi.mapper;

import com.wade.wadeapi.domain.Cctv;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface CctvMapper {
    List<Cctv> findAll();
}
