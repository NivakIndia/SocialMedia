package com.nivak.socialmedia.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Notification {
    private int notificationId;
    private int postid;
    private String userId;
    private String notificationMessage;
    private boolean seen;
    private String notificationDate;
    private String notificationTime;
}