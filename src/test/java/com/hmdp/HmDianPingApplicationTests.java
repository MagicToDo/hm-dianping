package com.hmdp;

import cn.hutool.core.util.RandomUtil;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisConstants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
class HmDianPingApplicationTests {

    @Test
    void contextLoads() {
        for (int i = 0; i < 100; i++) {
            String s = RandomUtil.randomString(20);
            System.out.println(s);
        }

    }

    @Resource
    public IShopService shopService;

    @Resource(name = "stringRedisTemplate")
    private GeoOperations<String, String> geoOperations;

    @Test
    void loadShop2Redis() {
        // 查询商店数据
        List<Shop> list = shopService.list();
        // 批量导入
        for (Shop shop : list) {
            geoOperations.add(
                    RedisConstants.SHOP_GEO_KEY + shop.getTypeId(),
                    new Point(shop.getX(), shop.getY()), shop.getId().toString()
            );
        }
    }
}
