package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.LoginFormDTO;
import com.example.entity.dto.Result;
import com.example.entity.po.User;

public interface UserService extends IService<User> {
<<<<<<< HEAD
    Result login(LoginFormDTO loginForm);

    Result logout(LoginFormDTO loginForm);
=======


    Result login(LoginFormDTO loginForm, HttpSession session);
>>>>>>> 59b93a7cd221e063e4ec15b94853a76ca185bdee
}
