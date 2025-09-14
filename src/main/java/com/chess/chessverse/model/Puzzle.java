package com.chess.chessverse.model;

import jakarta.persistence.*;

@Entity
public class Puzzle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fen;
    private String solution;
    private String difficulty;
    // ...existing code...
}
