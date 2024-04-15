package com.example.controller;

import com.example.entity.constants.Constants;
import com.example.entity.dto.CreateImageCode;
import com.example.entity.dto.LoginFormDTO;
import com.example.entity.dto.Result;
import com.example.entity.dto.SessionWebUserDto;
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

import static com.example.utils.RedisContants.LOGIN_TOKEN_KEY;

@RestController
public class AccountController{
    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

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
    public Result sendEmailCode() {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))) {
                return Result.fail("图片验证码不正确");
            }
            emailCodeService.sendEmailCode(email, type);
            return Result.ok(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }
    }

    //注册
    @RequestMapping("/register")
    public Result register(HttpSession session,
                               String email,
                               String nickName,
                               String password,
                               String checkCode,
                               String emailCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                Result.fail("图片验证码不正确");
            }
            userInfoService.register(email, nickName, password, emailCode);
            return Result.ok(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }
    
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm){
        return userService.login(loginForm);

    @RequestMapping("/resetPwd")
    public Result resetPwd(HttpSession session,
                               String email,
                               String password,
                               String checkCode,
                               String emailCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                return Result.fail("图片验证码不正确");
            }
            userInfoService.resetPwd(email, password, emailCode);
            return Result.ok(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    @RequestMapping("/getAvatar/{userId}")
    public void getAvatar(HttpServletResponse response,
                          @PathVariable("userId") String userId) {
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
    public Result getUserInfo(HttpSession session) {
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        return Result.ok(sessionWebUserDto);
    }

    @RequestMapping("/getUseSpace")
    public Result getUseSpace(HttpSession session) {
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        return Result.ok(redisComponent.getUserSpaceUse(sessionWebUserDto.getUserId()));
    }

    @RequestMapping("/logout")
    public Result logout(@RequestBody LoginFormDTO loginForm) {
        stringRedisTemplate.delete(LOGIN_TOKEN_KEY);
        return Result.ok(null);
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

    @RequestMapping("/updatePassword")
    public Result updatePassword(HttpSession session,
                                     String password) {
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        User user = new User();
        user.setPassword(StringTools.encodeByMD5(password));
        userService.updateUserInfoByUserId(userInfo, sessionWebUserDto.getUserId());
        return Result.ok(null);
    }
}
