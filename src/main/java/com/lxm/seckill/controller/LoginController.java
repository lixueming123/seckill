package com.lxm.seckill.controller;

import com.lxm.seckill.config.AccessLimit;
import com.lxm.seckill.entity.User;
import com.lxm.seckill.exception.GlobalException;
import com.lxm.seckill.service.UserService;
import com.lxm.seckill.utils.CookieUtil;
import com.lxm.seckill.vo.LoginVo;
import com.lxm.seckill.vo.RespBean;
import com.lxm.seckill.vo.RespBeanEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@Controller
@Slf4j
@RequestMapping("/login")
public class LoginController {

    @Autowired
    UserService userService;

    @Autowired
    RedisTemplate<String,Object> redisTemplate;

    @GetMapping
    public String toLogin() {
        return "login";
    }

    /**
     * 登录功能
     * @param loginVo LOGIN_DTO
     */
    @PostMapping("/doLogin")
    @ResponseBody
    public RespBean doLogin(@Valid LoginVo loginVo, HttpServletRequest req, HttpServletResponse resp) throws GlobalException {
        // log.info("{}", loginVo);
        return userService.doLogin(loginVo, req, resp);
    }

    @GetMapping("/logout")
    @AccessLimit
    @ResponseBody
    public RespBean logout(User user, HttpServletRequest req, HttpServletResponse resp) {
        String ticket = CookieUtil.getCookieValue(req, "userTicket");
        if (!StringUtils.hasText(ticket)) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }

        if (user == null) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }

        redisTemplate.delete("user:" + ticket);
        CookieUtil.deleteCookie(req, resp, "userTicket");
        return RespBean.success();
    }
}
