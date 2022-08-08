package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RefreshTokenInterceptor implements HandlerInterceptor {
	//新的StringRedisTemplate
	private StringRedisTemplate stringRedisTemplate;

	public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//      1.获取请求头的Token
		String token = request.getHeader("authorization");
		//判断用户是否存在
		if (StrUtil.isBlank(token)) {
			return true;
		}
		//		基于token获取redis用户值
		Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY + token);

		if (userMap.isEmpty()) {
			return true;
		}
		//		存在则将数据转化为UserDto,将一个map数据填充到bean，填充到的bean，是否忽略填充过程的错误。
		UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
//		存在，保存用户信息到THREA
		UserHolder.saveUser(userDTO);

//		刷新token的有效期
		stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

		return true;
	}

//	@Override
//	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
////        1.获取Session
//		HttpSession session = request.getSession();
////        2.从session获取对象
//		Object user = session.getAttribute("user");
////        3.判断用户是否存在
//		if (user==null){
//			//        4.不存在则拦截
//			response.setStatus(401);
//			return false;
//		}
////        5.保存在ThreadLocal
//		UserHolder.saveUser((UserDTO) user);
////        结束
//		return true;
//	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		UserHolder.removeUser();
	}
}
