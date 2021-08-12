package com.lxm.seckill.interceptor;

import com.lxm.seckill.entity.User;
import com.lxm.seckill.utils.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Deprecated
public class LoginInterceptor implements HandlerInterceptor {
    @Autowired
    RedisTemplate<String,Object> redisTemplate;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ticket = CookieUtil.getCookieValue(request, "userTicket");

        if (!StringUtils.hasText(ticket)) {
            request.getRequestDispatcher("/login").forward(request, response);
            return false;
        }

        User user = (User) redisTemplate.opsForValue().get("user:" + ticket);
        if (user == null) {
            request.getRequestDispatcher("/login").forward(request, response);
            return false;
        }
        return true;
    }
}
