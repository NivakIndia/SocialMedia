package com.nivak.socialmedia.User;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;





@Repository("users")
public interface UserRepository extends MongoRepository<User,ObjectId>{
    User findByUserId(String userId);
    User findByUserName(String userName);
}
