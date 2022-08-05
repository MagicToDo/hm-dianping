package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.hmdp.dto.Result.fail;

public class LoginInterceptor implements HandlerInterceptor {

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//      1.获取请求头的Token
		String token = request.getHeader("authorization");

		//判断用户是否存在
		if (StrUtil.isBlank(token)) {
//			不存在则拦截
			response.setStatus(401);
			return false;
		}

		//		基于token获取redis用户值
		Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY + token);

//		存在则将数据转化为UserDto
		UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
		if (userDTO == null) {
//			    4.不存在则拦截
			response.setStatus(401);
			return false;
		}
//		存在，保存用户信息到THREA
		UserHolder.saveUser(userDTO);
//		刷新token的有效期
		stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

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
