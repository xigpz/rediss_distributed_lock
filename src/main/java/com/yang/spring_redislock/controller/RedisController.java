package com.yang.spring_redislock.controller;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author yanggm
 * @date 2020-07-18 下午 16:40
 * 学习笔记,视频地址：https://www.bilibili.com/video/BV1FJ411a7gv
 */
@RestController
public class RedisController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private Redisson redisson;

    /**
     * redis实现分布式锁
     * @return
     */
    @GetMapping(value = "/getLock")
    public String getLock() {
        String lockKey = "lock";
        String lockValue = UUID.randomUUID().toString();
        Boolean bool = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 10, TimeUnit.SECONDS);//相当于我们的setnx命令
        try {
            if (!bool) {
                return "error";
            }

            int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                int realStock = stock - 1;
                stringRedisTemplate.opsForValue().set("stock", realStock + "");
                System.out.println("售卖成功,剩余" + realStock + "");

                return "success";
            } else {
                System.out.println("剩余库存不足");
                return "fail";
            }
        } finally {
            if (lockValue.equals(stringRedisTemplate.opsForValue().get(lockKey))) {
                stringRedisTemplate.delete(lockKey);
            }
        }
    }

    /**
     * redission实现分布式锁
     * @return
     */
    @GetMapping(value = "/getRedissionLock")
    public String getRedissionLock() {
        String lockKey = "lock";
        RLock redissonLock = redisson.getLock(lockKey);
        try {
            redissonLock.lock();
            int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                int realStock = stock - 1;
                stringRedisTemplate.opsForValue().set("stock", realStock + "");
                System.out.println("售卖成功,剩余" + realStock + "");

                return "success";
            } else {
                System.out.println("剩余库存不足");
                return "fail";
            }
        } finally {
            redissonLock.unlock();
        }
    }
}
