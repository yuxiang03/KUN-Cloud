package com.example.controller;


import com.example.component.RedisComponent;
import com.example.entity.dto.Result;
import com.example.entity.dto.SysSettingsDto;
import com.example.entity.query.FileInfoQuery;
import com.example.entity.query.UserInfoQuery;
import com.example.entity.vo.PaginationResultVO;
import com.example.entity.vo.UserInfoVO;
import com.example.service.FileInfoService;
import com.example.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController extends CommonFileController {

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private UserService userService;

    @Resource
    private FileInfoService fileInfoService;

    @RequestMapping("/getSysSettings")
    public Result getSysSettings() {
        return Result.ok(redisComponent.getSysSettingsDto());
    }


    @RequestMapping("/saveSysSettings")
    public Result saveSysSettings(
            String registerEmailTitle,
            String registerEmailContent,
            Integer userInitUseSpace) {
        SysSettingsDto sysSettingsDto = new SysSettingsDto();
        sysSettingsDto.setRegisterEmailTitle(registerEmailTitle);
        sysSettingsDto.setRegisterEmailContent(registerEmailContent);
        sysSettingsDto.setUserInitUseSpace(userInitUseSpace);
        redisComponent.saveSysSettingsDto(sysSettingsDto);
        return Result.ok(null);
    }

    @RequestMapping("/loadUserList")
    public Result loadUser(UserInfoQuery userInfoQuery) {
        userInfoQuery.setOrderBy("join_time desc");
        PaginationResultVO resultVO = userInfoService.findList(userInfoQuery);
        return Result.ok(convert2PaginationVO(resultVO, UserInfoVO.class));
    }

    @RequestMapping("/updateUserStatus")
    public Result updateUserStatus( String userId,  Integer status) {
        userInfoService.updateUserStatus(userId, status);
        return Result.ok(null);
    }

    @RequestMapping("/updateUserSpace")
    public Result updateUserSpace( String userId,  Integer changeSpace) {
        userInfoService.changeUserSpace(userId, changeSpace);
        return Result.ok(null);
    }

    /**
     * 查询所有文件
     *
     * @param query
     * @return
     */
    @RequestMapping("/loadFileList")
    public Result loadDataList(FileInfoQuery query) {
        query.setOrderBy("last_update_time desc");
        query.setQueryNickName(true);
        PaginationResultVO resultVO = fileInfoService.findListByPage(query);
        return Result.ok(resultVO);
    }

    @RequestMapping("/getFolderInfo")
    public Result getFolderInfo( String path) {
        return super.getFolderInfo(path, null);
    }


    @RequestMapping("/getFile/{userId}/{fileId}")
    public void getFile(HttpServletResponse response,
                        @PathVariable("userId")  String userId,
                        @PathVariable("fileId")  String fileId) {
        super.getFile(response, fileId, userId);
    }


    @RequestMapping("/ts/getVideoInfo/{userId}/{fileId}")
    public void getVideoInfo(HttpServletResponse response,
                             @PathVariable("userId")  String userId,
                             @PathVariable("fileId")  String fileId) {
        super.getFile(response, fileId, userId);
    }

    @GetMapping("/createDownloadUrl/{userId}/{fileId}")
    public Result createDownloadUrl(@PathVariable("userId") String userId,
                                    @PathVariable("fileId") String fileId) {
        return Result.ok();
    }

    /**
     * 下载
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/download/{code}")
    public void download(HttpServletRequest request, HttpServletResponse response,
                         @PathVariable("code") String code) throws Exception {
        download(request, response, code);
    }


    @RequestMapping("/delFile")
    public Result delFile(String fileIdAndUserIds) {
        String[] fileIdAndUserIdArray = fileIdAndUserIds.split(",");
        for (String fileIdAndUserId : fileIdAndUserIdArray) {
            String[] itemArray = fileIdAndUserId.split("_");
            fileInfoService.delFileBatch(itemArray[0], itemArray[1], true);
        }
        return Result.ok(null);
    }
}
