package com.example.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
<<<<<<< HEAD
import com.example.entity.dto.UserDTO;
=======
import com.example.entity.po.UserInfo;
>>>>>>> 59b93a7cd221e063e4ec15b94853a76ca185bdee
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.example.utils.RedisContants.LOGIN_TOKEN_KEY;
import static com.example.utils.RedisContants.LOGIN_TOKEN_TTL;

public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("authorization");
        //判断用户是否存在
        if(StrUtil.isBlank(token)){
            return true;
        }
        String tokenKey = LOGIN_TOKEN_KEY + token;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(tokenKey);
        if (entries.isEmpty()){
            return true;
        }
        UserInfo UserInfo = BeanUtil.fillBeanWithMap(entries, new UserInfo(), false);
        UserHolder.saveUser(UserInfo);
        stringRedisTemplate.expire(LOGIN_TOKEN_KEY + token,LOGIN_TOKEN_TTL, TimeUnit.MINUTES);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
