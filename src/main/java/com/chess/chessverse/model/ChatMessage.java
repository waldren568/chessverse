package com.chess.chessverse.model;

import jakarta.persistence.*;

@Entity
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String sender;
    private String content;
    private Long timestamp;
    // ...existing code...
}
