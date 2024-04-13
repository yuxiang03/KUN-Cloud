package com.example.entity.dto;

import lombok.Data;

@Data
public class LoginFormDTO {
    private String account;
    private String code;
    private String password;
}
