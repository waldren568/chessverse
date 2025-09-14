package com.chess.chessverse.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/bot")
public class BotController {
    
    @GetMapping
    public String botPage(Model model) {
        model.addAttribute("title", "ChessVerse - Bot Selection");
        return "bot";
    }
}
