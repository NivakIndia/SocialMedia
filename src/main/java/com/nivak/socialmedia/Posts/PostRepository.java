package com.nivak.socialmedia.Posts;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository("posts")
public interface PostRepository extends MongoRepository<Post,ObjectId> {
    
}
