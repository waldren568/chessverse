package com.chess.chessverse.repository;

import com.chess.chessverse.model.ForumPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {
}
