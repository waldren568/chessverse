package com.chess.chessverse.repository;

import com.chess.chessverse.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
