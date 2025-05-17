package com.nivak.socialmedia.Posts;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nivak.socialmedia.User.UserService;

@Service
public class PostService {
    
    @Autowired 
    private PostRepository postRepository;

    @Autowired
    private UserService userService;


    /* Profile Posts */
    public List<String> profilePosts(String id){
        List<Post> posts = postRepository.findByUserId(id);
        List<String> postIds = new ArrayList<>();
        for (Post post : posts) {
            postIds.add(post.getId());
        }
        return postIds;
    }

    /* Saved Posts */
    public List<String> savedPosts(String id){
        List<String> savedPostIds = userService.byId(id).getSavedPost();
        List<String> postIds = new ArrayList<>();
        if(savedPostIds == null) return postIds;
        
        for (String postId : savedPostIds) {
            postIds.add(postRepository.findById(new ObjectId(postId)).get().getId());
        }
        return postIds;
    }

    /* Post by Id */
    public Post getPostById(String id) {
        return postRepository.findById(new ObjectId(id)).get();
    }




    public Post postByPostId(String id){
        return postRepository.findById(new ObjectId(id)).get();
    }
    
    // All post
    public List<Post> allPost(){
        return postRepository.findAll();
    }

    // Post by userID
    public List<Post> postByUserId(String userid){
        return postRepository.findByUserId(userid);
    }

    

    // Comment by CommentId
    public Comment commentById(int commentId, Post post){
        for (Comment comment : post.getPostComments()) {
            if (comment.getCommentId() == commentId) {
                return comment;
            }
        }
        return null;
    }

    // Reply Comment by reply
    public CommentReply replyById(int replyid, Comment comment){
        for (CommentReply commentReply : comment.getCommentReplies()) {
            if (commentReply.getReplyId() == replyid) {
                return commentReply;
            }
        }
        return null;
    }

}
