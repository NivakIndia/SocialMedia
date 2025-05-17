package com.nivak.socialmedia.Controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Api;
import com.nivak.socialmedia.Cloud.CloudService;
import com.nivak.socialmedia.Responses.ApiResponse;
import com.nivak.socialmedia.Responses.Mention;
import com.nivak.socialmedia.Responses.UserDTO;
import com.nivak.socialmedia.User.Notification;
import com.nivak.socialmedia.User.User;
import com.nivak.socialmedia.User.UserService;





@RestController
@RequestMapping("/nivak/user")
public class MediaUserController {

    /* initializing fields */
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;
    @Autowired
    private UserService userService;

    @Autowired
    private CloudService cloudService;
    
    /* Otp Generator */
    public static int generateOtp(){
        int min = 100000;
        int max = 999999;

        Random random = new Random();
        return random.nextInt(max-min+1)+min;
    }

    /* To Check Email */
    public static boolean isEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    /* Register users */
    @PostMapping("/register/")
    public ResponseEntity<ApiResponse<String>> registerUser(@RequestBody User user){
        ApiResponse<String> response = new ApiResponse<>();
        try {
            int verificationToken = generateOtp();
            user.setVerificationToken(verificationToken);
            user.setAccountIsVerified(false);
            user.setUserFollowers(new ArrayList<>());
            user.setUserFollowings(new ArrayList<>());
            try {
                String id = userService.registerUser(user);
                response.setValues(true, "User Registration successfull!", id);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                response.setValues(false, "User Registration unsuccessfull!", "");
                return ResponseEntity.badRequest().body(response);
            }
           
        } catch (Exception e) {
            response.setValues(false, "Server Error!", "");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /* Verify user  */
    @PostMapping("/register/verify/")
    public ResponseEntity<ApiResponse<String>> verifyAccount(@RequestParam("id") String id, @RequestParam("code") int code){
        System.out.println("In verify");
        ApiResponse<String> response = new ApiResponse<>();
        try {
            User user = userService.byId(id);
            if (user != null) {
                if(user.getVerificationToken() == code){
                    user.setAccountIsVerified(true);
                    userService.saveUser(user);
                    response.setValues(true, "Code Verification successfull", "");
                    return ResponseEntity.ok(response);
                }
                else{
                    response.setValues(false, "Incorrect Code", "");
                    return ResponseEntity.badRequest().body(response);
                }
                
            }
            else{
                response.setValues(false, "User Not Found", "");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.setValues(false, "", "");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /* Resend code */
    @GetMapping("/register/verify/resendcode/{userid}/")
    public ResponseEntity<ApiResponse<String>> resendCode(@PathVariable("userid") String userid){
        ApiResponse<String> response = new ApiResponse<>();
        try {
            int verificationToken = generateOtp();
            userService.resendCode(userid, verificationToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /* For Login */
    @PostMapping("/login/")
    public ResponseEntity<ApiResponse<List<String>>> loginUser(@RequestParam("userid") String userid, @RequestParam("password") String password){
        ApiResponse<List<String>> response = new ApiResponse<>();
        try {
            User user = userService.loginUser(userid);
            if(user != null){
                if(password.equals(userService.decryptPassword(user.getPassword()))){
                    if(user.isAccountIsVerified()){
                        List<String> res = new ArrayList<>(Arrays.asList(
                            user.getId().toString()
                        ));
                        response.setValues(true, "Welcome "+user.getUserName() +" to Nivak", res);
                        return ResponseEntity.ok(response);
                    }
                    else{
                        List<String> res = new ArrayList<>(Arrays.asList(
                            user.getId().toString(), "not Verified"
                        ));
                        response.setValues(true, "User is not verified", res);
                        return ResponseEntity.ok(response);
                    } 
                }
                else{
                    response.setValues(false, "Password InCorrect",null);
                    return ResponseEntity.ok(response);
                }
            }
            else{
                response.setValues(false, "User Not Found", null);
                return ResponseEntity.ok(response);
            }
            
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            response.setValues(false, "Server Error", null);
           return ResponseEntity.badRequest().body(response);
        }
    }

    /* Forget Password */
    @GetMapping("/password/reset/{userid}/")
    public ResponseEntity<ApiResponse<String>> forgetpass(@PathVariable("userid") String userid){
        ApiResponse<String> response = new ApiResponse<>();
        try {
            int forgetPassToken = generateOtp();
            User user = userService.loginUser(userid);
            if (user != null) {
                userService.forgetPassword(user.getUserId(), forgetPassToken);
                response.setValues(true, "Verification code send", user.getId().toString());
                return ResponseEntity.ok(response);
            }
            else{
                response.setValues(false, "User Not Found", "");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    /* Get Verification Code */
    @GetMapping("/getverificationcode/{userid}/")
    public ResponseEntity<ApiResponse<Integer>> getVerificationCode(@PathVariable("userid") String userid) {
        ApiResponse<Integer> response = new ApiResponse<>();
        try {
            User user = userService.loginUser(userid);
            response.setValues(true, "", user.getForgetPassToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(response);
        }
    }
    

    /* Change password */
    @PostMapping("/password/reset/changepassword/")
    public ResponseEntity<ApiResponse<String>> changePassword(@RequestParam("userid") String userid, @RequestParam("newpassword") String newPassword){
        ApiResponse<String> response = new ApiResponse<>();
        try {
            User user = userService.byUserId(userid);
            user.setForgetPassToken(generateOtp());
            user.setPassword(userService.encryptPassword(newPassword));
            userService.saveUser(user);
            response.setValues(true, "Password Changed Successfully", null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /* Change password resend code */
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

    /* Return userid by id */
    @GetMapping("/useridbyid/{id}/")
    public ResponseEntity<ApiResponse<String>> byUserid(@PathVariable("id") String id){
        ApiResponse<String> response = new ApiResponse<>();
        User user = userService.byId(id);
        response.setValues(true, "Fetching User id", user.getUserId());
        return ResponseEntity.ok(response);
    }

    /* Return all user */
    @GetMapping("/getuserids$names/")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAlluserIds(){
        ApiResponse<List<UserDTO>> response = new ApiResponse<>();
        response.setValues(true, "data retrived", userService.getAllUserIds$Names());
        return ResponseEntity.ok(response);
    }

    

    @GetMapping("/getmentions/")
    public ResponseEntity<ApiResponse<List<Mention>>> getMentions(){
        ApiResponse<List<Mention>> response = new ApiResponse<>();
        response.setValues(true, "data retrived", userService.getMention());
        return ResponseEntity.ok(response);
        
    }


    /* Get User Suggestion */
    @PostMapping("/suggestions/")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getSuggestions(@RequestParam("id") String id){
        ApiResponse<List<UserDTO>> response = new ApiResponse<>();
        try {
            List<UserDTO> suggests = new ArrayList<>();
            List<User> users = userService.allUsers();
            User current_user = userService.byId(id);

            for(User user : users){
                if(!user.getId().equals(current_user.getId()) && !user.getUserFollowers().contains(id)){
                    suggests.add(new UserDTO(user.getId(),user.getUserId(), user.getFullName(), user.getUserName(), user.getProfileURL()));
                }
            }
            response.setValues(true, "", suggests);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(response);
        }

        
    }


    /* Get User Data */
    @PostMapping("/getuserdata/")
    public ResponseEntity<ApiResponse<User>> getMethodName(@RequestParam("id") String id) {
        ApiResponse<User> response = new ApiResponse<>();
        try {
            User user = userService.byId(id);
            user.setPassword("***********");
            user.setForgetPassToken(0);
            user.setVerificationToken(0);
            response.setValues(true, "User Fetched", user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setValues(false, "Server Down", null);
            return ResponseEntity.badRequest().body(response);
        }
    }


    /* User Friend */
    @PostMapping("/userfriend/")
    public ResponseEntity<ApiResponse<String>> userFriend(@RequestParam("userid") String userid, @RequestParam("userfriendid") String userFriendid) {
        ApiResponse<String> response = new ApiResponse<>();
        System.out.println("Friend");
        try {
            User user = userService.byId(userid);
            User userFriend = userService.byId(userFriendid);
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
                int notifiId = userFriend.getNotificationId()+1;
                notifi.setId(notifiId);
                notifi.setNotificationMessage(user.getUserName()+" is now Following you");
                notifi.setSeen(false);
                notifi.setNotificationDate(currentDate.toString());
                notifi.setNotificationTime(currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                notifi.setUserId(user.getId());

                notifications.add(notifi);

                userFriend.setNotificationId(notifiId);
                userFriend.setNotifications(notifications);
                simpMessagingTemplate.convertAndSend("/function/notification", "Notification");
            
            } else {
                following.remove(userFriendid);
                friendFollower.remove(userid);
            }

            user.setUserFollowings(following);
            userFriend.setUserFollowers(friendFollower);
            userService.saveUser(user);
            userService.saveUser(userFriend);
            return ResponseEntity.ok(response);
        } catch (MessagingException e) {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /* To Get User Friends */
    @PostMapping("/userfriends/")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getUserFriends(@RequestParam("id") String id, @RequestParam("type") String type){
        ApiResponse<List<UserDTO>> response = new ApiResponse<>();
        try {
            List<UserDTO> friends = new ArrayList<>();
            if(type.equals("followers")){
                List<String> users = userService.byId(id).getUserFollowers();
                for(String user : users){
                    User friend = userService.byId(user);
                    friends.add(new UserDTO(friend.getId(),friend.getUserId(), friend.getFullName(), friend.getUserName(), friend.getProfileURL()));
                }
            } else if(type.equals("followings")){
                List<String> users = userService.byId(id).getUserFollowings();
                for(String user : users){
                    User friend = userService.byId(user);
                    friends.add(new UserDTO(friend.getId(),friend.getUserId(), friend.getFullName(), friend.getUserName(), friend.getProfileURL()));
                }
            }
            response.setValues(true, "Success", friends);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /* Upload Profile pic */
    @PostMapping("/profilepic/")
    public ResponseEntity<ApiResponse<String>> uploadFile(@RequestParam("image") MultipartFile profileImage, @RequestParam("userid") String userid) throws IOException{
        ApiResponse<String> response = new ApiResponse<>();
        try {
            User user = userService.byId(userid);
            String profileurl = user.getProfileURL();

            String imageName = userid;

            if (profileurl == null || profileurl=="") {
                String imageURL = cloudService.profileImage(profileImage,imageName);
                user.setProfileURL(imageURL);
                userService.saveUser(user);
            } else {
                cloudService.deleteProfileImage(profileurl);
                String imageURL = cloudService.profileImage(profileImage,imageName);
                user.setProfileURL(imageURL);
                userService.saveUser(user);
            }
            response.setValues(true, "Image Uploaded", "Success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /* Save Post */
    @PostMapping("/savepost/")
    public ResponseEntity<ApiResponse<String>> savePost(@RequestParam("userid") String userid, @RequestParam("postid") String postId){
        System.out.println(postId);
        ApiResponse<String> response = new ApiResponse<>();
        try {
            User user = userService.byId(userid);
            List<String> savedPost = user.getSavedPost();
            if (savedPost == null) {
                savedPost = new ArrayList<>();
            }

            if (savedPost.contains(postId)) {
                savedPost.remove(postId);
            } else {
                savedPost.add(postId);
            }

            user.setSavedPost(savedPost);
            userService.saveUser(user);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /* Biometric update */
    @PostMapping("/biometric/")
    public ResponseEntity<ApiResponse<String>> biometricUpdate(@RequestParam("userid") String userid,@RequestParam(value = "bio", required = false) String bio,@RequestParam(value = "gender",required = false) String gender){
        ApiResponse<String> response = new ApiResponse<>();
        try {
            User user = userService.byId(userid);
            if (bio != null) {
                user.setUserBio(bio);
            }
            if (gender != null) {
                user.setGender(gender);
            }
            userService.saveUser(user);
            response.setValues(true, "Biometric Updated", "");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(response);
        }
    }


    
    /* Notification seen */
    @PostMapping("/notificationseen/")
    public ResponseEntity<ApiResponse<String>> notificationSeen(@RequestParam("userid") String userid, @RequestParam("notificationid") int notificationid){
        ApiResponse<String> response = new ApiResponse<>();
        try {
            User user = userService.byId(userid);
            List<Notification> notifications = user.getNotifications();
            for (Notification notification : notifications) {
                if(notification.getId() == notificationid){
                    notification.setSeen(true);
                }
            }

            user.setNotifications(notifications);
            userService.saveUser(user);
            simpMessagingTemplate.convertAndSend("/function/notification", "Notification");
            response.setValues(true, "notification seen", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
