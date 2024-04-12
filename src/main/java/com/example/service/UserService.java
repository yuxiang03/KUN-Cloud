package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.LoginFormDTO;
import com.example.entity.dto.Result;
import jakarta.servlet.http.HttpSession;

public interface UserService extends IService {
    Result login(LoginFormDTO loginForm, HttpSession session);
}
