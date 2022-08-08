package com.hmdp.utils;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {
	//新的StringRedisTemplate

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
//		判断是否要拦截（ThreadLocal中是否有用户）。UserHolder是static可以直接调用
		if (UserHolder.getUser() == null) {
			response.setStatus(401);
			return false;
		}
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
