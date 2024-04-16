package com.example.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.constants.Constants;
import com.example.entity.dto.Result;
import com.example.entity.dto.SysSettingsDto;
import com.example.entity.enums.PageSize;
import com.example.entity.po.EmailCode;
import com.example.entity.po.User;
import com.example.entity.query.SimplePage;
import com.example.entity.vo.PaginationResultVO;
import com.example.mapper.EmailCodeMapper;
import com.example.mapper.UserMapper;
import com.example.service.EmailCodeService;
import com.example.service.UserService;
import com.example.utils.CreateCodeUtils;
import jakarta.annotation.Resource;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 邮箱验证码 业务接口实现
 */
@Service("emailCodeService")
public class EmailCodeServiceImpl extends ServiceImpl<EmailCodeMapper,EmailCode> implements EmailCodeService {
    @Resource
    private JavaMailSender javaMailSender;

    @Resource
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    /**
     * 根据条件查询列表
     */
    @Override
    public Result findListByParam(EmailCodeQuery param) {
        return emailCodeService.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Result findCountByParam(EmailCodeQuery param) {
        return emailCodeService.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public Result findList(EmailCodeQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<EmailCode> list = this.findListByParam(param);
        PaginationResultVO<EmailCode> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Result add(EmailCode bean) {
        return emailCodeService.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Result addBatch(List<EmailCode> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return emailCodeService.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Result addOrUpdateBatch(List<EmailCode> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return emailCodeMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据EmailAndCode获取对象
     */
    @Override
    public Result getEmailCodeByEmailAndCode(String email, String code) {
        return emailCodeService.selectByEmailAndCode(email, code);
    }

    /**
     * 根据EmailAndCode修改
     */
    @Override
    public Result updateEmailCodeByEmailAndCode(EmailCode bean, String email, String code) {
        return emailCodeService.updateByEmailAndCode(bean, email, code);
    }

    /**
     * 根据EmailAndCode删除
     */
    @Override
    public Result deleteEmailCodeByEmailAndCode(String email, String code) {
        return emailCodeService.deleteByEmailAndCode(email, code);
    }

    private void sendEmailCode(String toEmail, String code) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            //邮件发件人
            helper.setFrom(appConfig.getSendUserName());
            //邮件收件人 1或多个
            helper.setTo(toEmail);

            SysSettingsDto sysSettingsDto = redisComponent.getSysSettingsDto();

            //邮件主题
            helper.setSubject(sysSettingsDto.getRegisterEmailTitle());
            //邮件内容
            helper.setText(String.format(sysSettingsDto.getRegisterEmailContent(), code));
            //邮件发送时间
            helper.setSentDate(new Date());
            javaMailSender.send(message);
        } catch (Exception e) {
            logger.error("邮件发送失败", e);
            throw new BusinessException("邮件发送失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result sendEmailCode(EmailCode emailCode, HttpSession session) {
        try {
            if (!emailCode.getCode().equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))) {
                return Result.fail("图片验证码不正确");
            }
            //如果是注册，校验邮箱是否已存在
            if (emailCode.getStatus() == Constants.ZERO) {
                QueryWrapper<User> queryWrapper=new QueryWrapper<>();
                QueryWrapper<User> qw = queryWrapper.eq("email", emailCode.getEmail());
                User user = userService.getOne(qw);
                if (null != user) {
                    try {
                        throw new Exception("邮箱已经存在");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            String code = CreateCodeUtils.creatCode(Constants.LENGTH_5);
            sendEmailCode(emailCode.getEmail(), code);

            update().set("status",1).eq("status",0).eq("email",emailCode.getEmail());
            EmailCode em = new EmailCode();
            em.setCode(code);
            em.setEmail(emailCode.getEmail());
            emailCode.setStatus(Constants.ZERO);
            save(em);
            return Result.ok(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }
    }

    @Override
    public void checkCode(String email, String code) {
        EmailCode emailCode = emailCodeMapper.selectByEmailAndCode(email, code);
        if (null == emailCode) {
            throw new BusinessException("邮箱验证码不正确");
        }
        if (emailCode.getStatus() == 1 || System.currentTimeMillis() - emailCode.getCreateTime().getTime() > Constants.LENGTH_15 * 1000 * 60) {
            throw new BusinessException("邮箱验证码已失效");
        }
        emailCodeMapper.disableEmailCode(email);
    }
}