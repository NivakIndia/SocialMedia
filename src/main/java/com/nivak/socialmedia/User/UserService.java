package com.nivak.socialmedia.User;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.nivak.socialmedia.Email.EmailService;
import com.nivak.socialmedia.SMSservice.SMSService;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private void MyUser(@Qualifier("users") UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @Autowired
    private EmailService emailService;

    @Autowired
    private SMSService smsService;

    // To Check Email
    public static boolean isEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    // To Check Number
    public static boolean isNumber(String input) {
        try {
            Long.parseLong(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Register user
    public void sendVerification(User user,String userid,int verificationToken,String code){
        if (isNumber(userid)) {
            String message = "Your OTP to verify your email with Nivak's media is: "+verificationToken;
            smsService.sendSMS(code+userid, message);
            user.setCode(code);
            userRepository.save(user);
        }
        else{
            userRepository.save(user);
            CompletableFuture.supplyAsync(()->{
                User saveUsers = userRepository.save(user);
                emailService.sendVerificationEmail(saveUsers);
                return saveUsers;
            });
        }
    }

    // Resend code
    public void resendCode(String userid,int verificationToken){
        User user = byUserId(userid);
        user.setVerificationToken(verificationToken);
        if (isNumber(userid)) {
            String code = user.getCode();
            String message = "Your OTP to verify your email with Nivak's media is: "+verificationToken;
            smsService.sendSMS(code+userid, message);
            userRepository.save(user);
        }
        else{
            userRepository.save(user);
            CompletableFuture.supplyAsync(()->{
                User saveUsers = userRepository.save(user);
                emailService.sendVerificationEmail(saveUsers);
                return saveUsers;
            });
        }
    }

    // Retrive all user
    public List<User> allUser(){
        return userRepository.findAll();
    }

    // Retrive user by userid
    public User byUserId(String userid){
        return userRepository.findByUserId(userid);
    }

    // Retrive user by username
    public User byUserName(String username){
        return userRepository.findByUserName(username);
    }

    // For user Login
    public User loginUser(String login){
        if (isEmail(login) || isNumber(login)) {
            return userRepository.findByUserId(login);
        } else {
            return userRepository.findByUserName(login);
        }
    }

    // Forget Password
    public void forgetPassword(String userid,int forgetPassToken){
        User user = byUserId(userid);
        user.setForgetPassToken(forgetPassToken);
        if (isNumber(userid)) {
            String code = user.getCode();
            String message = "Your OTP to rest your password with Nivak's media is: "+forgetPassToken;
            smsService.sendSMS(code+userid, message);
            userRepository.save(user);
        }
        else{
            userRepository.save(user);
            CompletableFuture.supplyAsync(()->{
                User saveUsers = userRepository.save(user);
                emailService.forgetPassword(saveUsers);
                return saveUsers;
            });
        }
    }

    // Change password Resend code
    public void changePasswordResendCode(String userid,int forgetPassToken){
        User user = byUserId(userid);
        user.setForgetPassToken(forgetPassToken);
        if (isNumber(userid)) {
            String code = user.getCode();
            String message = "Your OTP to reset your password with Nivak's media is: "+forgetPassToken;
            smsService.sendSMS(code+userid, message);
            userRepository.save(user);
        }
        else{
            userRepository.save(user);
            CompletableFuture.supplyAsync(()->{
                User saveUsers = userRepository.save(user);
                emailService.forgetPassword(saveUsers);
                return saveUsers;
            });
        }
    }
}
