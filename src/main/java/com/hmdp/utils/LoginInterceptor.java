package com.hmdp.utils;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static com.hmdp.dto.Result.fail;

public class LoginInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        1.获取Session
		HttpSession session = request.getSession();
//        2.从session获取对象
		Object user = session.getAttribute("user");
//        3.判断用户是否存在
		if (user==null){
			//        4.不存在则拦截
			response.setStatus(401);
			return false;
		}
//        5.保存在ThreadLocal
		UserHolder.saveUser((UserDTO) user);
//        结束
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		UserHolder.removeUser();
	}
}
