package com.example.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.constants.Constants;
import com.example.entity.dto.Result;
import com.example.entity.dto.SessionShareDto;
import com.example.entity.enums.FileDelFlagEnums;
import com.example.entity.enums.ShareValidTypeEnums;
import com.example.entity.po.FileInfo;
import com.example.entity.po.FileShare;
import com.example.entity.po.User;
import com.example.entity.query.FileShareQuery;
import com.example.entity.vo.ShareInfoVO;
import com.example.mapper.FileShareMapper;
import com.example.service.FileShareService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class FileShareServiceImpl extends ServiceImpl<FileShareMapper,FileShare> implements FileShareService {

    @Resource
    private FileShareMapper fileShareMapper;

    /**
     * 根据条件查询列表
     */
    @Override
    public Result findListByParam(FileShareQuery param) {
        return this.fileShareMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Result findCountByParam(FileShareQuery param) {
        return this.fileShareMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public Result findList(FileShareQuery param) {
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Result add(FileShare bean) {
        return save(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Result addBatch(List<FileShare> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.fileShareMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Result addOrUpdateBatch(List<FileShare> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.fileShareMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据ShareId获取对象
     */
    @Override
    public Result getFileShareByShareId(String shareId) {
        QueryWrapper<FileShare> queryWrapper = new QueryWrapper();
        queryWrapper.eq("share_id",shareId);
        FileShare share = fileShareMapper.selectOne(queryWrapper);
        if (share == null || (share.getExpireTime() != null && new Date().after(share.getExpireTime()))) {
            return Result.fail("文件不存在或已过期");
        }
        return Result.ok();
    }

    /**
     * 根据ShareId修改
     */
    @Override
    public Result updateFileShareByShareId(FileShare fileShare, String shareId) {
        return this.fileShareMapper.updateByShareId(bean, shareId);
    }

    /**
     * 根据ShareId删除
     */
    @Override
    public Result deleteFileShareByShareId(String shareId) {
        return this.fileShareMapper.deleteByShareId(shareId);
    }

    @Override
    public void saveShare(FileShare share) {
        ShareValidTypeEnums typeEnum = ShareValidTypeEnums.getByType(share.getValidType());
        if (null == typeEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (typeEnum != ShareValidTypeEnums.FOREVER) {
            share.setExpireTime(DateUtil.getAfterDate(typeEnum.getDays()));
        }
        Date curDate = new Date();
        share.setShareTime(curDate);
        if (StringTools.isEmpty(share.getCode())) {
            share.setCode(StringTools.getRandomString(Constants.LENGTH_5));
        }
        share.setShareId(StringTools.getRandomString(Constants.LENGTH_20));
        this.fileShareMapper.insert(share);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFileShareBatch(String[] shareIdArray, String userId) {
        Result count = this.fileShareMapper.deleteFileShareBatch(shareIdArray, userId);
        if (count != shareIdArray.length) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }

    @Override
    public Result checkShareCode(String shareId, String code) {
        FileShare share = this.fileShareMapper.selectByShareId(shareId);
        if (null == share || (share.getExpireTime() != null && new Date().after(share.getExpireTime()))) {
            throw new BusinessException(ResponseCodeEnum.CODE_902);
        }
        if (!share.getCode().equals(code)) {
            throw new BusinessException("提取码错误");
        }

        //更新浏览次数
        this.fileShareMapper.updateShareShowCount(shareId);
        SessionShareDto shareSessionDto = new SessionShareDto();
        shareSessionDto.setShareId(shareId);
        shareSessionDto.setShareUserId(share.getUserId());
        shareSessionDto.setFileId(share.getFileId());
        shareSessionDto.setExpireTime(share.getExpireTime());
        return shareSessionDto;
    }
}