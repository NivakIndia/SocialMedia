package com.nivak.socialmedia.User;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nivak.socialmedia.Email.EmailService;
import com.nivak.socialmedia.Responses.Mention;
import com.nivak.socialmedia.Responses.UserDTO;
import com.nivak.socialmedia.SMSservice.SMSService;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;


    @Autowired
    private EmailService emailService;

    @Autowired
    private SMSService smsService;

    /* To Check Email */
    private boolean isEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    /* To Check Number */
    private boolean isNumber(String input) {
        try {
            Long.valueOf(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /* Register user */
    public String registerUser(User user){
        try {
            this.sendVerification(user, user.getUserId(), user.getVerificationToken(), user.getCode());
        } catch (Exception e) {
        }
        user.setPassword(this.encryptPassword(user.getPassword()));
        return userRepository.save(user).getId();
    }

    /* Send Verificaton Code */
    public void sendVerification(User user,String userid,int verificationToken,String code){
        if (this.isNumber(userid)) {
            String message = "Your OTP to verify your email with Nivak's media is: "+verificationToken;
            smsService.sendSMS(code+userid, message);
        }
        else{
            CompletableFuture.supplyAsync(()->{
                User saveUsers = userRepository.save(user);
                emailService.sendVerificationEmail(saveUsers);
                return saveUsers;
            });
        }
    }

    /* Encrypt Password */
    public String encryptPassword(String password){
        return Base64.getEncoder().encodeToString(password.getBytes());
    }

    /* Decrypt Password */
    public String decryptPassword(String passsword){
        return new String(Base64.getDecoder().decode(passsword));
    }

    /* Resend Code */
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

    /* Save User */
    public void saveUser(User user){
        userRepository.save(user);
    }

    /* Retrive all userids and usernames */
    public List<UserDTO> getAllUserIds$Names(){
        List<UserDTO> ids_Names = new ArrayList<>();

        for (User user : userRepository.findAll()) {
            ids_Names.add(new UserDTO(user.getId(), user.getUserId(), user.getUserName(), user.getFullName(), user.getProfileURL())); 
        }

        return ids_Names;
    }

    public List<Mention> getMention(){
        List<Mention> mentions = new ArrayList<>();
        List<User> Users = userRepository.findAll();
        for (User user : Users) {
            mentions.add(new Mention(user.getId(), user.getUserName(), user.getFullName(), user.getProfileURL()));
        }
        return mentions;
    }

    /* Retrive user by id */
    public User byId(String id){
        Optional<User> user = userRepository.findById(new ObjectId(id));
        return user.get();
    }

    /* Retrive all user */
    public List<User> allUsers(){
        return userRepository.findAll();
    }

    /* Retrive user by userid */
    public User byUserId(String userid){
        return userRepository.findByUserId(userid);
    }

    /* Retrive user by username */
    public User byUserName(String username){
        return userRepository.findByUserName(username);
    }

    /* For user Login */
    public User loginUser(String login){
        if (isEmail(login) || isNumber(login)) {
            return userRepository.findByUserId(login);
        } else {
            return userRepository.findByUserName(login);
        }
    }

    /* Forget Password */
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

    /* Change password Resend code */
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
