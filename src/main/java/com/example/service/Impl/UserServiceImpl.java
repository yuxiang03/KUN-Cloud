package com.example.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.constants.Constants;
import com.example.entity.dto.LoginFormDTO;
import com.example.entity.dto.Result;
import com.example.entity.dto.UserDTO;
import com.example.entity.enums.UserStatusEnum;
import com.example.entity.po.User;
import com.example.entity.pojo.Register;
import com.example.mapper.UserMapper;
import com.example.service.EmailCodeService;
import com.example.service.UserService;
import com.example.utils.JwtUtils;
import com.example.utils.MD5Utils;
import com.example.utils.RegexUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.example.utils.RedisContants.*;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper,User> implements UserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private EmailCodeService emailCodeService;

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
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> map = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName,fieldValue)->fieldValue.toString()));
        String token = JwtUtils.generateJwt(map);
        String tokenKey = LOGIN_TOKEN_KEY+token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,map);
        stringRedisTemplate.expire(tokenKey,LOGIN_TOKEN_TTL, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set(LOGIN_TOKEN_KEY,token);
        stringRedisTemplate.expire(LOGIN_TOKEN_KEY,LOGIN_TOKEN_TTL, TimeUnit.MINUTES);
        return Result.ok(token);
    }

    @Override
    public Result logout(LoginFormDTO loginForm) {
        return Result.ok();
    }

    @Override
    public Result register(Register register) {
        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        QueryWrapper<User> email = queryWrapper.eq("email", register.getEmail());
        User user = getOne(email);
        if (null != user) {
            return Result.fail("邮箱账号已经存在");
        }

        QueryWrapper<User> nick_name = queryWrapper.eq("nick_name", register.getNickName());
        User nickNameUser = getOne(nick_name);
        if (null != nickNameUser) {
            return Result.fail("昵称已经存在");
        }
        //校验邮箱验证码
        emailCodeService.checkCode(register.getEmail(), register.getEmailCode());
        user = new User();
        user.setNickName(register.getNickName());
        user.setEmail(register.getEmail());
        String password = register.getPassword();
        String pw = MD5Utils.encrypt(password);
        user.setPassword(pw);
        user.setJoinTime(new Date());
        user.setStatus(UserStatusEnum.ENABLE.getStatus());
        user.setTotalSpace(512 * Constants.MB);
        user.setUseSpace(0L);
        save(user);
        return Result.ok(null);
    }
}
