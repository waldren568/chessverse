package com.chess.chessverse.repository;

import com.chess.chessverse.model.Move;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoveRepository extends JpaRepository<Move, Long> {
}
