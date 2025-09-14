package com.chess.chessverse.controller;

import com.chess.chessverse.model.Notification;
import com.chess.chessverse.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }
}
