package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.SystemConstants.SHOP_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 * 缓存击穿互斥锁解决方案
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl缓存击穿互斥锁 extends ServiceImpl<ShopMapper, Shop> implements IShopService {

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Override
//	缓存击穿互斥锁解决方案
	public Result queryById(Long id) {
		//缓存穿透
//		Shop shop = queryWithPassThrough(id);
		//缓存击穿互斥锁
		Shop shop = queryWithMutex(id);
		if (shop == null) {
			return Result.fail("店铺不存在");
		}
		return Result.ok(shop);
	}

	public Shop queryWithMutex(Long id) {
		String key = SHOP_KEY + id;
		//		1.线程从redis查询商铺缓存
		String shopJson = stringRedisTemplate.opsForValue().get(key);

//		2.判断是否存在
		if (StrUtil.isNotBlank(shopJson)) {
//		3.存在，直接返回
			log.warn("redis");
			return JSONUtil.toBean(shopJson, Shop.class);
		}

		if (shopJson != null) {
			//返回一个错误信息
			return null;
		}

//		4.实现缓存重建
//		4.1.获取互斥锁
		String lockKey;
		lockKey = "lock:shop:" + id;
		Shop shop;

		try {
			boolean isLock = tryLock(lockKey);
//		4.2.判断是否获取成功
			if (!isLock) {
				//		4.3.失败，休眠并重试
				Thread.sleep(50);
				return queryWithMutex(id);
			}

//		4.4.成功，根据id查询数据库
			shop = getById(id);

			if (shop == null) {
				//		5.不存在，返回错误
				return null;
			}
//		6.存在，存入redis。返回
			stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), 30, TimeUnit.MINUTES);
			log.warn("数据库查询");
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			//		7.释放互斥锁
			unlock(lockKey);
		}

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
