package com.chess.chessverse.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String home() {
        return "index";
    }
    
    @GetMapping("/play")
    public String play() {
        return "play";
    }

    @GetMapping("/analysis")
    public String analysis() {
        return "analysis";
    }

    @GetMapping("/analysis/board")
    public String analysisBoard() {
    return "analysis_board";
    }

    @GetMapping("/analysis/review")
    public String analysisReview() {
        return "analysis_review";
    }

    @GetMapping("/settings")
    public String settings() {
        return "settings";
    }
}
