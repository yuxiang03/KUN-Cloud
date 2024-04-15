package com.example.controller;

import com.example.entity.dto.Result;
import com.example.entity.dto.SessionWebUserDto;
import com.example.entity.enums.FileDelFlagEnums;
import com.example.entity.query.FileInfoQuery;
import com.example.entity.vo.FileInfoVO;
import com.example.entity.vo.PaginationResultVO;
import com.example.service.FileInfoService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recycle")
public class RecycleController{

    @Resource
    private FileInfoService fileInfoService;

    /**
     * 根据条件分页查询
     */
    @RequestMapping("/loadRecycleList")
<<<<<<< HEAD
    public Result loadRecycleList(Integer pageNo, Integer pageSize) {

        PaginationResultVO result = fileInfoService.findListByPage(pageNo,pageSize);
        return getSuccessResult(convert2PaginationVO(result, FileInfoVO.class));
=======
    public Result loadRecycleList(HttpSession session, Integer pageNo, Integer pageSize) {
        FileInfoQuery query = new FileInfoQuery();
        query.setPageSize(pageSize);
        query.setPageNo(pageNo);
        query.setUserId(getUserInfoFromSession(session).getUserId());
        query.setOrderBy("recovery_time desc");
        query.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        PaginationResultVO result = fileInfoService.findListByPage(query);
        return Result.ok(convert2PaginationVO(result, FileInfoVO.class));
>>>>>>> origin/main
    }

    @RequestMapping("/recoverFile")
    public Result recoverFile(HttpSession session,String fileIds) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.recoverFileBatch(webUserDto.getUserId(), fileIds);
        return Result.ok(null);
    }

    @RequestMapping("/delFile")
    public Result delFile(HttpSession session,String fileIds) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.delFileBatch(webUserDto.getUserId(), fileIds,false);
        return Result.ok(null);
    }
}
