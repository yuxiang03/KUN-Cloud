package com.example.service;

import com.example.entity.dto.Result;
import com.example.entity.po.FileShare;
import com.example.entity.query.FileShareQuery;

import java.util.List;

/**
 * 分享信息 业务接口
 */
public interface FileShareService {

    /**
     * 根据条件查询列表
     */
    Result findListByParam(FileShareQuery param);

    /**
     * 根据条件查询列表
     */
    Result findCountByParam(FileShareQuery param);

    /**
     * 分页查询
     */
    Result findListByPage(FileShareQuery param);

    /**
     * 新增
     */
    Result add(FileShare bean);

    /**
     * 批量新增
     */
    Result addBatch(List<FileShare> listBean);

    /**
     * 批量新增/修改
     */
    Result addOrUpdateBatch(List<FileShare> listBean);

    /**
     * 根据ShareId查询对象
     */
    Result getFileShareByShareId(String shareId);


    /**
     * 根据ShareId修改
     */
    Result updateFileShareByShareId(FileShare bean, String shareId);


    /**
     * 根据ShareId删除
     */
    Result deleteFileShareByShareId(String shareId);

    void saveShare(FileShare share);

    void deleteFileShareBatch(String[] shareIdArray, String userId);

    Result checkShareCode(  String shareId,String code);
}