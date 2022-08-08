package com.hmdp.controller;


import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.service.IShopTypeService;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("list")
    public Result queryTypeList() {
        String shopList = "shopList";
        //1.查询redis店铺类型
        List<String> typeListJson = stringRedisTemplate.opsForList().range(shopList, 0, -1);

//        2.如果存在，直接返回
        if (!CollectionUtils.isEmpty(typeListJson)) {
            JSONUtil.toBean(typeListJson,typeList.class);
            return   Result.ok(typeListJson);
        }
        //3.不存在，查询数据库
        List<ShopType> typeList = typeService
                .query().orderByAsc("sort").list();
//        4.如果数据库不存在，返回error
        if(CollectionUtils.isEmpty(typeList)){
            return Result.fail("error");
        }

//        5.存在，保存到redis里
        for (ShopType shopType:typeList
             ) { stringRedisTemplate.opsForList().rightPush(shopList, String.valueOf(shopType));
        }

//       6.返回
        return Result.ok(typeList);
    }
}
