package com.example.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.LoginFormDTO;
import com.example.entity.dto.Result;
import com.example.entity.dto.UserDTO;
import com.example.entity.po.User;
import com.example.mapper.UserMapper;
import com.example.service.UserService;
import com.example.utils.RegexUtils;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.example.utils.RedisContants.*;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper,User> implements UserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result login(LoginFormDTO loginForm) {
        String account = loginForm.getAccount();
        if(RegexUtils.isPhoneInvalid(account)||RegexUtils.isEmailInvalid(account)) return Result.fail("格式错误");
        Object cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+account);
        if (cacheCode==null) return Result.fail("请先发送验证码");
        String code = loginForm.getCode();
        if(code==null||!cacheCode.toString().equals(code)) return Result.fail("验证码错误");
        User user = query().eq("phone", account).or().eq("email",account).one();
        if (user==null) Result.fail("用户未注册");
        String token = UUID.randomUUID().toString();
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> map = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName,fieldValue)->fieldValue.toString()));
        String tokenKey = LOGIN_TOKEN_KEY+token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,map);
        stringRedisTemplate.expire(tokenKey,LOGIN_TOKEN_TTL, TimeUnit.MINUTES);
        return Result.ok(token);
    }

    @Override
    public Result logout(LoginFormDTO loginForm) {
        return Result.ok();
    }
}
