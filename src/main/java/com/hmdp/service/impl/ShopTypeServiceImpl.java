package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
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
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Override
	public Result queryAllType() {
		String shopList = "shop:List:";
		List<ShopType> typeList = new ArrayList<>();
		//1.查询redis店铺类型
		List<String> typeListJson = stringRedisTemplate.opsForList().range(shopList, 0, -1);

//        2.如果存在，直接返回
		if ((typeListJson != null) && (!typeListJson.isEmpty())) {
			for (String shopTypeList : typeListJson
			) {
				typeList.add(JSONUtil.toBean(shopTypeList, ShopType.class));
			}
			return Result.ok(typeList);
		}
		//3.不存在，查询数据库
		typeList = this
				.query().orderByAsc("sort").list();
//        4.如果数据库不存在，返回error
		if (CollectionUtils.isEmpty(typeList)) {
			return Result.fail("error");
		}
//        5.存在，将对象转换为List并存到Redis。
		for (ShopType shopType : typeList
		) {
			stringRedisTemplate.opsForList().rightPush(shopList, JSONUtil.toJsonStr(shopType));
		}
		stringRedisTemplate.expire(shopList, 30, TimeUnit.DAYS);
//       6.返回
		return Result.ok(typeList);
	}
}
