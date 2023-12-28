package com.nivak.socialmedia.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Notification {
    private int notificationId;
    private String notification;
    private boolean seen;
}
