package com.example.entity.vo;

import lombok.Data;

@Data
public class ListInfo {
    private Long userId;
    private Integer current;
    private Integer size;
    private String category;
}
