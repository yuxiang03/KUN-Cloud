package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.LoginFormDTO;
import com.example.entity.dto.Result;
import com.example.entity.po.User;
import jakarta.servlet.http.HttpSession;

public interface UserService extends IService<User> {


    Result login(LoginFormDTO loginForm, HttpSession session);
}
