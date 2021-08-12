package com.lxm.seckill.vo;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

@Data
public class LoginVo {
    @NotBlank
    @IsMobile
    private String mobile;

    @NotBlank
    @Length(min = 32)
    private String password;
}
