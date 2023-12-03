package com.nivak.socialmedia.Posts;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentReply {
    private String replyEmail;
    private String replyContent;
    private String replyDate;
    private String replyTime;
    private List<String> replyLikes;
}
