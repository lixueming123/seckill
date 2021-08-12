package com.lxm.seckill.controller;

import com.lxm.seckill.exception.GlobalException;
import com.lxm.seckill.service.UserService;
import com.lxm.seckill.vo.LoginVo;
import com.lxm.seckill.vo.RespBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
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

    @GetMapping
    public String toLogin() {
        return "login";
    }

    /**
     * 登录功能
     * @param loginVo
     */
    @PostMapping("/doLogin")
    @ResponseBody
    public RespBean doLogin(@Valid LoginVo loginVo, HttpServletRequest req, HttpServletResponse resp) throws GlobalException {
        // log.info("{}", loginVo);
        return userService.doLogin(loginVo, req, resp);
    }
}
