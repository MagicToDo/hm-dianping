package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.SystemConstants.SHOP_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 * 缓存击穿逻辑过期解决方案
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Override
//	缓存击穿逻辑过期解决方案
	public Result queryById(Long id) {

		//缓存击穿
		Shop shop = queryWithLogicalExpire(id);
		if (shop == null) {
			return Result.fail("店铺不存在");
		}
		return Result.ok(shop);
	}

	//线程池
	private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

	public Shop queryWithLogicalExpire(Long id) {
		String key = SHOP_KEY + id;
		//		1.线程从redis查询商铺缓存
		String shopJson = stringRedisTemplate.opsForValue().get(key);

//		2.判断是否存在
		if (StrUtil.isBlank(shopJson)) {
//		3.存在，直接返回
			log.warn("redis中不存在");
			return null;
		}
//		4.命中，需要先把json反序列化为对象
		RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
		JSONObject data = (JSONObject) redisData.getData();
		Shop shop = JSONUtil.toBean(data, Shop.class);
		LocalDateTime expireTime = redisData.getExpireTime();
//		5.判断是否过期
		if (expireTime.isAfter(LocalDateTime.now())) {
			//		5.1 未过期直接返回
			return shop;
		}

//		5.2 已过期缓存重建
//		6.1 创建互斥锁
		String localkey = "lock:shop" + id;
		boolean isLock = tryLock(localkey);
//		获取到锁,开启独立线程
		if (isLock) {
			CACHE_REBUILD_EXECUTOR.submit(() -> {
				try {
//				缓存重建
					this.saveShop2Redis(id, 30L);
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					//				释放锁
					unlock(localkey);
				}
			});
		}
//		6.2 未获取到锁直接返回旧商铺信息数据
		return shop;
	}


	private boolean tryLock(String key) {
		log.warn("trylock");
		Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
		return BooleanUtil.isTrue(flag);
	}

	private void unlock(String key) {
		stringRedisTemplate.delete(key);
	}

	//持久化预热redis数据
	public void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
//		1.查询店铺数据
		Shop shop = getById(id);
		Thread.sleep(200);
//		2.封装逻辑过期时间
		RedisData redisData = new RedisData();
		redisData.setData(shop);
		redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
//		3.写入Redis
		stringRedisTemplate.opsForValue().set(SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
	}

	@Override
	@Transactional
	public Result update(Shop shop) {
		Long id = shop.getId();
		if (id == null) {
//			return Result.ok("商铺信息更新成功");
			return Result.fail("店铺id不能为空");
		}
		//更新数据库
		updateById(shop);
//		删除缓存
		stringRedisTemplate.delete(SHOP_KEY + id);
		return Result.ok();
	}
}
