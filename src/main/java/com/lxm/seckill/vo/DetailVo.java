package com.lxm.seckill.vo;

import com.lxm.seckill.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetailVo {
    private GoodsVo goodsVo;

    private User user;

    private Integer secKillStatus;

    private Integer remainSeconds;
}
