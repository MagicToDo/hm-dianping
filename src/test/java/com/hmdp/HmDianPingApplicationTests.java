package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
@Slf4j
class HmDianPingApplicationTests {

	@Resource
	private ShopServiceImpl shopService;

	@Test
	void testSavaShop() throws InterruptedException {
		shopService.saveShop2Redis(1L, 20L);
	}

}
