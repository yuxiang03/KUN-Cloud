package com.example.entity.query;

import lombok.Data;

@Data
public class FileInfoQuery {
    private Long fileId;
    private Long userId;
    private String fileMd5;
    private Long filePid;
    private Long fileSize;
    private String fileName;
    private String fileCover;
    private String filePath;
    private String createTime;
    private String lastUpdateTime;
    private Integer folderType;
    //1:视频 2:音频  3:图片 4:文档 5:其他
    private Integer fileCategory;
    /**
     * 1:视频 2:音频  3:图片 4:pdf 5:doc 6:excel 7:txt 8:code 9:zip 10:其他
     */
    private Integer fileType;
    /**
     * 0:转码中 1转码失败 2:转码成功
     */
    private Integer status;
    private Integer delFlag;
    /*    *//**
     * 回收站时间
     *//*
    private String recoveryTime;
    private String recoveryTimeStart;
    private String recoveryTimeEnd;
    *//**
     * 删除标记 0:删除  1:回收站  2:正常
     *//*
    private Integer delFlag;
    private String[] fileIdArray;
    private String[] filePidArray;
    private String[] excludeFileIdArray;
    private Boolean queryExpire;
    private Boolean queryNickName;*/
}
