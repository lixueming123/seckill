package com.lxm.seckill.service;

import com.lxm.seckill.entity.Goods;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lxm.seckill.vo.GoodsVo;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lxm
 * @since 2021-08-07
 */
public interface GoodsService extends IService<Goods> {

    List<GoodsVo> findGoodsVoList();

    GoodsVo findGoodsVoById(Long goodsId);
}
