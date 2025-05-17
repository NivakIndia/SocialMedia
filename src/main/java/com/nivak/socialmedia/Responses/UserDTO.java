package com.nivak.socialmedia.Responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
        
    private String id;
    private String userId;
    private String userName;
    private String fullName;
    private String profileURL;
    
}