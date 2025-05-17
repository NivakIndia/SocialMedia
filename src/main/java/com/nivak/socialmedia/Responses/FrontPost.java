package com.nivak.socialmedia.Responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FrontPost {
    private String id;
    private String userId;
    private String postDate;
    private String postTime;
}
