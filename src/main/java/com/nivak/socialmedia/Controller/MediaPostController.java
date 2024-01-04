package com.nivak.socialmedia.Controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.nivak.socialmedia.Cloud.CloudService;
import com.nivak.socialmedia.Posts.Comment;
import com.nivak.socialmedia.Posts.CommentReply;
import com.nivak.socialmedia.Posts.Post;
import com.nivak.socialmedia.Posts.PostRepository;
import com.nivak.socialmedia.Posts.PostService;
import com.nivak.socialmedia.User.Notification;
import com.nivak.socialmedia.User.User;
import com.nivak.socialmedia.User.UserRepository;
import com.nivak.socialmedia.User.UserService;



@Controller
@RequestMapping("/nivak/media")
public class MediaPostController {
    // initializing fields
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CloudService cloudService;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    // userid is Email
    public static boolean isEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    // New Post
    @PostMapping("/newpost/")
    public ResponseEntity<String> newPost(@RequestParam("userid") String userid, @RequestParam("post") MultipartFile postFile, @RequestParam("description") String description) {
        try {
            List<Post> post = postService.allPost();
            Post newPost = new Post();

            String folderName = "";
            if(isEmail(userid)){
                String[] useridparts = userid.split("@");
                folderName += "Post/"+useridparts[0];
            }
            else{
                folderName += "Post/"+userid;
            }
            String postURL = cloudService.postUpload(postFile,folderName,UUID.randomUUID().toString());

            LocalDate currentDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
            LocalTime currentTime = LocalTime.now(ZoneId.of("Asia/Kolkata"));

            
            newPost.setPostId(post.size()+1);
            newPost.setUserId(userid);
            newPost.setPostURL(postURL);
            newPost.setPostDescription(description);
            newPost.setPostDate(currentDate.toString());
            newPost.setPostTime(currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")).toString());
            newPost.setPostLikes(new ArrayList<>());

            postRepository.save(newPost);

            return ResponseEntity.ok("Post successfull");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE).body("Cannot Post: "+e);
        }
    }
    

    // Delete Post

    // Return All Post
    @GetMapping("/allpost/")
    public ResponseEntity<List<Post>> allPost() {
        return new ResponseEntity<>(postService.allPost(),HttpStatus.OK);
    }

    // Return Post By UserId
    @GetMapping("/postbyuserid/{userid}/")
    public ResponseEntity<List<Post>> postByUserId(@PathVariable("userid") String userid){
        return new ResponseEntity<>(postService.postByUserId(userid),HttpStatus.OK);
    }

    // Return Post By PostId
    @GetMapping("/postbypostid/{postid}/")
    public ResponseEntity<Post> postByPostId(@PathVariable("postid") int postid){
        return new ResponseEntity<>(postService.postByPostId(postid),HttpStatus.OK);
    }

    // Like and DisLike Post
    @PostMapping("/intraction/")
    public ResponseEntity<String> postIntraction(@RequestParam("postid") int postid, @RequestParam("userid") String userid){
        try {
            Post post = postService.postByPostId(postid);
            List<String> likes = post.getPostLikes();
            if (likes.contains((userid))) {
                likes.remove(userid);
            } else {
                likes.add(userid);
                
                // Notification
                LocalDate currentDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
                LocalTime currentTime = LocalTime.now(ZoneId.of("Asia/Kolkata"));

                User postLiker = userService.byUserId(userid);
                User notifyUser = userService.byUserId(post.getUserId());
                if (!postLiker.getUserId().equals(notifyUser.getUserId())) {
                    List<Notification> notifications = notifyUser.getNotifications();
                    Notification notifi = new Notification();
                    notifi.setNotificationId(notifications.size()+1);
                    notifi.setPostid(postid);
                    notifi.setNotificationMessage(postLiker.getUserName()+" is liked your post");
                    notifi.setSeen(false);
                    notifi.setNotificationDate(currentDate.toString());
                    notifi.setNotificationTime(currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")).toString());
                    notifications.add(notifi);
                    notifyUser.setNotifications(notifications);
                    userRepository.save(notifyUser);
                    simpMessagingTemplate.convertAndSend("/function/notification", "Post Instaction: "+postid);
                }
            }
            post.setPostLikes(likes);
            postRepository.save(post);
            
            simpMessagingTemplate.convertAndSend("/function/intraction", "Post Instaction: "+postid);
            return ResponseEntity.ok("Intraction Successfull");
        } catch (NumberFormatException e) {
            System.out.println("Invalid Post ID");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Post ID");
        } catch (Exception e) {
            System.out.println("Error in Interactio");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error in Interaction");
        }
    }
    // Comment the post
    @PostMapping("/postcomment/")
    public ResponseEntity<String> postComment(@RequestParam("postid") int postid, @RequestParam("userid") String userid,@RequestParam("comment") String comment){
        try {
            Post post = postService.postByPostId(postid);
            List<Comment> comments = post.getPostComments();

            if (comments == null) {
                comments = new ArrayList<>();
            }

            LocalDate currentDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
            LocalTime currentTime = LocalTime.now(ZoneId.of("Asia/Kolkata"));

            Comment commentContent = new Comment();
            commentContent.setCommentId(comments.size()+1);
            commentContent.setCommenterUserId(userid);
            commentContent.setCommentMessage(comment);
            commentContent.setCommentDate(currentDate.toString());
            commentContent.setCommentTime(currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")).toString());
            commentContent.setCommentLikes(new ArrayList<>());
            commentContent.setCommentReplies(new ArrayList<>());
            comments.add(commentContent);

            post.setPostComments(comments);
            postRepository.save(post);

            // Notification
            User commentUser = userService.byUserId(userid);
            User notifyUser = userService.byUserId(post.getUserId());
            if(!commentUser.getUserId().equals(notifyUser.getUserId())){
                List<Notification> notifications = notifyUser.getNotifications();
                Notification notifi = new Notification();
                notifi.setNotificationId(notifications.size()+1);
                notifi.setPostid(postid);
                notifi.setNotificationMessage(commentUser.getUserName()+" has commented your post");
                notifi.setSeen(false);
                notifi.setNotificationDate(currentDate.toString());
                notifi.setNotificationTime(currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")).toString());
                notifications.add(notifi);
                notifyUser.setNotifications(notifications);
                userRepository.save(notifyUser);
            }

            simpMessagingTemplate.convertAndSend("/function/notification", "Notification");
            simpMessagingTemplate.convertAndSend("/function/postcomment", "Post Comment: "+postid);
            return ResponseEntity.ok("Intraction Successfull");
        } catch (NumberFormatException e) {
            System.out.println("Invalid Post ID");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Post ID");
        } catch (Exception e) {
            System.out.println("Error in Interactio");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error in Interaction");
        }
    }

    // Post Comment like
    @PostMapping("/commentintraction/")
    public ResponseEntity<String> commentIntraction(@RequestParam("postid") int postid, @RequestParam("commentid") int commentid, @RequestParam("userid") String userid){
        try {
            Post post = postService.postByPostId(postid);
            List<Comment> comments = post.getPostComments();
            Comment comment = postService.commentById(commentid, post);
            List<String> commentLikes = comment.getCommentLikes();

            if(commentLikes.contains(userid)){
                commentLikes.remove(userid);
            }else{
                commentLikes.add(userid);
                // Notification
                LocalDate currentDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
                LocalTime currentTime = LocalTime.now(ZoneId.of("Asia/Kolkata"));

                User commentLiker = userService.byUserId(userid);
                User notifyUser = userService.byUserId(comment.getCommenterUserId());
                if(!commentLiker.getUserId().equals(notifyUser.getUserId())){
                    List<Notification> notifications = notifyUser.getNotifications();
                    Notification notifi = new Notification();
                    notifi.setNotificationId(notifications.size()+1);
                    notifi.setPostid(postid);
                    notifi.setNotificationMessage(commentLiker.getUserName()+" has liked your comment");
                    notifi.setSeen(false);
                    notifi.setNotificationDate(currentDate.toString());
                    notifi.setNotificationTime(currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")).toString());
                    notifications.add(notifi);
                    notifyUser.setNotifications(notifications);
                    userRepository.save(notifyUser);
                    simpMessagingTemplate.convertAndSend("/function/notification", "Notification");
                }

            }
            
            comment.setCommentLikes(commentLikes);

            for (int i = 0; i < comments.size(); i++) {
                if (comments.get(i).getCommentId() == commentid) {
                    comments.set(i, comment);
                    break;
                }
            }

            post.setPostComments(comments);
            postRepository.save(post);

            simpMessagingTemplate.convertAndSend("/function/commentintraction", "Post commentintraction: "+postid);
            return ResponseEntity.ok("Comment Intraction Successfull"+comments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Comment Intraction Successfull");
        }
    }

    // Post Reply Comment
    @PostMapping("/postreplycomment/")
    public ResponseEntity<String> postReplyComment(@RequestParam("postid") int postid, @RequestParam("userid") String userid,@RequestParam("commentid") int commentid,@RequestParam("commentmsg") String commentmsg){
        try {
            Post post = postService.postByPostId(postid);
            List<Comment> comments = post.getPostComments();

            Comment comment = postService.commentById(commentid, post);
            List<CommentReply> commentReply = comment.getCommentReplies();
            
            CommentReply replycomment = new CommentReply();

            LocalDate currentDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
            LocalTime currentTime = LocalTime.now(ZoneId.of("Asia/Kolkata"));

            replycomment.setReplyId(commentReply.size()+1);
            replycomment.setReplyerUserId(userid);
            replycomment.setReplyDate(currentDate.toString());
            replycomment.setReplyTime(currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")).toString());
            replycomment.setReplyMessage(commentmsg);
            replycomment.setReplyLikes(new ArrayList<>());
            commentReply.add(replycomment);
            comment.setCommentReplies(commentReply);

            for (int i = 0; i < comments.size(); i++) {
                if (comments.get(i).getCommentId() == commentid) {
                    comments.set(i, comment);
                    break;
                }
            }

            post.setPostComments(comments);
            postRepository.save(post);

            // Notification
            User commentReplier = userService.byUserId(userid);
            User notifyUser = userService.byUserId(comment.getCommenterUserId());
            if(!commentReplier.getUserId().equals(notifyUser.getUserId())){
                List<Notification> notifications = notifyUser.getNotifications();
                Notification notifi = new Notification();
                notifi.setNotificationId(notifications.size()+1);
                notifi.setPostid(postid);
                notifi.setNotificationMessage(commentReplier.getUserName()+" has repliyed to your comment");
                notifi.setSeen(false);
                notifi.setNotificationDate(currentDate.toString());
                notifi.setNotificationTime(currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")).toString());
                notifications.add(notifi);
                notifyUser.setNotifications(notifications);
                userRepository.save(notifyUser);
            }

            simpMessagingTemplate.convertAndSend("/function/notification", "Notification");
            simpMessagingTemplate.convertAndSend("/function/postreplycomment", "Post reply comment: "+postid);
            return ResponseEntity.ok("Post Comment Reply Successfull");
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid ID");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error Interaction");
        }
    }

    @PostMapping("/replycommentintraction/")
    public ResponseEntity<String> replyCommentIntraction(@RequestParam("postid") int postid, @RequestParam("commentid") int commentid, @RequestParam("userid") String userid, @RequestParam("replyid") int replyid){
        try {
            Post post = postService.postByPostId(postid);
            List<Comment> comments = post.getPostComments();
            Comment comment = postService.commentById(commentid, post);
            List<CommentReply> commentReplies= comment.getCommentReplies();
            CommentReply commentReply = postService.replyById(replyid, comment);
            List<String> replyLike = commentReply.getReplyLikes();

            if (replyLike.contains(userid)) {
                replyLike.remove(userid);
            } else {
                replyLike.add(userid);
                // Notification
                LocalDate currentDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
                LocalTime currentTime = LocalTime.now(ZoneId.of("Asia/Kolkata"));

                User commentLiker = userService.byUserId(userid);
                User notifyUser = userService.byUserId(commentReply.getReplyerUserId());
                if (!commentLiker.getUserId().equals(notifyUser.getUserId())) {
                    List<Notification> notifications = notifyUser.getNotifications();
                    Notification notifi = new Notification();
                    notifi.setNotificationId(notifications.size()+1);
                    notifi.setPostid(postid);
                    notifi.setNotificationMessage(commentLiker.getUserName()+" has liked your reply");
                    notifi.setSeen(false);
                    notifi.setNotificationDate(currentDate.toString());
                    notifi.setNotificationTime(currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")).toString());
                    notifications.add(notifi);
                    notifyUser.setNotifications(notifications);
                    userRepository.save(notifyUser);
                    simpMessagingTemplate.convertAndSend("/function/notification", "Notification");
                }

            }
            commentReply.setReplyLikes(replyLike);
            
             for (int i = 0; i < commentReplies.size(); i++) {
                if (commentReplies.get(i).getReplyId() == replyid) {
                    commentReplies.set(i, commentReply);
                    break;
                }
            }
            
            comment.setCommentReplies(commentReplies);

            for (int i = 0; i < comments.size(); i++) {
                if (comments.get(i).getCommentId() == commentid) {
                    comments.set(i, comment);
                    break;
                }
            }

            post.setPostComments(comments);
            postRepository.save(post);
            simpMessagingTemplate.convertAndSend("/function/replycommentintraction", "Post reply comment intraction: "+postid);
            return ResponseEntity.ok("Comment Intraction Successfull"+post);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Comment Intraction Successfull");
        }
    }
 
}
