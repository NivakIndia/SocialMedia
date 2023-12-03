package com.nivak.socialmedia.Posts;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "Posts")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Post {
    private ObjectId id;
    private int postId;
    private String userId;
    private String postPath;
    private String postDescription;
    private String postDate;
    private String postTime;
    private List<String> postLikes;
    private List<Comment> postComments;
}
