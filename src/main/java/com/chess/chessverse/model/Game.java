package com.chess.chessverse.model;

import jakarta.persistence.*;

@Entity
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User whitePlayer;
    @ManyToOne
    private User blackPlayer;
    private String fen; // Stato della scacchiera in FEN
    private String status; // ongoing, finished, draw, ecc.

    // Getters e setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    // ...existing code...
}
