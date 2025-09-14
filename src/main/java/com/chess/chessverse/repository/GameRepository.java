package com.chess.chessverse.repository;

import com.chess.chessverse.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {
}
