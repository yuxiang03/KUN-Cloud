package com.example.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {

    private static final Long BEGIN_TIMESTEMP = 1700535904310L;
    private static final int COUNT_BIT = 32;

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String keyPrefix){
        LocalDateTime nowTime = LocalDateTime.now();
        long nowSecond = nowTime.toEpochSecond(ZoneOffset.UTC);
        long timesTemp = nowSecond - BEGIN_TIMESTEMP;
        String data = nowTime.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count = stringRedisTemplate.opsForValue().increment("icr:"+keyPrefix+data);
        return timesTemp << COUNT_BIT | count;
    }
}
