package com.lxm.seckill.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lxm.seckill.config.AccessLimit;
import com.lxm.seckill.entity.Order;
import com.lxm.seckill.entity.SeckillMessage;
import com.lxm.seckill.entity.SeckillOrder;
import com.lxm.seckill.entity.User;
import com.lxm.seckill.rabbitmq.MqSender;
import com.lxm.seckill.service.GoodsService;
import com.lxm.seckill.service.OrderService;
import com.lxm.seckill.service.SeckillOrderService;
import com.lxm.seckill.utils.Const;
import com.lxm.seckill.vo.GoodsVo;
import com.lxm.seckill.vo.RespBean;
import com.lxm.seckill.vo.RespBeanEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/seckill")
@Slf4j
public class SeckillController implements InitializingBean {

    @Autowired
    SeckillOrderService seckillOrderService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Autowired
    MqSender mqSender;

    public static ConcurrentHashMap<Long, Boolean> emptyStock = new ConcurrentHashMap<>();


    /**
     * 秒杀接口
     * 700 (windows)
     * redis 1800
     * 内存标记 + mq下单 3000
     * 250 (linux)
     * @param goodsId 商品id
     * @param user    用户
     * @return RespBean
     */
    @PostMapping("/test")
    @AccessLimit
    public RespBean seckill(@RequestParam("goodsId") Long goodsId,
                            User user) {
        // 内存标记判断库存
        if (emptyStock.get(goodsId)) {
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }

        // 判断是否重复抢购

//        SeckillOrder seckillOrder = seckillOrderService.getOne(
//                new QueryWrapper<SeckillOrder>()
//                        .eq("user_id", user.getId())
//                        .eq("goods_id", goodsId));

        SeckillOrder seckillOrder = (SeckillOrder)
                redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);

        if (seckillOrder != null) {
            return RespBean.error(RespBeanEnum.REPEAT_ERROR);
        }
        // 判断库存

//        GoodsVo goodsVo = goodsService.findGoodsVoById(goodsId);
//        if (goodsVo.getStockCount() < 1) {
//            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
//        }

        Long stock = redisTemplate.opsForValue().decrement(Const.SECKILL_GOODS_PREFIX + goodsId);
        assert stock != null;
        if (stock < 0) {
            redisTemplate.opsForValue().increment(Const.SECKILL_GOODS_PREFIX + goodsId);
            emptyStock.put(goodsId, true);
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }

        // mq下单操作
        SeckillMessage seckillMessage = new SeckillMessage();
        seckillMessage.setGoodsId(goodsId);
        seckillMessage.setUser(user);
        mqSender.sendSeckillMessage(seckillMessage);

        return RespBean.success(0);
    }

    /**
     * 秒杀接口
     * @param goodsId   商品id
     * @param path      秒杀路径
     * @param user      用户
     * @return          ..
     */
    @PostMapping("/{path}")
    @AccessLimit
    public RespBean safeSeckill(@RequestParam("goodsId") Long goodsId, @PathVariable String path, User user) {
        boolean check = orderService.checkPath(user, goodsId, path);
        if (!check) {
            return RespBean.error(RespBeanEnum.REQUEST_ILLEGAL);
        }
        return seckill(goodsId, user);
    }

    /**
     * 获取秒杀路径
     * @param goodsId   商品id
     * @param user      用户
     * @return          ..
     */
    @GetMapping("/path/{goodsId}")
    @AccessLimit(seconds = 60, maxCount = 10)
    public RespBean getPath(@PathVariable("goodsId") Long goodsId, User user) {
        String path = orderService.createPath(user, goodsId);
        return RespBean.success(path);
    }

    /**
     * 获取秒杀结果
     * @param goodsId   商品id
     * @param user      用户
     * @return          ..
     */
    @GetMapping("/result")
    @AccessLimit
    public RespBean getResult(Long goodsId, User user) {
        if (user == null) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
        // 库存判断
        if (redisTemplate.opsForValue().get(Const.IS_STOCK_EMPTY_PREFIX + goodsId) != null) {
            return RespBean.success(-1L);
        }

        SeckillOrder order = seckillOrderService.getOne(new QueryWrapper<SeckillOrder>()
                .eq("goods_id", goodsId)
                .eq("user_id", user.getId()));

        // 订单存在直接返回
        if (order != null) {
            return RespBean.success(order.getOrderId() + "");
        }

        return RespBean.success(0L);
    }

    /**
     * Redis 预加载库存
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> list = goodsService.findGoodsVoList();
        if (CollectionUtil.isEmpty(list)) {
            return;
        }

        for (GoodsVo goodsVo : list) {
            redisTemplate.opsForValue().set(Const.SECKILL_GOODS_PREFIX + goodsVo.getId(), goodsVo.getStockCount(),
                    goodsVo.getEndDate().getTime() - goodsVo.getStartDate().getTime() + 3000,
                    TimeUnit.MILLISECONDS);
            emptyStock.put(goodsVo.getId(), false);

            redisTemplate.delete(Const.IS_STOCK_EMPTY_PREFIX + goodsVo.getId());
        }
    }

    //@PostMapping
    @Deprecated
    public String seckill(Long goodsId, User user, Model model) {

        model.addAttribute("user", user);
        // 判断是否重复抢购
        SeckillOrder seckillOrder = seckillOrderService.getOne(
                new QueryWrapper<SeckillOrder>()
                        .eq("user_id", user.getId())
                        .eq("goods_id", goodsId));

        if (seckillOrder != null) {
            model.addAttribute("errmsg", RespBeanEnum.REPEAT_ERROR.getMessage());
            return "secKillFail";
        }

        // 判断库存
        GoodsVo goodsVo = goodsService.findGoodsVoById(goodsId);
        if (goodsVo.getStockCount() < 1) {
            model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK.getMessage());
            return "secKillFail";
        }

        model.addAttribute("goods", goodsVo);
        Order order = orderService.seckillOrder(user, goodsVo);
        model.addAttribute("order", order);
        return "orderDetail";
    }
}
