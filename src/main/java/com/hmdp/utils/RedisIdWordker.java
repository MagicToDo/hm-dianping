package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWordker {
	
	private static final  long BEGIN_TIMESTAMP = 1700000000L;
	
	private StringRedisTemplate stringRedisTemplate;
	
	public  RedisIdWordker(StringRedisTemplate stringRedisTemplate){
		this.stringRedisTemplate = stringRedisTemplate;
	}
	
	public long nextId(String keyPrefix){
//		1.生成时间戳
		LocalDateTime now = LocalDateTime.now();
		long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
		long timestamp = nowSecond - BEGIN_TIMESTAMP;

//		2.生成序列号
//		得到当前日期，精确到天
		String date = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		//自增
		Long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

//		3.拼接并返回
		return 0L;
	}
}
