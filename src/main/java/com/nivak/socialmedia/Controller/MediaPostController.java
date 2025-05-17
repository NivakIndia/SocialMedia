package com.nivak.socialmedia.Controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.nivak.socialmedia.Cloud.CloudService;
import com.nivak.socialmedia.Posts.Comment;
import com.nivak.socialmedia.Posts.CommentReply;
import com.nivak.socialmedia.Posts.Post;
import com.nivak.socialmedia.Posts.PostRepository;
import com.nivak.socialmedia.Posts.PostService;
import com.nivak.socialmedia.Responses.ApiResponse;
import com.nivak.socialmedia.Responses.FrontPost;
import com.nivak.socialmedia.User.Notification;
import com.nivak.socialmedia.User.User;
import com.nivak.socialmedia.User.UserRepository;
import com.nivak.socialmedia.User.UserService;




@Controller
@RequestMapping("/nivak/post")
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

    /* New Post */
    @PostMapping("/newpost/")
    public ResponseEntity<ApiResponse<String>> newPost(@RequestParam("id") String id, @RequestParam("post") MultipartFile postFile, @RequestParam("description") String description) {
        ApiResponse<String> response = new ApiResponse<>();
        try {
            Post newPost = new Post();

            String folderName = "Post/"+id;

            String postURL = cloudService.postUpload(postFile,folderName,UUID.randomUUID().toString());

            LocalDate currentDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
            LocalTime currentTime = LocalTime.now(ZoneId.of("Asia/Kolkata"));

            newPost.setUserId(id);
            newPost.setPostURL(postURL);
            newPost.setPostDescription(description);
            newPost.setPostDate(currentDate.toString());
            newPost.setPostTime(currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            newPost.setPostLikes(new ArrayList<>());

            postRepository.save(newPost);
            response.setValues(true, "Post Uploaded", "");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            response.setValues(false, "Server Down", "");
            return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE).body(response);
        }
    }

    /* Get User Post */
    @PostMapping("/profileposts/")
    public ResponseEntity<ApiResponse<List<String>>> profilePosts(@RequestParam("id") String id) {
        ApiResponse<List<String>> response = new ApiResponse<>();
        try {

            List<String> posts = postService.profilePosts(id);
            response.setValues(true, "Fetched Posts", posts);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setValues(false, e.getMessage(), null);
           return ResponseEntity.badRequest().body(response);
        }
    }

    /* Get User Post */
    @PostMapping("/savedposts/")
    public ResponseEntity<ApiResponse<List<String>>> savedPosts(@RequestParam("id") String id) {
        ApiResponse<List<String>> response = new ApiResponse<>();
        try {

            List<String> posts = postService.savedPosts(id);
            response.setValues(true, "Fetched Posts", posts);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setValues(false, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /* Get Post By Id */
    @PostMapping("/postbyid/")
    public ResponseEntity<ApiResponse<Post>> getPostById(@RequestParam("id") String id){
        ApiResponse<Post> response = new ApiResponse<>();
        try {

            Post post = postService.getPostById(id);
            response.setValues(true, "Fetched Posts", post);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setValues(false, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }

    /* Post Intraction */
    @PostMapping("/intraction/")
    public ResponseEntity<ApiResponse<String>> postIntraction(@RequestParam("postid") String postid, @RequestParam("userid") String userid){
        ApiResponse<String> response = new ApiResponse<>();
        try {
            Post post = postService.getPostById(postid);
            List<String> likes = post.getPostLikes();
            if (likes.contains((userid))) {
                likes.remove(userid);
            } else {
                likes.add(userid);
                
                // Notification
                LocalDate currentDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
                LocalTime currentTime = LocalTime.now(ZoneId.of("Asia/Kolkata"));
                if(!post.getUserId().equals(userid)){
                    User postLiker = userService.byId(userid);
                    User notifyUser = userService.byId(post.getUserId());
                    List<Notification> notifications = notifyUser.getNotifications();
                    Notification notifi = new Notification();
                    int id = notifyUser.getNotificationId()+1;

                    notifi.setId(id);
                    notifi.setPostId(post.getId());
                    notifi.setNotificationMessage(postLiker.getUserName()+" is liked your post");
                    notifi.setSeen(false);
                    notifi.setNotificationDate(currentDate.toString());
                    notifi.setNotificationTime(currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));

                    notifications.add(notifi);
                    notifyUser.setNotificationId(id);
                    notifyUser.setNotifications(notifications);
                    userRepository.save(notifyUser);
                    simpMessagingTemplate.convertAndSend("/function/notification", postid);
                    
                }

            }
            post.setPostLikes(likes);
            postRepository.save(post);
            
            simpMessagingTemplate.convertAndSend("/function/intraction", postid);
            
            return ResponseEntity.ok(response);
        } catch (MessagingException e) {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /* Comment the post */
    @PostMapping("/postcomment/")
    public ResponseEntity<ApiResponse<String>> postComment(@RequestParam("postid") String postid, @RequestParam("userid") String userid, @RequestParam("comment") String comment){
        ApiResponse<String> response = new ApiResponse<>();
        try {
            Post post = postService.getPostById(postid);
            List<Comment> comments = post.getPostComments();

            if (comments == null) {
                comments = new ArrayList<>();
            }

            LocalDate currentDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
            LocalTime currentTime = LocalTime.now(ZoneId.of("Asia/Kolkata"));

            Comment commentContent = new Comment();
            int commentId = post.getPostCommentId()+1;
            commentContent.setCommentId(commentId);
            commentContent.setCommenterUserId(userid);
            commentContent.setCommentMessage(comment);
            commentContent.setCommentDate(currentDate.toString());
            commentContent.setCommentTime(currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            commentContent.setCommentLikes(new ArrayList<>());
            commentContent.setCommentReplies(new ArrayList<>());
            comments.add(commentContent);

            post.setPostComments(comments);
            post.setPostCommentId(commentId);
            postRepository.save(post);

            // Notification
            if(!post.getUserId().equals(userid)){
                User commentUser = userService.byId(userid);
                User notifyUser = userService.byId(post.getUserId());
                List<Notification> notifications = notifyUser.getNotifications();
                Notification notifi = new Notification();
                int id = notifyUser.getNotificationId()+1;

                notifi.setId(id);
                notifi.setPostId(postid);
                notifi.setCommentId(commentId);
                notifi.setNotificationMessage(commentUser.getUserName()+" has commented your post " + comment.substring(0, 10)+"...");
                notifi.setSeen(false);
                notifi.setNotificationDate(currentDate.toString());
                notifi.setNotificationTime(currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));

                notifications.add(notifi);
                notifyUser.setNotifications(notifications);
                notifyUser.setNotificationId(id);
                userRepository.save(notifyUser);
            }

            simpMessagingTemplate.convertAndSend("/function/notification", "Notification");
            simpMessagingTemplate.convertAndSend("/function/postcomment", "Post Comment: "+postid);
            return ResponseEntity.ok(response);
        } catch (MessagingException e) {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /* Post Comment like */
    @PostMapping("/commentintraction/")
    public ResponseEntity<ApiResponse<String>> commentIntraction(@RequestParam("postid") String postid, @RequestParam("commentid") int commentid, @RequestParam("userid") String userid){
        ApiResponse<String> response = new ApiResponse<>();
        try {
            Post post = postService.getPostById(postid);
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

                User commentLiker = userService.byId(userid);
                User notifyUser = userService.byId(comment.getCommenterUserId());
                if(!commentLiker.getUserId().equals(notifyUser.getUserId())){
                    List<Notification> notifications = notifyUser.getNotifications();
                    Notification notifi = new Notification();
                    int id = notifyUser.getNotificationId()+1;

                    notifi.setId(id);
                    notifi.setPostId(postid);
                    notifi.setNotificationMessage(commentLiker.getUserName()+" has liked your comment");
                    notifi.setCommentId(commentid);
                    notifi.setSeen(false);
                    notifi.setNotificationDate(currentDate.toString());
                    notifi.setNotificationTime(currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));

                    notifications.add(notifi);
                    notifyUser.setNotifications(notifications);
                    notifyUser.setNotificationId(id);
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
            return ResponseEntity.ok(response);
        } catch (MessagingException e) {
            return ResponseEntity.badRequest().body(response);
        }
    }


    /* Post Reply Comment */
    @PostMapping("/postreplycomment/")
    public ResponseEntity<ApiResponse<String>> postReplyComment(@RequestParam("postid") String postid, @RequestParam("userid") String userid,@RequestParam("commentid") int commentid,@RequestParam("commentmsg") String commentmsg){
        ApiResponse<String> response = new ApiResponse<>();
        try {
            Post post = postService.getPostById(postid);
            List<Comment> comments = post.getPostComments();

            Comment comment = postService.commentById(commentid, post);
            List<CommentReply> commentReply = comment.getCommentReplies();
            
            CommentReply replycomment = new CommentReply();

            LocalDate currentDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
            LocalTime currentTime = LocalTime.now(ZoneId.of("Asia/Kolkata"));

            int replyId = comment.getReplyId()+1;
            replycomment.setReplyId(replyId);
            replycomment.setReplyerUserId(userid);
            replycomment.setReplyDate(currentDate.toString());
            replycomment.setReplyTime(currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
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
            comment.setReplyId(replyId);
            post.setPostComments(comments);
            postRepository.save(post);

            // Notification
            if(!post.getUserId().equals(userid)){
                User commentReplier = userService.byId(userid);
                User notifyUser = userService.byId(comment.getCommenterUserId());
                List<Notification> notifications = notifyUser.getNotifications();
                Notification notifi = new Notification();
                int id = notifyUser.getNotificationId()+1;

                notifi.setId(id);
                notifi.setPostId(postid);
                notifi.setCommentId(commentid);
                notifi.setNotificationMessage(commentReplier.getUserName()+" has repliyed to your comment as "+ (commentmsg.length()>10? (commentmsg.substring(0, 10)+"..."):commentmsg));
                notifi.setSeen(false);
                notifi.setNotificationDate(currentDate.toString());
                notifi.setNotificationTime(currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));

                notifications.add(notifi);
                notifyUser.setNotificationId(id);
                notifyUser.setNotifications(notifications);
                userRepository.save(notifyUser);
            }

            simpMessagingTemplate.convertAndSend("/function/notification", "Notification");
            simpMessagingTemplate.convertAndSend("/function/postreplycomment", "Post reply comment: "+postid);
            return ResponseEntity.ok(response);
        } catch (NumberFormatException | MessagingException e) {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /* Post Reply comment intraction */
    @PostMapping("/replycommentintraction/")
    public ResponseEntity<ApiResponse<String>> replyCommentIntraction(@RequestParam("postid") String postid, @RequestParam("commentid") int commentid, @RequestParam("userid") String userid, @RequestParam("replyid") int replyid){
        ApiResponse<String> response = new ApiResponse<>();
        try {
            Post post = postService.getPostById(postid);
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

                
                if (!post.getUserId().equals(userid)) {
                    User commentLiker = userService.byId(userid);
                    User notifyUser = userService.byId(commentReply.getReplyerUserId());
                    List<Notification> notifications = notifyUser.getNotifications();
                    Notification notifi = new Notification();
                    int id = notifyUser.getNotificationId()+1;

                    notifi.setId(id);
                    notifi.setPostId(postid);
                    notifi.setCommentId(commentid);
                    notifi.setNotificationMessage(commentLiker.getUserName()+" has liked your reply "+ (commentReply.getReplyMessage().length()>10? (commentReply.getReplyMessage().substring(0, 10)+"..."):commentReply.getReplyMessage()));
                    notifi.setSeen(false);
                    notifi.setNotificationDate(currentDate.toString());
                    notifi.setNotificationTime(currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                    notifications.add(notifi);
                    notifyUser.setNotificationId(id);
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
            return ResponseEntity.ok(response);
        } catch (MessagingException e) {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /* Get Front Post */
    @PostMapping("/frontposts/")
    public ResponseEntity<ApiResponse<List<FrontPost>>> getFrontPost(@RequestParam("id") String id){
        ApiResponse<List<FrontPost>> response = new ApiResponse<>();
        try {
            List<FrontPost> frontPosts = new ArrayList<>();
            User user = userService.byId(id);
            List<Post> posts = postService.allPost();
            for (Post post : posts) {
                if(user.getId().equals(post.getUserId()) || user.getUserFollowings().contains(post.getUserId())){
                    frontPosts.add(new FrontPost(post.getId(), post.getUserId(), post.getPostDate(), post.getPostTime()));
                }
            }

            response.setValues(true, "", frontPosts);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/allposts/")
    public ResponseEntity<Page<String>> getPosts(@RequestParam("page") int page, @RequestParam("size") int size, @RequestParam("id") String id, @RequestParam("isReels") boolean isReels) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findAll(pageable);

        List<String> userFollowings = userService.byId(id).getUserFollowings();

        List<String> filteredPosts = posts.getContent().stream()
                .filter(post ->
                        !post.getUserId().equals(id) &&
                        !userFollowings.contains(post.getUserId()))
                .filter(post -> 
                        !isReels || (post.getPostURL() != null && (post.getPostURL().endsWith(".mp4") || post.getPostURL().endsWith(".mov") || post.getPostURL().endsWith(".avi"))))
                .map(Post::getId)
                .collect(Collectors.toList());

        // Create a pageable response for filtered posts
        int start = Math.min((int) pageable.getOffset(), filteredPosts.size());
        int end = Math.min((start + pageable.getPageSize()), filteredPosts.size());

        List<String> paginatedPosts = filteredPosts.subList(start, end);
        Page<String> responsePage = new PageImpl<>(paginatedPosts, pageable, filteredPosts.size());

        return ResponseEntity.ok(responsePage);
    }

    // Return Post By UserId
    @GetMapping("/postbyuserid/{userid}/")
    public ResponseEntity<List<Post>> postByUserId(@PathVariable("userid") String userid){
        return new ResponseEntity<>(postService.postByUserId(userid),HttpStatus.OK);
    }

    // Return Post By PostId
    @GetMapping("/postbypostid/{postid}/")
    public ResponseEntity<Post> postByPostId(@PathVariable("postid") int postid){
        return new ResponseEntity<>(postService.postByPostId(""),HttpStatus.OK);
    }

    // Like and DisLike Post
    
    

    
 
}
