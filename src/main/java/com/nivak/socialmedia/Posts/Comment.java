package com.nivak.socialmedia.Posts;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Comment {
    private int commentId;
    private String commenterUserId;
    private String commentMessage;
    private String commentDate;
    private String commentTime;
    private List<String> commentLikes;
    private List<CommentReply> commentReplies; 
}
