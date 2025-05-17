package com.nivak.socialmedia.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Notification {
    private int id;
    private String postId;
    private String userId;
    private int commentId;
    private String notificationMessage;
    private boolean isSeen;
    private String notificationDate;
    private String notificationTime;
}