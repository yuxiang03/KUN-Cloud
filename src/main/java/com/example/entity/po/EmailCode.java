package com.example.entity.po;

import lombok.Data;

import java.io.Serializable;

@Data
public class EmailCode implements Serializable {
    private String email;
    private String code;
    private Integer status;
}
