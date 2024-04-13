package com.example.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SysSettingsDto{
    /**
     * 注册发送邮件标题
     */
    private String registerEmailTitle = "邮箱验证码";

    /**
     * 注册发送邮件内容
     */
    private String registerEmailContent = "你好，您的邮箱验证码是：%s，15分钟有效";

    /**
     * 用户初始化空间大小 1024M
     */
    private Integer userInitUseSpace = 1024;
}
