package com.myshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myshop.entity.BarterRequest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 换物申请表 Mapper，通用 CRUD 由 MyBatis-Plus 提供的 {@link BaseMapper} 实现。
 */
@Mapper
public interface BarterRequestMapper extends BaseMapper<BarterRequest> {
}
