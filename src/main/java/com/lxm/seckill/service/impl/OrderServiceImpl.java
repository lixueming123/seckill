package com.lxm.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lxm.seckill.controller.OrderController;
import com.lxm.seckill.entity.Order;
import com.lxm.seckill.entity.SeckillGoods;
import com.lxm.seckill.entity.SeckillOrder;
import com.lxm.seckill.entity.User;
import com.lxm.seckill.exception.GlobalException;
import com.lxm.seckill.mapper.OrderMapper;
import com.lxm.seckill.service.*;
import com.lxm.seckill.utils.Const;
import com.lxm.seckill.utils.MD5Utils;
import com.lxm.seckill.vo.*;
import com.sun.org.apache.xpath.internal.operations.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author lxm
 * @since 2021-08-07
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    SeckillGoodsService seckillGoodsService;

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    SeckillOrderService seckillOrderService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    UserService userService;
    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Transactional
    @Override
    public Order seckillOrder(User user, GoodsVo goodsVo) {
        // 减库存
        SeckillGoods secGoods = seckillGoodsService.getOne(new QueryWrapper<SeckillGoods>()
                .eq("goods_id", goodsVo.getId()));
        if (secGoods.getStockCount() < 1) {
            redisTemplate.opsForValue().setIfAbsent(Const.IS_STOCK_EMPTY_PREFIX + secGoods.getId(), true,
                    secGoods.getEndDate().getTime() - secGoods.getStartDate().getTime() + 3000,
                    TimeUnit.MILLISECONDS);
            throw new GlobalException(RespBeanEnum.EMPTY_STOCK);
        }

        boolean update = seckillGoodsService.update(new UpdateWrapper<SeckillGoods>()
                .setSql("stock_count=stock_count-1")
                .eq("id", secGoods.getId())
                .gt("stock_count", 0));

        if (!update) {
            redisTemplate.opsForValue().setIfAbsent(Const.IS_STOCK_EMPTY_PREFIX + secGoods.getId(), true,
                    secGoods.getEndDate().getTime() - secGoods.getStartDate().getTime() + 3000,
                    TimeUnit.MILLISECONDS);
            throw new GlobalException(RespBeanEnum.EMPTY_STOCK);
        }

        // 生成订单
        Order order = new Order();
        order.setDeliveryAddrId(0L);
        order.setUserId(user.getId());
        order.setGoodsId(goodsVo.getId());
        order.setGoodsName(goodsVo.getGoodsName());
        order.setGoodsPrice(goodsVo.getSeckillPrice());
        order.setGoodsCount(1);
        order.setOrderChannel(1);
        order.setStatus(0);
        order.setCreateDate(new Date());
        orderMapper.insert(order);

        // 生成秒杀订单
        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setOrderId(order.getId());
        seckillOrder.setUserId(user.getId());
        seckillOrder.setGoodsId(goodsVo.getId());
        seckillOrderService.save(seckillOrder);
        // 将该订单加入redis
        redisTemplate.opsForValue().set("order:" + user.getId() + ":" + goodsVo.getId(), seckillOrder,
                secGoods.getEndDate().getTime() - order.getCreateDate().getTime() + 3000,
                TimeUnit.MILLISECONDS);

        return order;
    }

    @Override
    public List<OrderResult> getOrderList() {
        List<Order> list = this.list();
        List<OrderResult> orderResults = new ArrayList<>();
        for (Order order : list) {
            OrderResult orderResult = new OrderResult();
            OrderVo orderVo = new OrderVo();
            orderVo.setOrder(order);
            GoodsVo goodsVo = goodsService.findGoodsVoById(order.getGoodsId());
            User user = userService.getById(order.getUserId());
            orderVo.setGoods(goodsVo);
            orderResult.setOrderVo(orderVo);
            orderResult.setUserNickName(user.getNickname());
            orderResults.add(orderResult);
        }
        return orderResults;
    }

    @Override
    public OrderVo getOrderDetail(Long orderId) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new GlobalException(RespBeanEnum.ORDER_NOT_EXIST);
        }

        OrderVo orderVo = new OrderVo();
        orderVo.setOrder(order);
        GoodsVo goods = goodsService.findGoodsVoById(order.getGoodsId());
        orderVo.setGoods(goods);
        return orderVo;
    }

    @Override
    public String createPath(User user, Long goodsId) {
        if (user == null || goodsId < 0) {
            throw new GlobalException(RespBeanEnum.REQUEST_ILLEGAL);
        }
        String path =  MD5Utils.md5(user.getId() + goodsId + user.getSalt());
        redisTemplate.opsForValue().set(Const.SECKILL_PATH_PREFIX + user.getId() + ":" + goodsId,
                path, 60, TimeUnit.SECONDS);
        return path;
    }

    @Override
    public boolean checkPath(User user, Long goodsId, String path) {
        if (user == null || goodsId < 0 || !StringUtils.hasText(path)) {
            return false;
        }
        String p = (String) redisTemplate.opsForValue().get(Const.SECKILL_PATH_PREFIX + user.getId() + ":" + goodsId);
        return path.equals(p);
    }
}
