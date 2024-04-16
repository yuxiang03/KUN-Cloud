package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.LoginFormDTO;
import com.example.entity.dto.Result;
import com.example.entity.po.User;
import com.example.entity.po.Register;
import jakarta.servlet.http.HttpSession;

public interface UserService extends IService<User> {
    Result login(LoginFormDTO loginForm, HttpSession httpSession);
    Result logout(String token);

    Result register(Register register);

    Result resetPwd(String email, String password, String emailCode);

    Result getUserInfo(String token);

    Result getUseSpace(String token);

    void updatePwdByUserId(HttpSession session, String password);
}
