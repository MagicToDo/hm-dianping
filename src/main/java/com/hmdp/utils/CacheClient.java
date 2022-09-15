package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.CACHE_NULL_TTL;

/**
 * redis工具类
 */
@Slf4j
@Component
public class CacheClient {


	private final StringRedisTemplate stringRedisTemplate;

	public CacheClient(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public void set(String key, Object value, Long time, TimeUnit unit) {
		stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
	}

	//	逻辑过期
	public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
//	设置逻辑过期
		RedisData redisData = new RedisData();
		redisData.setData(value);
		redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
		//写入Redis
		stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
	}


	//缓存穿透
	public <R, T> R queryWithPassThrough(
			String keyPrefix, T id, Class<R> type, Function<T, R> dbFallback, Long time, TimeUnit unit) {
		String key = keyPrefix + id;
//		1.从redis查询商铺缓存
		String json = stringRedisTemplate.opsForValue().get(key);
//		2.判断是否存在
		if (StrUtil.isNotBlank(json)) {
//		3.存在，直接返回
			return JSONUtil.toBean(json, type);
		}
//		判断的命中的是否是空值
		if (json != null) {
			//返回一个错误信息
			return null;
		}

//		4.不存在，根据id查询数据库
		R r = dbFallback.apply(id);
//		5.不存在，返回错误
		if (r == null) {
//			将空值写入redis
			stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//			返回错误信息
			return null;
		}
//		6.存在，存入redis。返回
		this.set(key, r, time, unit);

		return r;
	}


	//线程池
	private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

	public <R, T> R queryWithLogicalExpire(String keyPrefix, T id, Class<R> type, Function<T, R> dbFallback, Long time, TimeUnit unit) {
		String key = keyPrefix + id;
		//		1.线程从redis查询商铺缓存
		String json = stringRedisTemplate.opsForValue().get(key);

//		2.判断是否存在
		if (StrUtil.isBlank(json)) {
//		3.存在，直接返回
			log.warn("redis中不存在");
			return null;
		}
//		4.命中，需要先把json反序列化为对象
		RedisData redisData = JSONUtil.toBean(json, RedisData.class);
		R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
		LocalDateTime expireTime = redisData.getExpireTime();
//		5.判断是否过期
		if (expireTime.isAfter(LocalDateTime.now())) {
			//		5.1 未过期直接返回
			return r;
		}

//		5.2 已过期缓存重建
//		6.1 创建互斥锁
		String localkey = "lock:" + id;
		boolean isLock = tryLock(localkey);
//		获取到锁,开启独立线程
		if (isLock) {
			CACHE_REBUILD_EXECUTOR.submit(() -> {
				try {
//				缓存重建
					R r1 = dbFallback.apply(id);
//					写入redis
					this.setWithLogicalExpire(key, r1, time, unit);
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					//				释放锁
					unlock(localkey);
				}
			});
		}
//		6.2 未获取到锁直接返回旧信息数据
		return r;
	}

	/**
	 * 锁
	 *
	 * @param key
	 * @return
	 */
	private boolean tryLock(String key) {
		log.warn("trylock");
		Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
		return BooleanUtil.isTrue(flag);
	}

	private void unlock(String key) {
		stringRedisTemplate.delete(key);
	}
}
