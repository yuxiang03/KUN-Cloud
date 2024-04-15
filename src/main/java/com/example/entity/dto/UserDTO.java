package com.example.entity.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String nickName;
    private Long useSpace;
    private Long totalSpace;
}
