package com.nivak.socialmedia;

import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.nivak.socialmedia.User.User;
import com.nivak.socialmedia.User.UserRepository;
import com.nivak.socialmedia.User.UserService;


@Controller
@RequestMapping("/nivak/media")
public class SocialMediaController {

    // initializing fields
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;
    
    // Otp Generator
    public static int generateOtp(){
        int min = 100000;
        int max = 999999;

        Random random = new Random();
        return random.nextInt(max-min+1)+min;
    }


    // Register users
    @PostMapping("/register/")
    public ResponseEntity<String> registerUser(@RequestParam("username") String username,@RequestParam("fullname") String fullname,@RequestParam("userid") String userid,@RequestParam("password") String password,@RequestParam(value = "countrycode", required = false) String code){
        try {
            User user = new User();
            user.setUserName(username);
            user.setFullName(fullname);
            user.setUserId(userid);
            user.setPassword(password);
            int verificationToken = generateOtp();
            user.setVerificationToken(verificationToken);
            user.setAccountIsVerified(false);
            try {
                userService.sendVerification(user, userid, verificationToken,code);
                return ResponseEntity.ok("Registration successfull");
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Error in sending otp "+e);
            }
           
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error in Registration "+e);
        }
    }

    // Verify user
    @GetMapping("/register/verify/{userid}/")
    public ResponseEntity<String> verifyAccount(@PathVariable("userid") String userid){
        try {
            User user = userService.byUserId(userid);
            if (user != null) {
                user.setAccountIsVerified(true);
                userRepository.save(user);
                return ResponseEntity.ok("Email is verified");
            }
            else{
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Email not verified");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Email not verified");
        }
    }

    // Resend code
    @GetMapping("/register/verify/resendcode/{userid}/")
    public ResponseEntity<String> resendCode(@PathVariable("userid") String userid){
        try {
            int verificationToken = generateOtp();
            userService.resendCode(userid, verificationToken);
            return ResponseEntity.ok("Resend code");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("can't able to resend code");
        }
    }

    // For Login
    @GetMapping("/login/{login}/")
    public ResponseEntity<User> loginUser(@PathVariable("login") String login){
        return new ResponseEntity<>(userService.loginUser(login),HttpStatus.OK);
    }

    // Forget Password
    @GetMapping("/password/reset/{userid}/")
    public ResponseEntity<String> forgetpass(@PathVariable("userid") String userid){
        try {
            int forgetPassToken = generateOtp();
            User user = userService.byUserId(userid);
            if (user != null) {
                userService.forgetPassword(userid, forgetPassToken);
                return ResponseEntity.ok("Sent code");
            }
            else{
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error in password reset");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error in Password reset");
        }
    }

    // Change password
    @PostMapping("/password/reset/changepassword/")
    public ResponseEntity<String> changePassword(@RequestParam("userid") String userid, @RequestParam("newpassword") String newPassword){
        try {
            User user = userService.byUserId(userid);
            user.setPassword(newPassword);
            userRepository.save(user);
            return ResponseEntity.ok("Password changed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error in password reset");
        }
    }
    
    // Change password resend code
    @GetMapping("/password/reset/changepassword/resendcode/{userid}/")
    public ResponseEntity<String> changePasswordResendCode(@PathVariable("userid") String userid){
        try {
            int forgetPassToken = generateOtp();
            userService.forgetPassword(userid, forgetPassToken);
            return ResponseEntity.ok("Resend code");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("can't able to resend code");
        }
    }

    // Return user by email
    @GetMapping("/byuserid/{userid}/")
    public ResponseEntity<User> byUserid(@PathVariable("userid") String userid){
        return new ResponseEntity<User>(userService.byUserId(userid), HttpStatus.OK);
    }

    // Return all user
    @GetMapping("/allusers/")
    public ResponseEntity<List<User>> showAllUser(){
        return new ResponseEntity<>(userService.allUser(),HttpStatus.OK);
    }

}
