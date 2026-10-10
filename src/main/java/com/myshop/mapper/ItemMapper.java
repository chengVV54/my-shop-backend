package com.myshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myshop.entity.Item;
import org.apache.ibatis.annotations.Mapper;

/**
 * 物品表 Mapper，通用 CRUD 由 MyBatis-Plus 提供的 {@link BaseMapper} 实现。
 */
@Mapper
public interface ItemMapper extends BaseMapper<Item> {
}
