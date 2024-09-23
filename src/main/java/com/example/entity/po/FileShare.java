package com.example.entity.po;

import cn.hutool.core.date.DateUtil;
import com.example.entity.enums.DateTimePatternEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class FileShare{
    private String shareId;
    private String fileId;
    private String userId;
    //有效期类型 0:1天 1:7天 2:30天 3:永久有效
    private Integer validType;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date shareTime;
    private String code;
    private Integer showCount;
    private String fileName;
    private Integer folderType;
    private Integer fileCategory;
    private Integer fileType;
    private String fileCover;
    @Override
    public String toString() {
        return "分享ID:" + (shareId == null ? "空" : shareId) + "，文件ID:" + (fileId == null ? "空" : fileId) + "，用户ID:" + (userId == null ? "空" : userId) + "，有效期类型 0:1天 " +
                "1:7天 2:30天 3:永久有效:" + (validType == null ? "空" : validType) + "，失效时间:" + (expireTime == null ? "空" : DateUtil.format(expireTime,
                DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern())) + "，分享时间:" + (shareTime == null ? "空" : DateUtil.format(shareTime,
                DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern())) + "，提取码:" + (code == null ? "空" : code) + "，浏览次数:" + (showCount == null ? "空" : showCount);
    }
}
