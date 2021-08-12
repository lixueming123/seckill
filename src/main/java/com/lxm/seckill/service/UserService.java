package com.lxm.seckill.service;

import com.lxm.seckill.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lxm.seckill.exception.GlobalException;
import com.lxm.seckill.vo.LoginVo;
import com.lxm.seckill.vo.RespBean;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lxm
 * @since 2021-08-06
 */
public interface UserService extends IService<User> {

    RespBean doLogin(LoginVo loginVo, HttpServletRequest req, HttpServletResponse resp);

    User getUserByCookie(String ticket, HttpServletRequest request, HttpServletResponse response);
}
