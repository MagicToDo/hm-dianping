package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Override
	public Result queryById(Long id) {
		String key="cache:shop:" + id;
//		1.从redis查询商铺缓存
		String shopJson = stringRedisTemplate.opsForValue().get(key);

//		2.判断是否存在
		if (StrUtil.isNotBlank(shopJson)) {
//		3.存在，直接返回
			Shop shop = JSONUtil.toBean(shopJson, Shop.class);
			return Result.ok(shop);
		}
//		4.不存在，根据id查询数据库
		Shop shop = getById(id);

		if (shop == null) {
			//		5.不存在，返回错误
			return Result.fail("店铺不存在");
		}
//		6.存在，存入redis。返回
		stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),30, TimeUnit.MINUTES);

		return Result.ok(shop);
	}

	@Override
	@Transactional
	public Result update(Shop shop) {

		Long id = shop.getId();
		//更新数据库
		boolean b = this.updateById(shop);
//		删除缓存
		if (b&&id!=null){
			stringRedisTemplate.delete("cache:shop:"+id);
//			return Result.ok("商铺信息更新成功");
			return Result.ok();
		}
		return Result.fail("商铺信息更新错误");
	}
}
