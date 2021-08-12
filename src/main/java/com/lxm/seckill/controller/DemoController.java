package com.lxm.seckill.controller;

import cn.hutool.core.map.MapBuilder;
import com.lxm.seckill.entity.User;
import com.lxm.seckill.interceptor.LoginInterceptor;
import com.lxm.seckill.rabbitmq.MqConsumer;
import com.lxm.seckill.rabbitmq.MqSender;
import com.lxm.seckill.vo.LoginVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/demo")
public class DemoController {

    @Autowired
    MqSender mqSender;

    @PostMapping("/hello")
    public LoginVo hello(@RequestBody LoginVo loginVo) {
        loginVo.setPassword("789456");
        return loginVo;
    }

    @GetMapping("/mq")
    public boolean mq() {
        mqSender.send("hello world");
        return true;
    }

    @GetMapping("/mq/count")
    public Map<Object, Object> count() {

        return MapBuilder.create()
                .put("enterCnt", MqConsumer.enterCnt)
                .put("finishCnt", MqConsumer.cnt)
                .map();
    }
}
