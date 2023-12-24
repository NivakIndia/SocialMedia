package com.nivak.socialmedia.User;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "Users")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private ObjectId id;
    private String userName;
    private String fullName;
    private String code;
    private String userId;
    private String password;
    private int verificationToken;
    private int forgetPassToken;
    private boolean accountIsVerified;
    private String profileURL;
    private List<String> userFollowings;
    private List<String> userFollowers;
    private String userBio;
    private String gender;
}
