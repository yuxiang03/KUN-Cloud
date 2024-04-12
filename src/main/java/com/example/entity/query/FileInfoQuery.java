package com.example.entity.query;

import lombok.Data;

@Data
public class FileInfoQuery extends BaseParam {
    private String fileId;
    private String fileIdFuzzy;
    private String userId;
    private String userIdFuzzy;
    private String fileMd5;
    private String fileMd5Fuzzy;
    private String filePid;
    private String filePidFuzzy;
    private Long fileSize;
    private String fileName;
    private String fileNameFuzzy;
    /**
     * 封面
     */
    private String fileCover;
    private String fileCoverFuzzy;
    /**
     * 文件路径
     */
    private String filePath;
    private String filePathFuzzy;
    /**
     * 创建时间
     */
    private String createTime;

    private String createTimeStart;

    private String createTimeEnd;
    /**
     * 最后更新时间
     */
    private String lastUpdateTime;

    private String lastUpdateTimeStart;

    private String lastUpdateTimeEnd;

    /**
     * 0:文件 1:目录
     */
    private Integer folderType;

    /**
     * 1:视频 2:音频  3:图片 4:文档 5:其他
     */
    private Integer fileCategory;

    /**
     * 1:视频 2:音频  3:图片 4:pdf 5:doc 6:excel 7:txt 8:code 9:zip 10:其他
     */
    private Integer fileType;

    /**
     * 0:转码中 1转码失败 2:转码成功
     */
    private Integer status;

    /**
     * 回收站时间
     */
    private String recoveryTime;
    private String recoveryTimeStart;
    private String recoveryTimeEnd;
    /**
     * 删除标记 0:删除  1:回收站  2:正常
     */
    private Integer delFlag;
    private String[] fileIdArray;
    private String[] filePidArray;
    private String[] excludeFileIdArray;
    private Boolean queryExpire;
    private Boolean queryNickName;
}