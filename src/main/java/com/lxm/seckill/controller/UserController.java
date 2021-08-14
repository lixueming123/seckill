package com.lxm.seckill.controller;


import com.lxm.seckill.config.AccessLimit;
import com.lxm.seckill.entity.User;
import com.lxm.seckill.service.UserService;
import com.lxm.seckill.utils.MD5Utils;
import com.lxm.seckill.utils.UUIDUtil;
import com.lxm.seckill.vo.RespBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lxm
 * @since 2021-08-06
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    private final Random random = new Random();

    /*
    * 用户信息接口
    * 2790.327(windows)
    * 600   (linux)
    * */
    @GetMapping("/info")
    @AccessLimit
    public RespBean info(User user) {
        return RespBean.success(user);
    }

    /**
     * 创建10000个测试用户
     * @return user list
     * @throws IOException io ex
     */
    @GetMapping("/create")
    public List<User> generateUserFile() throws IOException {
        File file = new File("user.csv");
        List<User> list = new ArrayList<>();
        FileOutputStream fos = new FileOutputStream(file, true);
        fos.write("userTicket,userId\r\n".getBytes());
        for (int i = 0; i < 10000; i++) {
            String ticket = UUIDUtil.uuid();
            User user = new User();
            long base = 1300_000_0000L;
            long rand = (long) (random.nextDouble() * 700_000_0000L);
            user.setId(base + rand);
            user.setNickname("testUser" + i);
            String salt = "qweasd";
            user.setSalt(salt);
            user.setPassword(MD5Utils.inputPassToDBPass("123456", salt));
            user.setHead("head" + i);
            user.setRegisterDate(new Date());
            user.setLastLoginDate(new Date());
            user.setLoginCount(1);
            list.add(user);
            fos.write((ticket + "," +user.getId() + "\r\n").getBytes());

            redisTemplate.opsForValue().set("user:"+ticket, user, 3, TimeUnit.DAYS);
        }

        userService.saveBatch(list, 1000);
        return list;
    }

}
