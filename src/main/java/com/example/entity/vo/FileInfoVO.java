package com.example.entity.vo;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileInfoVO {
    private String filePid;
    private Long fileSize;
    private String fileMd5;
    private String fileName;
    private MultipartFile file;
    private String fileCover;
    private Integer folderType;
    private Integer fileCategory;
    private Integer fileType;
    private Integer chunkIndex;
    private Integer chunks;
}
