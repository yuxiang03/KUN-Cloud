package com.example.controller;

import com.example.entity.dto.Result;
import com.example.entity.dto.SessionWebUserDto;
import com.example.entity.po.FileShare;
import com.example.entity.query.FileShareQuery;
import com.example.entity.vo.PaginationResultVO;
import com.example.service.FileShareService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("shareController")
@RequestMapping("/share")
public class ShareController{
    @Resource
    private FileShareService fileShareService;


    @RequestMapping("/loadShareList")
    public Result loadShareList(HttpSession session, FileShareQuery query) {
        query.setOrderBy("share_time desc");
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        query.setUserId(userDto.getUserId());
        query.setQueryFileName(true);
        PaginationResultVO resultVO = this.fileShareService.findListByPage(query);
        return Result.fail(resultVO);
    }

    @RequestMapping("/shareFile")
    public Result shareFile(HttpSession session,
                                String fileId,
                                Integer validType,
                                String code) {
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        FileShare share = new FileShare();
        share.setFileId(fileId);
        share.setValidType(validType);
        share.setCode(code);
        share.setUserId(userDto.getUserId());
        fileShareService.saveShare(share);
        return Result.fail(share);
    }

    @RequestMapping("/cancelShare")
    public Result cancelShare(HttpSession session, String shareIds) {
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        fileShareService.deleteFileShareBatch(shareIds.split(","), userDto.getUserId());
        return Result.fail(null);
    }
}
