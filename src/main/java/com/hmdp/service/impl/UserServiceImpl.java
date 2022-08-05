package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
//    @Resource
//    private RedisTemplate redisTemplate;
    @Override
    public Result sendCode(String phone, HttpSession session) {
//        校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
//            不符合生成错误信息
            return Result.fail("手机号格式验证错误");
        }
        //        符合生成验证码
        String code = RandomUtil.randomNumbers(6);
//        保存验证码到redis
//        session.setAttribute("code", code);
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
//        发送验证码
        log.debug("发送验证码成功,验证码:{}",code);

//        校验
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
//        1.校验手机号
        String phone =loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)){
            //        2.如果不符合返回错误信息
            return Result.fail("手机号格式错误");
        }
//        3.从Redis获取验证码并校验
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if (cacheCode==null||!cacheCode.equals(code)){
            return Result.fail("验证码错误");
        }

//        通过手机号查询用户是否存在
        User user = query().eq("phone", phone).one();

//        4.判断用户是否存在
        if(user == null){
            //        不存在则创建用户，并保存用户到数据库
            user=createUserWithPhone(phone);
        }
        //        随机生成token，作为登陆令牌
        String token = UUID.randomUUID().toString(true);

//        将User对象转为Hash存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> stringObjectMap = BeanUtil.beanToMap(userDTO);

//        5.保存用户到Redis
//        session.setAttribute("user",user);
         stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token,stringObjectMap);
//         设置有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY+token, LOGIN_USER_TTL,TimeUnit.MINUTES);
//         存储

        return Result.ok(token);
    }

//    @Override
//    public Result login(LoginFormDTO loginForm, HttpSession session) {
//        String phone=loginForm.getPhone();
//        //        1.用户提交手机号和验证码
//        if (RegexUtils.isPhoneInvalid(phone)){
////            不符合
//            return Result.fail("手机格式错误");
//        }
//        if (loginForm.getCode()==null||!loginForm.getCode().equals(session.getAttribute("code"))) {
//            //        2.校验验证码
//            return Result.fail("验证码错误");
//        }
////        3.根据手机号查询用户 select * from tb_user where phone =?
//        User user=query().eq("phone",phone).one();
//
//
////        4.判断用户是否存在
//        if(user == null){
//            user=createUserWithPhone(phone);
//        }
////        不存在则创建用户，并保存用户到数据库
//        session.setAttribute("user",user);
////        5.保存用户到session
//        return Result.ok("欢迎进入");
//    }

    private User createUserWithPhone(String phone) {
//        1.创建用户。
         User user = new User();
         user.setPhone(phone);
//         user.setNickName("niubi_"+RandomUtil.randomString(8));
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(8));
        save(user);
         return user;
    }
}
