package com.lxm.seckill.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxm.seckill.config.AccessLimit;
import com.lxm.seckill.config.UserContext;
import com.lxm.seckill.entity.User;
import com.lxm.seckill.service.UserService;
import com.lxm.seckill.utils.Const;
import com.lxm.seckill.utils.CookieUtil;
import com.lxm.seckill.vo.RespBean;
import com.lxm.seckill.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AccessLimitInterceptor implements HandlerInterceptor {

    @Autowired
    RedisTemplate<String,Object> redisTemplate;

    @Autowired
    UserService userService;

    @Autowired
    ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            AccessLimit annotation = handlerMethod.getMethodAnnotation(AccessLimit.class);

            if (annotation == null) {
                return  true;
            }

            boolean needLogin = annotation.needLogin();
            int seconds = annotation.seconds();
            int maxCount = annotation.maxCount();

            User user = getUser(request, response);
            UserContext.setUser(user);
            if (needLogin && user == null) {
                writeError(response, RespBeanEnum.SESSION_ERROR);
                return false;
            }

            if (seconds == -1) return true;
            if (maxCount == Integer.MAX_VALUE) return true;
            
            String uri = request.getRequestURI();
            ValueOperations<String, Object> ops = redisTemplate.opsForValue();
            String key = Const.ACCESS_LIMIT_PREFIX + uri + ":" +user.getId();
            Integer cnt = (Integer) ops.get(key);
            if (cnt == null) {
                ops.set(key, 1, seconds, TimeUnit.SECONDS);
            }
            else if (cnt < maxCount) {
                ops.increment(key);
            }
            else {
                writeError(response, RespBeanEnum.ACCESS_LIMIT_REACHED);
                return false;
            }
        }
        return true;
    }


    public void writeError(HttpServletResponse response, RespBeanEnum errorEnum) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        RespBean respBean = RespBean.error(errorEnum);
        writer.write(objectMapper.writeValueAsString(respBean));
        writer.flush();
        writer.close();
    }


    public User getUser(HttpServletRequest request, HttpServletResponse response) {
        String ticket = CookieUtil.getCookieValue(request, "userTicket");
        if (!StringUtils.hasText(ticket)) {
            return null;
        }
        return userService.getUserByCookie(ticket, request, response);
    }
}
