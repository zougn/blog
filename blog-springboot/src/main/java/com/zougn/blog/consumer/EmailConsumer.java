package com.zougn.blog.consumer;

import com.alibaba.fastjson.JSON;
import com.zougn.blog.dto.EmailDTO;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import static com.zougn.blog.constant.MQPrefixConst.EMAIL_QUEUE;

/**
 * 通知邮箱
 *
 * @author yezhqiu
 * @date 2021/06/13
 * @since 1.0.0
 **/
@Component
@RabbitListener(queues = EMAIL_QUEUE)
public class EmailConsumer {

    /**
     * 邮箱号
     */
    private String email;

    private JavaMailSender javaMailSender;

    public EmailConsumer(@Value("${spring.mail.username}") String email, JavaMailSender javaMailSender) {
        this.email = email;
        this.javaMailSender = javaMailSender;
    }

    @RabbitHandler
    public void process(byte[] data) {
        EmailDTO emailDTO = JSON.parseObject(new String(data), EmailDTO.class);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(email);
        message.setTo(emailDTO.getEmail());
        message.setSubject(emailDTO.getSubject());
        message.setText(emailDTO.getContent());
        javaMailSender.send(message);
    }

}
