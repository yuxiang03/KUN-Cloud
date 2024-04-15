package com.example.controller;

import com.example.entity.constants.Constants;
import com.example.entity.dto.CreateImageCode;
import com.example.entity.dto.LoginFormDTO;
import com.example.entity.dto.Result;
import com.example.entity.dto.SessionWebUserDto;
import com.example.entity.po.EmailCode;
import com.example.entity.po.RePwd;
import com.example.entity.po.Register;
import com.example.entity.po.User;
import com.example.service.EmailCodeService;
import com.example.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

@RestController
public class AccountController{
    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json;charset=UTF-8";

    @Resource
    private UserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private EmailCodeService emailCodeService;

    @RequestMapping(value = "/checkCode")
    public void checkCode(HttpServletResponse response, HttpSession session, Integer type) throws
            IOException {
        CreateImageCode vCode = new CreateImageCode(130, 38, 5, 10);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        String code = vCode.getCode();
        if (type == null || type == 0) {
            session.setAttribute(Constants.CHECK_CODE_KEY, code);
        } else {
            session.setAttribute(Constants.CHECK_CODE_KEY_EMAIL, code);
        }
        vCode.write(response.getOutputStream());
    }

    @RequestMapping("/sendEmailCode")
    public Result sendEmailCode(@RequestBody EmailCode emailCode, HttpSession session) {
        return emailCodeService.sendEmailCode(emailCode,session);
    }

    //注册
    @RequestMapping("/register")
    public Result register(@RequestBody Register register,HttpSession session) {
        try {
            if (!register.getCheckCode().equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                return Result.fail("图片验证码不正确");
            }
            return userService.register(register);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }
    
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm) {
        return userService.login(loginForm);
    }


    //忘记密码
    @RequestMapping("/resetPwd")
    public Result resetPwd(@RequestBody RePwd rePwd, HttpSession session) {
        try {
            if (!rePwd.getCheckCode().equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                return Result.fail("图片验证码不正确");
            }
            return userService.resetPwd(rePwd.getEmail(), rePwd.getPassword(), rePwd.getEmailCode());
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    @RequestMapping("/getAvatar/{userId}")
    public void getAvatar(HttpServletResponse response, @PathVariable("userId") String userId) {
        String avatarFolderName = Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
        File folder = new File(appConfig.getProjectFolder() + avatarFolderName);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        String avatarPath = appConfig.getProjectFolder() + avatarFolderName + userId + Constants.AVATAR_SUFFIX;
        File file = new File(avatarPath);
        if (!file.exists()) {
            if (!new File(appConfig.getProjectFolder() + avatarFolderName + Constants.AVATAR_DEFUALT).exists()) {
                printNoDefaultImage(response);
                return;
            }
            avatarPath = appConfig.getProjectFolder() + avatarFolderName + Constants.AVATAR_DEFUALT;
        }
        response.setContentType("image/jpg");
        readFile(response, avatarPath);
    }

    private void printNoDefaultImage(HttpServletResponse response) {
        response.setHeader(CONTENT_TYPE, CONTENT_TYPE_VALUE);
        response.setStatus(HttpStatus.OK.value());
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.print("请在头像目录下放置默认头像default_avatar.jpg");
            writer.close();
        } catch (Exception e) {
            logger.error("输出无默认图失败", e);
        } finally {
            writer.close();
        }
    }

    @RequestMapping("/getUserInfo")
    public Result getUserInfo(String token) {
        return userService.getUserInfo(token);
    }

    @RequestMapping("/getUseSpace")
    public Result getUseSpace(String token) {
        return userService.getUseSpace(token);
    }

    @RequestMapping("/logout")
    public Result logout(String token) {
        return userService.logout(token);
    }

    @RequestMapping("/updateUserAvatar")
    public Result updateUserAvatar(HttpSession session, MultipartFile avatar) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
        File targetFileFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
        if (!targetFileFolder.exists()) {
            targetFileFolder.mkdirs();
        }
        File targetFile = new File(targetFileFolder.getPath() + "/" + webUserDto.getUserId() + Constants.AVATAR_SUFFIX);
        try {
            avatar.transferTo(targetFile);
        } catch (Exception e) {
            logger.error("上传头像失败", e);
        }

        User user = new User();
        userService.updateUserInfoByUserId(user, webUserDto.getUserId());
        webUserDto.setAvatar(null);
        session.setAttribute(Constants.SESSION_KEY, webUserDto);
        return Result.ok(null);
    }

    //更新密码
    @RequestMapping("/updatePassword")
    public Result updatePassword(HttpSession session, String password) {
        userService.updatePwdByUserId(session,password);
        return Result.ok(null);
    }
}
