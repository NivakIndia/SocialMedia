package com.nivak.socialmedia.Posts;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


@Repository("posts")
public interface PostRepository extends MongoRepository<Post,ObjectId> {
    List<Post> findByUserId(String userId);
    Post findByPostId(int postId);
}
