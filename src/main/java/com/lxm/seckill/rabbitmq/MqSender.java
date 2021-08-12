package com.lxm.seckill.rabbitmq;

import com.lxm.seckill.config.RabbitConfig;
import com.lxm.seckill.entity.SeckillMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MqSender {

    @Autowired
    RabbitTemplate rabbitTemplate;

    public void send(Object message) {
        log.info("发送消息{}", message);
        rabbitTemplate.convertAndSend("queue", message);
    }

    public void sendSeckillMessage(SeckillMessage seckillMessage) {
        rabbitTemplate.convertAndSend(RabbitConfig.SEC_EXCHANGE,
                "seckill.message",
                seckillMessage);
    }

}
