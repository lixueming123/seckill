package com.lxm.seckill.service.impl;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.lxm.seckill.entity.User;
import com.lxm.seckill.exception.GlobalException;
import com.lxm.seckill.mapper.UserMapper;
import com.lxm.seckill.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lxm.seckill.utils.CookieUtil;
import com.lxm.seckill.utils.MD5Utils;
import com.lxm.seckill.utils.UUIDUtil;
import com.lxm.seckill.utils.ValidatorUtil;
import com.lxm.seckill.vo.LoginVo;
import com.lxm.seckill.vo.RespBean;
import com.lxm.seckill.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author lxm
 * @since 2021-08-06
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {



    @Autowired
    RedisTemplate<String,Object> redisTemplate;

    /**
     * 登录方法
     * @param loginVo loginVo
     */
    @Override
    public RespBean doLogin(LoginVo loginVo, HttpServletRequest req, HttpServletResponse resp) {
        String mobile = loginVo.getMobile();
        String password = loginVo.getPassword();

        User user = this.getById(mobile);
        if (user == null) {
            throw new GlobalException(RespBeanEnum.LOGIN_ERROR);
        }

        // 校验密码
        if (!MD5Utils.formPassToDBPass(password, user.getSalt()).equals(user.getPassword())) {
            throw new GlobalException(RespBeanEnum.LOGIN_ERROR);
        }

        // 生成cookie
        String ticket = UUIDUtil.uuid();
//        HttpSession session = req.getSession();
//        session.setAttribute(ticket, user);

        // 将用户信息存入redis中
        redisTemplate.opsForValue().set("user:" + ticket, user, 3, TimeUnit.DAYS);
        redisTemplate.opsForValue().set("user:" + ticket, user);
        CookieUtil.setCookie(req, resp,"userTicket", ticket, 3 * 24 * 60 * 60);
        return RespBean.success();
    }

    @Override
    public User getUserByCookie(String ticket, HttpServletRequest request, HttpServletResponse response) {
        if (StringUtils.isBlank(ticket)) {
            return null;
        }

        User user = (User) redisTemplate.opsForValue().get("user:" + ticket);

        if (user != null) {
            redisTemplate.expire("user:" + ticket, 7, TimeUnit.DAYS);
            CookieUtil.setCookie(request, response, "userTicket", ticket, 7 * 24 * 60 * 60);
        }
        return user;
    }
}
