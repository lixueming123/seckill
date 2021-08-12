package com.lxm.seckill.mapper;

import com.lxm.seckill.entity.Goods;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lxm.seckill.vo.GoodsVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author lxm
 * @since 2021-08-07
 */
public interface GoodsMapper extends BaseMapper<Goods> {

    List<GoodsVo> findGoodsVoList();

    GoodsVo findGoodsVoById(@Param("goodsId") Long goodsId);
}
