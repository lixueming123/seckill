package com.lxm.seckill.vo;

import com.lxm.seckill.entity.Order;
import com.lxm.seckill.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResult {
    private OrderVo orderVo;
    private String userNickName;
}
