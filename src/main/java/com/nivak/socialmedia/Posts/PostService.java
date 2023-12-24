package com.nivak.socialmedia.Posts;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class PostService {
    
    @Autowired 
    private PostRepository postRepository;

    @Autowired
    private void myPost(@Qualifier("posts") PostRepository postRepository){
        this.postRepository = postRepository;
    }
    
    // All post
    public List<Post> allPost(){
        return postRepository.findAll();
    }

    // Post by userID
    public List<Post> postByUserId(String userid){
        return postRepository.findByUserId(userid);
    }

    // Post by postId
    public Post postByPostId(int id){
        return postRepository.findByPostId(id);
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
