package com.example.entity.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileInfoVO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long fileId;
    private Long userId;
    private Long filePid;
    private Long fileSize;
    private String fileMd5;
    private String fileName;
    private MultipartFile file;
    private String fileCover;
    private Integer folderType;
    private Integer fileCategory;
    private Integer fileType;
    private Integer chunkIndex;
    private Integer totalChunks;
}
