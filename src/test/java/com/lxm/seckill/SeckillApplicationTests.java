package com.lxm.seckill;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.util.Random;

@SpringBootTest
class SeckillApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    public void test() throws IOException {
        BufferedReader bis = new BufferedReader(new FileReader("user.csv"));
        BufferedWriter bos = new BufferedWriter(new FileWriter("user2.csv"));
        String s = bis.readLine();
        bos.write(s + ",goodsId\r\n");

        Random random = new Random();
        String line = null;
        while ((line = bis.readLine()) != null) {
            bos.write(line + "," + (random.nextInt(2) + 1) );
            bos.write("\r\n");
        }

        bos.flush();
        bos.close();
        bis.close();
    }

}
