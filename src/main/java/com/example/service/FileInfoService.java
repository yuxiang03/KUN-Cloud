package com.example.service;

import com.example.entity.dto.Result;
import com.example.entity.po.FileInfo;
import com.example.entity.query.FileInfoQuery;
import com.example.entity.vo.FileInfoVO;
import com.example.entity.vo.FolderVO;
import com.example.entity.vo.PaginationResultVO;
import jakarta.servlet.http.HttpSession;

import java.util.List;

public interface FileInfoService {
    List<FileInfo> findListByParam(FileInfoQuery param);
    Integer findCountByParam(FileInfoQuery param);

    PaginationResultVO<FileInfo> findListByPage(Integer pageNo, Integer pageSize);

    boolean add(FileInfo bean);

    Integer addBatch(List<FileInfo> listBean);

    Integer addOrUpdateBatch(List<FileInfo> listBean);

    FileInfo getFileInfoByFileIdAndUserId(String fileId, String userId);

    Integer updateFileInfoByFileIdAndUserId(FileInfo bean, String fileId, String userId);

    Integer deleteFileInfoByFileIdAndUserId(String fileId, String userId);

    Result uploadFile(FileInfoVO fileInfoVO, HttpSession session);

    Result rename(String fileId, String userId, String fileName);

    Result newFolder(FolderVO folderVO);

    void changeFileFolder(String fileIds, String filePid, String userId);

    void removeFile2RecycleBatch(String userId, String fileIds);

    void recoverFileBatch(String userId, String fileIds);

    void delFileBatch(String userId, String fileIds, Boolean adminOp);

    void checkRootFilePid(String rootFilePid, String userId, String fileId);

    void saveShare(String shareRootFilePid, String shareFileIds, String myFolderId, String shareUserId, String cureentUserId);
}