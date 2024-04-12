package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Date;

@SpringBootTest
class KunpanApplicationTests {

    @Autowired
    private JavaMailSender javaMailSender;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    public void sendSimpleMail() {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setSubject("简单邮件标题" + System.currentTimeMillis());
        mail.setText("简单邮件内容");
        mail.setTo("vagrant.yuxiang@gmail.com");
        mail.setFrom("2472503964@qq.com");
        mail.setSentDate(new Date());
        javaMailSender.send(mail);
        System.out.println("简单发送邮件完成");
    }

    @Test
    void sendMessage(){
        rabbitTemplate.convertAndSend("simple.fanout",null,"kkkk");
    }
}
