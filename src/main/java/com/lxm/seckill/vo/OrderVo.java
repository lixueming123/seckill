package com.lxm.seckill.vo;

import com.lxm.seckill.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderVo {

    private GoodsVo goods;

    private Order order;

}
