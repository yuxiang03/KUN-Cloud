package com.example.service;

import com.example.entity.dto.Result;
import com.example.entity.po.EmailCode;
import jakarta.servlet.http.HttpSession;

import java.util.List;


/**
 * 邮箱验证码 业务接口
 */
public interface EmailCodeService {

    /**
     * 根据条件查询列表
     */
    Result findListByParam(EmailCodeQuery param);

    /**
     * 根据条件查询列表
     */
    Result findCountByParam(EmailCodeQuery param);

    /**
     * 分页查询
     */
    Result findList(EmailCodeQuery param);

    /**
     * 新增
     */
    Result add(EmailCode bean);

    /**
     * 批量新增
     */
    Result addBatch(List<EmailCode> listBean);

    /**
     * 批量新增/修改
     */
    Result addOrUpdateBatch(List<EmailCode> listBean);

    /**
     * 根据EmailAndCode查询对象
     */
    Result getEmailCodeByEmailAndCode(String email, String code);


    /**
     * 根据EmailAndCode修改
     */
    Result updateEmailCodeByEmailAndCode(EmailCode bean, String email, String code);


    /**
     * 根据EmailAndCode删除
     */
    Result deleteEmailCodeByEmailAndCode(String email, String code);

    Result sendEmailCode(EmailCode emailCode, HttpSession session);

    void checkCode(String email, String code);
}