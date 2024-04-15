package com.example.controller;

import com.example.entity.dto.Result;
import com.example.entity.dto.SessionWebUserDto;
import com.example.entity.enums.FileCategoryEnums;
import com.example.entity.enums.FileDelFlagEnums;
import com.example.entity.enums.FileFolderTypeEnums;
import com.example.entity.po.FileInfo;
import com.example.entity.query.FileInfoQuery;
import com.example.entity.vo.FileInfoVO;
import com.example.entity.vo.PaginationResultVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件信息 Controller
 */
@RequestMapping("/file")
public class FileInfoController extends CommonFileController {

    @RequestMapping("/loadDataList")
    public Result loadDataList(HttpSession session, FileInfoQuery query, String category) {
        FileCategoryEnums categoryEnum = FileCategoryEnums.getByCode(category);
        if (null != categoryEnum) {
            query.setFileCategory(categoryEnum.getCategory());
        }
        query.setUserId(getUserInfoFromSession(session).getUserId());
        query.setOrderBy("last_update_time desc");
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        PaginationResultVO result = fileInfoService.findListByPage(query);
        return Result.ok(convert2PaginationVO(result, FileInfoVO.class));
    }

    @RequestMapping("/uploadFile")
    public Result uploadFile(HttpSession session,
                             String fileId,
                             MultipartFile file,
                             String fileName,
                             String filePid,
                             String fileMd5,
                             Integer chunkIndex,
                             Integer chunks) {

        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        UploadResultDto resultDto = fileInfoService.uploadFile(webUserDto, fileId, file, fileName, filePid, fileMd5, chunkIndex, chunks);
        return Result.ok(resultDto);
    }


    @RequestMapping("/getImage/{imageFolder}/{imageName}")
    public void getImage(HttpServletResponse response, @PathVariable("imageFolder") String imageFolder, @PathVariable("imageName") String imageName) {
        super.getImage(response, imageFolder, imageName);
    }

    @RequestMapping("/ts/getVideoInfo/{fileId}")
    public void getVideoInfo(HttpServletResponse response, HttpSession session, @PathVariable("fileId") String fileId) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        super.getFile(response, fileId, webUserDto.getUserId());
    }

    @RequestMapping("/getFile/{fileId}")
    public void getFile(HttpServletResponse response, HttpSession session, @PathVariable("fileId") String fileId) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        super.getFile(response, fileId, webUserDto.getUserId());
    }

    @RequestMapping("/newFoloder")
    public Result newFoloder(HttpSession session,
                                 String filePid,
                                 String fileName) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        FileInfo fileInfo = fileInfoService.newFolder(filePid, webUserDto.getUserId(), fileName);
        return Result.ok(fileInfo);
    }

    @RequestMapping("/getFolderInfo")
    public Result getFolderInfo(HttpSession session, String path) {
        return super.getFolderInfo(path, getUserInfoFromSession(session).getUserId());
    }


    @RequestMapping("/rename")
    public Result rename(HttpSession session,
                             String fileId,
                             String fileName) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        FileInfo fileInfo = fileInfoService.rename(fileId, webUserDto.getUserId(), fileName);
        return Result.ok(CopyTools.copy(fileInfo, FileInfoVO.class));
    }

    @RequestMapping("/loadAllFolder")

    public Result loadAllFolder(HttpSession session, String filePid, String currentFileIds) {
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(getUserInfoFromSession(session).getUserId());
        query.setFilePid(filePid);
        query.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        if (!StringTools.isEmpty(currentFileIds)) {
            query.setExcludeFileIdArray(currentFileIds.split(","));
        }
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        query.setOrderBy("create_time desc");
        List<FileInfo> fileInfoList = fileInfoService.findListByParam(query);
        return Result.ok(CopyTools.copyList(fileInfoList, FileInfoVO.class));
    }

    @RequestMapping("/changeFileFolder")
    public Result changeFileFolder(HttpSession session,
                                       String fileIds,
                                       String filePid) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.changeFileFolder(fileIds, filePid, webUserDto.getUserId());
        return Result.ok(null);
    }

    @RequestMapping("/createDownloadUrl/{fileId}")

    public Result createDownloadUrl(HttpSession session, @PathVariable("fileId") String fileId) {
        return super.createDownloadUrl(fileId, getUserInfoFromSession(session).getUserId());
    }

    @RequestMapping("/download/{code}")
    public void download(HttpServletRequest request, HttpServletResponse response, @PathVariable("code") String code) throws Exception {
        super.download(request, response, code);
    }


    @RequestMapping("/delFile")

    public Result delFile(HttpSession session, String fileIds) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.removeFile2RecycleBatch(webUserDto.getUserId(), fileIds);
        return Result.ok(null);
    }
}