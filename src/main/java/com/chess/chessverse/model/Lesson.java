package com.chess.chessverse.model;

import jakarta.persistence.*;

@Entity
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    // ...existing code...
}
