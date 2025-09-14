package com.chess.chessverse.controller;

import com.chess.chessverse.model.ForumPost;
import com.chess.chessverse.repository.ForumPostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forum")
public class ForumPostController {
    @Autowired
    private ForumPostRepository forumPostRepository;

    @GetMapping
    public List<ForumPost> getAllPosts() {
        return forumPostRepository.findAll();
    }
}
