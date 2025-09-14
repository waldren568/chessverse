package com.chess.chessverse.model;

import jakarta.persistence.*;

@Entity
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String message;
    private boolean read;
    // ...existing code...
}
