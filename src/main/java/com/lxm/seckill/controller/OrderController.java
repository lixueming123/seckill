package com.lxm.seckill.controller;


import com.lxm.seckill.config.AccessLimit;
import com.lxm.seckill.entity.User;
import com.lxm.seckill.service.OrderService;
import com.lxm.seckill.vo.OrderVo;
import com.lxm.seckill.vo.RespBean;
import com.lxm.seckill.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lxm
 * @since 2021-08-07
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    OrderService orderService;

    @GetMapping("/detail/{orderId}")
    @AccessLimit
    public RespBean getOrder(@PathVariable Long orderId, User user) {
        if (user == null) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }

        OrderVo orderVo = orderService.getOrderDetail(orderId);
        return RespBean.success(orderVo);
    }

}
