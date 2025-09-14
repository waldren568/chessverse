package com.chess.chessverse.model;

import jakarta.persistence.*;

@Entity
public class ForumPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String author;
    private String content;
    private Long timestamp;
    // ...existing code...
}
