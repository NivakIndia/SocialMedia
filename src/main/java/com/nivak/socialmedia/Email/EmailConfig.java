package com.nivak.socialmedia.Email;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;


@Configuration
public class EmailConfig {
    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Bean
    JavaMailSender javaMailSender(){
        JavaMailSenderImpl eMailSenderImpl = new JavaMailSenderImpl();
        eMailSenderImpl.setHost(host);
        eMailSenderImpl.setPort(port);
        eMailSenderImpl.setUsername(username);
        eMailSenderImpl.setPassword(password);

        Properties properties = eMailSenderImpl.getJavaMailProperties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable","true");

        return eMailSenderImpl;
    }
}
