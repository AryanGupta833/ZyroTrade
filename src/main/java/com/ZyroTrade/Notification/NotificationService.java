package com.ZyroTrade.Notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;

    public void notify(String username,String title,String message){
        Notification n=new Notification();
        n.setUsername(username);
        n.setTitle(title);
        n.setMessage(message);

        notificationRepository.save(n);

    }
}
