package com.chess.chessverse.model;

import jakarta.persistence.*;

@Entity
public class Move {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fromSquare;
    private String toSquare;
    private String piece;
    private String fenAfter;
    // ...existing code...
}
