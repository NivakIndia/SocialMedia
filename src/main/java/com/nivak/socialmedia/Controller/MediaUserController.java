package com.nivak.socialmedia.Controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.nivak.socialmedia.Cloud.CloudService;
import com.nivak.socialmedia.User.Notification;
import com.nivak.socialmedia.User.User;
import com.nivak.socialmedia.User.UserRepository;
import com.nivak.socialmedia.User.UserService;





@Controller
@RequestMapping("/nivak/media")
public class MediaUserController {

    // initializing fields
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CloudService cloudService;
    
    // Otp Generator
    public static int generateOtp(){
        int min = 100000;
        int max = 999999;

        Random random = new Random();
        return random.nextInt(max-min+1)+min;
    }

    // To Check Email
    public static boolean isEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
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
            user.setUserFollowers(new ArrayList<>());
            user.setUserFollowings(new ArrayList<>());
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

    // Return user by userid
    @GetMapping("/byuserid/{userid}/")
    public ResponseEntity<User> byUserid(@PathVariable("userid") String userid){
        return new ResponseEntity<User>(userService.byUserId(userid), HttpStatus.OK);
    }

    // Return user by userid
    @GetMapping("/byusername/{username}/")
    public ResponseEntity<User> byUserName(@PathVariable("username") String username){
        return new ResponseEntity<User>(userService.byUserName(username), HttpStatus.OK);
    }

    // Return all user
    @GetMapping("/allusers/")
    public ResponseEntity<List<User>> showAllUser(){
        return new ResponseEntity<>(userService.allUser(),HttpStatus.OK);
    }

    // Upload Profile pic
    @PostMapping("/profilepic/")
    public ResponseEntity<String> uploadFile(@RequestParam("image") MultipartFile profileImage, @RequestParam("userid") String userid) throws IOException{
        
        try {
            User user = userService.byUserId(userid);
            String profileurl = user.getProfileURL();

            String imageName = "";
            if (isEmail(userid)) {
                String[] userIdParts = userid.split("@");
                imageName = userIdParts[0];
            } else {
                imageName = userid;
            }

            if (profileurl == null || profileurl=="") {
                String imageURL = cloudService.profileImage(profileImage,imageName);
                user.setProfileURL(imageURL);
                userRepository.save(user);
            } else {
                cloudService.deleteProfileImage(profileurl);
                String imageURL = cloudService.profileImage(profileImage,imageName);
                user.setProfileURL(imageURL);
                userRepository.save(user);
            }
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unsuccess: "+e);
        }
    }


    // Biometric update
    @PostMapping("/biometric/")
    public ResponseEntity<String> biometricUpdate(@RequestParam("userid") String userid,@RequestParam(value = "bio", required = false) String bio,@RequestParam(value = "gender",required = false) String gender){
        try {
            User user = userService.byUserId(userid);
            if (bio != null) {
                user.setUserBio(bio);
            }
            if (gender != null) {
                user.setGender(gender);
            }
            userRepository.save(user);
            return ResponseEntity.ok("Biometric Updated");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Biometric Cant be Updated: " + e);
        }
    }

    // User Friend 
    @PostMapping("/userfriend/")
    public ResponseEntity<String> userFriend(@RequestParam("userid") String userid, @RequestParam("userfriendid") String userFriendid) {
        try {
            User user = userService.byUserId(userid);
            User userFriend = userService.byUserId(userFriendid);
            List<String> friendFollower = userFriend.getUserFollowers();
            List<String> following = user.getUserFollowings();

            if (!following.contains(userFriendid)) {
                following.add(userFriendid);
                friendFollower.add(userid);

                // Notification
                LocalDate currentDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
                LocalTime currentTime = LocalTime.now(ZoneId.of("Asia/Kolkata"));

                List<Notification> notifications = userFriend.getNotifications();
                if(notifications == null){
                    notifications = new ArrayList<>();
                }
                Notification notifi = new Notification();
                notifi.setNotificationId(notifications.size()+1);
                notifi.setNotificationMessage(user.getUserName()+" is now Following you");
                notifi.setSeen(false);
                notifi.setNotificationDate(currentDate.toString());
                notifi.setNotificationTime(currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")).toString());
                notifi.setUserId(user.getUserId());
                System.out.println(notifi);
                notifications.add(notifi);
                userFriend.setNotifications(notifications);
                simpMessagingTemplate.convertAndSend("/function/notification", "Notification");
            
            } else {
                following.remove(userFriendid);
                friendFollower.remove(userid);
            }

            user.setUserFollowings(following);
            userFriend.setUserFollowers(friendFollower);
            userRepository.save(user);
            userRepository.save(userFriend);
            return ResponseEntity.ok("User Friend Status: Successfull");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("User Friend status: "+e);
        }
    }
    
    // Save Post
    @PostMapping("/savepost/")
    public ResponseEntity<String> savePost(@RequestParam("userid") String userid, @RequestParam("postid") Integer postId){
        try {
            User user = userService.byUserId(userid);
            List<Integer> savedPost = user.getSavedPost();
            if (savedPost == null) {
                savedPost = new ArrayList<>();
            }

            if (savedPost.contains(postId)) {
                savedPost.remove(postId);
            } else {
                savedPost.add(postId);
            }

            user.setSavedPost(savedPost);
            userRepository.save(user);

            return ResponseEntity.ok("Post save and unsave status: successfull");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Post save and unsave status: "+e);
        }
    }

    // Notification seen
    @PostMapping("/notificationseen/")
    public ResponseEntity<String> notificationSeen(@RequestParam("userid") String userid, @RequestParam("notificationid") int notificationid){
        try {
            User user = userService.byUserId(userid);
            List<Notification> notifications = user.getNotifications();
            for (Notification notification : notifications) {
                if(notification.getNotificationId() == notificationid){
                    notification.setSeen(true);
                }
            }

            user.setNotifications(notifications);
            userRepository.save(user);
            simpMessagingTemplate.convertAndSend("/function/notification", "Notification");
            return ResponseEntity.ok("Notification status: successfull");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Notification status: "+e);
        }
    }
}
