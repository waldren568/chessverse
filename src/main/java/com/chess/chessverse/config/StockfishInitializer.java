package com.chess.chessverse.config;

import com.chess.chessverse.service.StockfishSimpleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StockfishInitializer implements CommandLineRunner {
    
    @Autowired
    @Qualifier("stockfishSimple")
    private StockfishSimpleService stockfishSimpleService;

    @Override
    public void run(String... args) throws Exception {
        // Inizializza Stockfish all'avvio dell'applicazione
        stockfishSimpleService.initialize();
    }
}
