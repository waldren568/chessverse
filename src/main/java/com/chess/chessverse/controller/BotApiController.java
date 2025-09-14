package com.chess.chessverse.controller;

import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bot")
public class BotApiController {
    
    @GetMapping("/stats")
    public Map<String, Object> getBotStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Statistiche fittizie per ora - da implementare con database
        stats.put("totalGames", 42);
        stats.put("winRate", 65);
        stats.put("favoriteBot", "Stockfish Level 1");
        stats.put("avgMoves", 28);
        
        // Statistiche per bot specifici
        Map<String, Object> botStats = new HashMap<>();
        
        Map<String, Object> stockfishStats = new HashMap<>();
        stockfishStats.put("gamesPlayed", 25);
        stockfishStats.put("winRate", 40);
        stockfishStats.put("avgMoves", 35);
        stockfishStats.put("lastPlayed", "2025-09-06");
        
        Map<String, Object> randomStats = new HashMap<>();
        randomStats.put("gamesPlayed", 17);
        randomStats.put("winRate", 85);
        randomStats.put("avgMoves", 22);
        randomStats.put("lastPlayed", "2025-09-06");
        
        botStats.put("stockfish-1", stockfishStats);
        botStats.put("random", randomStats);
        
        stats.put("botDetails", botStats);
        
        return stats;
    }
    
    @PostMapping("/select")
    public Map<String, Object> selectBot(@RequestBody Map<String, String> request) {
        String botType = request.get("botType");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("selectedBot", botType);
        response.put("message", "Bot selezionato con successo!");
        
        // Qui potresti salvare la preferenza nel database
        
        return response;
    }
    
    @GetMapping("/available")
    public Map<String, Object> getAvailableBots() {
        Map<String, Object> bots = new HashMap<>();
        
        // Bot attivi
        Map<String, Object> stockfish = new HashMap<>();
        stockfish.put("name", "Stockfish Level 1");
        stockfish.put("elo", 800);
        stockfish.put("difficulty", "Principiante");
        stockfish.put("style", "Tattico");
        stockfish.put("responseTime", "~2 secondi");
        stockfish.put("available", true);
        stockfish.put("icon", "🐟");
        
        Map<String, Object> random = new HashMap<>();
        random.put("name", "Random Bot");
        random.put("elo", 400);
        random.put("difficulty", "Molto Facile");
        random.put("style", "Imprevedibile");
        random.put("responseTime", "Istantaneo");
        random.put("available", true);
        random.put("icon", "🎲");
        
        bots.put("stockfish-1", stockfish);
        bots.put("random", random);
        
        // Bot futuri
        Map<String, Object> leela = new HashMap<>();
        leela.put("name", "Leela Chess Zero");
        leela.put("elo", 2000);
        leela.put("difficulty", "Avanzato");
        leela.put("style", "AI Neurale");
        leela.put("responseTime", "~5 secondi");
        leela.put("available", false);
        leela.put("icon", "🧠");
        
        bots.put("leela", leela);
        
        return bots;
    }
}
