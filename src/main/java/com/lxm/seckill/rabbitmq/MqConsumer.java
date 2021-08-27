package com.lxm.seckill.rabbitmq;

import com.lxm.seckill.config.RabbitConfig;
import com.lxm.seckill.controller.SeckillController;
import com.lxm.seckill.entity.SeckillMessage;
import com.lxm.seckill.entity.SeckillOrder;
import com.lxm.seckill.exception.GlobalException;
import com.lxm.seckill.service.GoodsService;
import com.lxm.seckill.service.OrderService;
import com.lxm.seckill.utils.Const;
import com.lxm.seckill.vo.GoodsVo;
import com.lxm.seckill.vo.RespBeanEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class MqConsumer {

    @Autowired
    GoodsService goodsService;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Autowired
    OrderService orderService;

    public static AtomicInteger enterCnt = new AtomicInteger(0);
    public static AtomicInteger cnt = new AtomicInteger(0);

    @RabbitListener(queues = RabbitConfig.SEC_QUEUE)
    public void receiveSeckillMessage(Message<SeckillMessage> message) {
        enterCnt.getAndIncrement();
        log.info("消费者接受消息:{}", message);
        SeckillMessage payload = message.getPayload();

        // 库存判断
        GoodsVo goods = goodsService.findGoodsVoById(payload.getGoodsId());
        if (goods.getStockCount() < 1) {
            return;
        }

        // 重复抢购
        Object order = redisTemplate.opsForValue().get("order:" + payload.getUser().getId() + ":" + goods.getId());
        if (order != null) {
            return;
        }
        // 下单
        try {
            orderService.seckillOrder(payload.getUser(), goods);
        }
        catch (DuplicateKeyException e) {
            log.warn("用户重复抢购，用户id为:{}", payload.getUser().getId());
            redisTemplate.opsForValue().increment(Const.SECKILL_GOODS_PREFIX + goods.getId());
            SeckillController.emptyStock.put(goods.getId(), false);
        }
        catch (GlobalException e) {
            log.warn("库存不足");
            redisTemplate.opsForValue().increment(Const.SECKILL_GOODS_PREFIX + goods.getId());
            SeckillController.emptyStock.put(goods.getId(), false);
        }

        cnt.getAndIncrement();
    }
}
