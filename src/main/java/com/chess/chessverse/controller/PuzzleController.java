package com.chess.chessverse.controller;

import com.chess.chessverse.model.Puzzle;
import com.chess.chessverse.repository.PuzzleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/puzzles")
public class PuzzleController {
    @Autowired
    private PuzzleRepository puzzleRepository;

    @GetMapping
    public List<Puzzle> getAllPuzzles() {
        return puzzleRepository.findAll();
    }
}
