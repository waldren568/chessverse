package com.chess.chessverse.controller;

import com.chess.chessverse.model.Move;
import com.chess.chessverse.repository.MoveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/moves")
public class MoveController {
    @Autowired
    private MoveRepository moveRepository;

    @GetMapping
    public List<Move> getAllMoves() {
        return moveRepository.findAll();
    }
}
