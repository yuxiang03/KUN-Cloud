package com.example.entity.pojo;

import lombok.Data;

@Data
public class Register {
    private String email;
    private String nickName;
    private String password;
    private String checkCode;
    private String emailCode;
}
