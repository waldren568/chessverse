package com.chess.chessverse.repository;

import com.chess.chessverse.model.Puzzle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PuzzleRepository extends JpaRepository<Puzzle, Long> {
}
