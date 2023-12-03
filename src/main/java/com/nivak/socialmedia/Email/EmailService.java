package com.nivak.socialmedia.Email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.nivak.socialmedia.User.User;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender javaMailSender;

    public void sendVerificationEmail(User users){
        MimeMessage message = javaMailSender.createMimeMessage();
        int token = users.getVerificationToken();
        try{
            MimeMessageHelper helper = new MimeMessageHelper(message,true);
            helper.setTo(users.getUserId());
            helper.setSubject("Email Verification");

            String emailContent = "Dear User,<br> Your OTP to verify your email with Nivak's Meida is <u><b>"+token+"</b></u>. <br> Please do not share this otp with anyone<br><br><br> Kind regards,<br>Nivak's Team";
            helper.setText(emailContent,true);
            javaMailSender.send(message);
        }
        catch(MessagingException e){
            e.printStackTrace();
        }
    }
    
    // Forget pass
    public void forgetPassword(User users){
        MimeMessage message = javaMailSender.createMimeMessage();
        int token = users.getForgetPassToken();
        try{
            MimeMessageHelper helper = new MimeMessageHelper(message,true);
            helper.setTo(users.getUserId());
            helper.setSubject("Email Verification");

            String emailContent = "Dear User,<br> Your OTP to reset your password with Nivak's Meida is <u><b>"+token+"</b></u>. <br> Please do not share this otp with anyone<br><br><br> Kind regards,<br>Nivak's Team";
            helper.setText(emailContent,true);
            javaMailSender.send(message);
        }
        catch(MessagingException e){
            e.printStackTrace();
        }
    }
}
