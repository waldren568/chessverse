package com.chess.chessverse.config;

import com.chess.chessverse.service.StockfishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StockfishConfig {
    
    @Autowired
    private StockfishService stockfishService;
    
    /**
     * Inizializza Stockfish all'avvio dell'applicazione
     */
    @Bean
    public ApplicationRunner initializeStockfish() {
        return args -> {
            System.out.println("🚀 Inizializzazione Stockfish...");
            stockfishService.initialize();
            
            if (stockfishService.isAvailable()) {
                System.out.println("✅ Stockfish Level 1 pronto!");
                System.out.println("ℹ️  " + stockfishService.getEngineInfo());
            } else {
                System.out.println("⚠️  Stockfish non disponibile - usando bot casuale come fallback");
                System.out.println("💡 Per usare Stockfish, installa il motore e assicurati che sia nel PATH");
            }
            
            // Aggiungi shutdown hook per chiudere Stockfish pulitamente
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("🔄 Chiusura Stockfish...");
                stockfishService.shutdown();
            }));
        };
    }
}
