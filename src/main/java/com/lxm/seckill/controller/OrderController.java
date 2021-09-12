package com.lxm.seckill.controller;


import com.lxm.seckill.config.AccessLimit;
import com.lxm.seckill.entity.Order;
import com.lxm.seckill.entity.User;
import com.lxm.seckill.exception.GlobalException;
import com.lxm.seckill.service.OrderService;
import com.lxm.seckill.vo.OrderResult;
import com.lxm.seckill.vo.OrderVo;
import com.lxm.seckill.vo.RespBean;
import com.lxm.seckill.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("admin/list")
    @AccessLimit
    public RespBean getOrderResultList(User user) {
        if (user.getId() != 13212345678L) {
            return RespBean.error(RespBeanEnum.REQUEST_ILLEGAL);
        }
        List<OrderResult> orderList = orderService.getOrderList();
        return RespBean.success(orderList);
    }

    @GetMapping("/pay/{orderId}")
    @AccessLimit
    public RespBean pay(@PathVariable("orderId") Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new GlobalException(RespBeanEnum.ORDER_NOT_EXIST);
        }

        order.setStatus(1);
        orderService.saveOrUpdate(order);
        return RespBean.success("支付成功");
    }

}
