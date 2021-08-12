package com.lxm.seckill.config;


import com.lxm.seckill.entity.User;

public class UserContext {
    private static ThreadLocal<User> userHolder = new ThreadLocal<>();

    public static User getUser() {
        return userHolder.get();
    }

    public static void setUser(User user) {
        userHolder.set(user);
    }
}
