package com.chess.chessverse.controller;

import com.chess.chessverse.model.ChatMessage;
import com.chess.chessverse.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatMessageController {
    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @GetMapping
    public List<ChatMessage> getAllMessages() {
        return chatMessageRepository.findAll();
    }
}
