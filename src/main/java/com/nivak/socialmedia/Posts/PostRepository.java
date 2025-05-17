package com.nivak.socialmedia.Posts;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository("posts")
public interface PostRepository extends MongoRepository<Post,ObjectId> {
    List<Post> findByUserId(String userId);
    @Query("SELECT p FROM Post p WHERE p.user.id != :userId AND p.user.id NOT IN :excludedUserIds")
    Page<Post> findAllExcludingUserIds(
        @Param("userId") String userId, 
        @Param("excludedUserIds") List<String> excludedUserIds, 
        Pageable pageable);

}
