package com.example.utils;

import com.alibaba.fastjson.JSONObject;
import com.example.entity.dto.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {
    //    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){
//        if (UserHolder.getUser()==null){
//            response.setStatus(401);
//            return false;
//        }
//        return true;
//    }
    @Override   //目标资源方法放行前运行  返回true,放行  返回false,不放行
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //1.获取请求url
        String url = request.getRequestURL().toString();
        //2.判断请求url中是否包含login如果包含则放行
        if (url.contains("login")) {
            return true;
        }
        //3.获取请求头中的令牌
        String jwt = request.getHeader("token");
        //4.判断令牌是否存在,如果不存在则返回错误结果(未登录)
        if (!StringUtils.hasLength(jwt)) {
            log.info("请求头为空返回未登陆的信息");
            Result error = Result.fail("NOT_LOGIN");
            //手动转换 对象转换为json------>阿里巴巴fastjson
            String notLogin = JSONObject.toJSONString(error);
            response.getWriter().write(notLogin);
            return false;
        }
        //5.解析jwt 如果解析失败,返回错误结果(未登录)
        try {
            JwtUtils.parseJWT(jwt);

        } catch (Exception e) { //jwt解析失败
            e.printStackTrace();
            log.info("解析令牌失败返回未登录错误信息");
            Result error = Result.fail("NOT_LOGIN");
            //手动转换 对象转换为json------>阿里巴巴fastjson
            String notLogin = JSONObject.toJSONString(error);
            response.getWriter().write(notLogin);
            return false;
        }
        //6.放行
        log.info("令牌合法放行");
        return true;
    }


}
