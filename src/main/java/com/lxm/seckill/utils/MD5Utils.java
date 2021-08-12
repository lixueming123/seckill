package com.lxm.seckill.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

@Component
public class MD5Utils {

    public static final String SALT = "asdqwe";

    public static String md5(String src) {
        return DigestUtils.md5Hex(src);
    }

    public static String inputPassToFormPass(String input) {
        String str = input + SALT;
        return md5(str);
    }

    public static String formPassToDBPass(String form, String salt) {
        String str = salt + form;
        return md5(str);
    }

    public static String inputPassToDBPass(String input, String salt) {
        String fromPass = inputPassToFormPass(input);
        return formPassToDBPass(fromPass, salt);
    }

    public static void main(String[] args) {
        System.out.println(inputPassToFormPass("123456"));
        System.out.println(formPassToDBPass("c17f820d7d726bf8dd715963d8e80f61", "qweasd"));
        System.out.println(inputPassToDBPass("123456", "qweasd"));
    }

}
