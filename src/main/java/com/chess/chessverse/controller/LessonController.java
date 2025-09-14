package com.chess.chessverse.controller;

import com.chess.chessverse.model.Lesson;
import com.chess.chessverse.repository.LessonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {
    @Autowired
    private LessonRepository lessonRepository;

    @GetMapping
    public List<Lesson> getAllLessons() {
        return lessonRepository.findAll();
    }
}
