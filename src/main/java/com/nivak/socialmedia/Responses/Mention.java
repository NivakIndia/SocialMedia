package com.nivak.socialmedia.Responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Mention {
    private String id;
    private String userName;
    private String fullName;
    private String profileURL;
}
