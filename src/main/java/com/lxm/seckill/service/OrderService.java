package com.lxm.seckill.service;

import com.lxm.seckill.entity.Order;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lxm.seckill.entity.User;
import com.lxm.seckill.vo.GoodsVo;
import com.lxm.seckill.vo.OrderVo;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lxm
 * @since 2021-08-07
 */
public interface OrderService extends IService<Order> {

    Order seckillOrder(User user, GoodsVo goodsVo);

    OrderVo getOrderDetail(Long orderId);

    String createPath(User user, Long goodsId);

    boolean checkPath(User user, Long goodsId, String path);
}
