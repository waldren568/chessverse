package com.chess.chessverse.service;

import org.springframework.stereotype.Service;
import java.io.*;
import java.util.concurrent.*;

@Service("stockfishSimple")
public class StockfishSimpleService {
    
    private Process stockfishProcess;
    private PrintWriter processWriter;
    private BufferedReader processReader;
    private boolean isInitialized = false;
    
    public void initialize() {
        System.out.println("🚀 Inizializzazione Stockfish Simple...");
        
        String stockfishPath = System.getProperty("user.dir") + File.separator + "stockfish.exe";
        System.out.println("📁 Percorso Stockfish: " + stockfishPath);
        
        try {
            // Test se il file esiste
            File stockfishFile = new File(stockfishPath);
            if (!stockfishFile.exists()) {
                System.out.println("❌ File Stockfish non trovato: " + stockfishPath);
                return;
            }
            
            System.out.println("✅ File trovato: " + stockfishFile.length() + " bytes");
            
            // Avvia il processo
            ProcessBuilder pb = new ProcessBuilder(stockfishPath);
            pb.directory(new File(System.getProperty("user.dir")));
            stockfishProcess = pb.start();
            
            // Aspetta che il processo si avvii
            Thread.sleep(200);
            
            if (!stockfishProcess.isAlive()) {
                System.out.println("❌ Il processo Stockfish è terminato");
                return;
            }
            
            System.out.println("✅ Processo Stockfish avviato con PID: " + stockfishProcess.pid());
            
            // Configura i stream
            processWriter = new PrintWriter(stockfishProcess.getOutputStream(), true);
            processReader = new BufferedReader(new InputStreamReader(stockfishProcess.getInputStream()));
            
            // TEST SUPER AGGRESSIVO STOCKFISH
            System.out.println("🔬 DIAGNOSI COMPLETA STOCKFISH:");
            
            // Test 0: Verifica processo
            System.out.println("🔍 Processo vivo: " + stockfishProcess.isAlive());
            System.out.println("🔍 PID: " + stockfishProcess.pid());
            
            // Test 1: Prima verifica bytes disponibili
            Thread.sleep(100);
            int available = stockfishProcess.getInputStream().available();
            System.out.println("📊 Bytes immediatamente disponibili: " + available);
            
            // Test 2: Invio comando semplice
            System.out.println("📤 Invio 'isready'...");
            processWriter.println("isready");
            processWriter.flush();
            
            Thread.sleep(200);
            available = stockfishProcess.getInputStream().available();
            System.out.println("📊 Bytes dopo isready: " + available);
            
            // Test 3: UCI con debug intensivo
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<Boolean> future = executor.submit(() -> {
                try {
                    System.out.println("📤 Invio comando UCI...");
                    processWriter.println("uci");
                    processWriter.flush();
                    
                    System.out.println("🔄 Inizio lettura risposta...");
                    String line;
                    int lineCount = 0;
                    long startTime = System.currentTimeMillis();
                    
                    while ((line = processReader.readLine()) != null) {
                        lineCount++;
                        long elapsed = System.currentTimeMillis() - startTime;
                        System.out.println("📋 [" + elapsed + "ms] Linea " + lineCount + ": [" + line + "]");
                        
                        if (line.trim().equals("uciok")) {
                            System.out.println("✅✅✅ TROVATO UCIOK! ✅✅✅");
                            return true;
                        }
                        
                        // Sicurezza: max 50 linee
                        if (lineCount > 50) {
                            System.out.println("⚠️ Limite linee raggiunto");
                            break;
                        }
                    }
                    
                    System.out.println("❌ Fine stream senza uciok (linee lette: " + lineCount + ")");
                    return false;
                } catch (Exception e) {
                    System.out.println("💥 ERRORE CRITICO: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            });
            
            try {
                // Aspetta massimo 5 secondi per uciok
                boolean success = future.get(5, TimeUnit.SECONDS);
                
                if (success) {
                    // Configura Stockfish per livello principiante
                    processWriter.println("setoption name Skill Level value 0");
                    processWriter.println("setoption name UCI_LimitStrength value true");
                    processWriter.println("setoption name UCI_Elo value 800");
                    processWriter.println("isready");
                    
                    // Aspetta readyok
                    Future<Boolean> readyFuture = executor.submit(() -> {
                        try {
                            String line;
                            while ((line = processReader.readLine()) != null) {
                                System.out.println("📋 << " + line);
                                if (line.trim().equals("readyok")) {
                                    return true;
                                }
                            }
                            return false;
                        } catch (Exception e) {
                            return false;
                        }
                    });
                    
                    readyFuture.get(2, TimeUnit.SECONDS);
                    
                    isInitialized = true;
                    System.out.println("✅ Stockfish inizializzato correttamente!");
                } else {
                    System.out.println("❌ Non ricevuto uciok da Stockfish");
                }
                
            } catch (TimeoutException e) {
                System.out.println("❌ Timeout durante inizializzazione Stockfish");
                future.cancel(true);
            }
            
            executor.shutdown();
            
        } catch (Exception e) {
            System.out.println("❌ Errore inizializzazione Stockfish: " + e.getMessage());
            e.printStackTrace();
        }
        
        if (!isInitialized) {
            System.out.println("💡 Stockfish non disponibile - usando bot casuale come fallback");
        }
    }
    
    public String getBestMove(String fen) {
        return getBestMoveWithSkillLevel(fen, 1); // Default a livello 1
    }
    
    public String getBestMoveWithSkillLevel(String fen, int skillLevel) {
        if (!isInitialized || stockfishProcess == null || !stockfishProcess.isAlive()) {
            return null;
        }
        
        // Clamp del livello tra 1 e 17
        skillLevel = Math.max(1, Math.min(17, skillLevel));
        
        try {
            // � OPENING DETECTION: Velocizza le prime mosse
            boolean isOpening = isOpeningPosition(fen);
            
            // �🔥 SISTEMA RIVOLUZIONARIO: Livelli alti = Stockfish UNLEASHED
            String searchCommand;
            int timeoutSeconds;
            
            if (skillLevel <= 5) {
                // Livelli 1-5: Skill Level limitato (principianti)
                processWriter.println("setoption name Skill Level value " + Math.min(skillLevel * 2, 10));
                if (isOpening) {
                    searchCommand = "go depth " + (skillLevel + 1); // Più veloce negli opening
                    timeoutSeconds = 1;
                } else {
                    searchCommand = "go depth " + (skillLevel + 2);
                    timeoutSeconds = 3;
                }
                System.out.println("🎯 Stockfish Level " + skillLevel + " (Limitato) - Skill=" + (skillLevel * 2) + (isOpening ? " OPENING" : ""));
                
            } else if (skillLevel <= 10) {
                // Livelli 6-10: Skill Level medio con più tempo
                processWriter.println("setoption name Skill Level value " + Math.min(skillLevel + 5, 15));
                int baseTime = isOpening ? 500 : (1000 + (skillLevel - 5) * 500);
                searchCommand = "go movetime " + baseTime;
                timeoutSeconds = isOpening ? 2 : 5;
                System.out.println("🚀 Stockfish Level " + skillLevel + " (Intermedio) - Skill=" + (skillLevel + 5) + ", Time=" + baseTime + "ms" + (isOpening ? " OPENING" : ""));
                
            } else if (skillLevel <= 14) {
                // Livelli 11-14: Skill Level alto + tempo lungo
                processWriter.println("setoption name Skill Level value " + Math.min(skillLevel + 5, 20));
                int baseTime = isOpening ? 1000 : (3000 + (skillLevel - 10) * 1000);
                searchCommand = "go movetime " + baseTime;
                timeoutSeconds = isOpening ? 3 : 8;
                System.out.println("⚡ Stockfish Level " + skillLevel + " (Avanzato) - Skill=" + (skillLevel + 5) + ", Time=" + baseTime + "ms" + (isOpening ? " OPENING" : ""));
                
            } else {
                // Livelli 15-17: STOCKFISH SCATENATO! 🔥
                processWriter.println("setoption name Skill Level value 20"); // MASSIMO SKILL
                
                if (skillLevel == 15) {
                    int moveTime = isOpening ? 2000 : 8000;
                    searchCommand = "go movetime " + moveTime;
                    System.out.println("🔥 Stockfish Level 15 (SUPER MAESTRO) - SKILL MASSIMO + " + moveTime/1000 + "s" + (isOpening ? " OPENING" : ""));
                } else if (skillLevel == 16) {
                    int depth = isOpening ? 12 : 18;
                    searchCommand = "go depth " + depth;
                    System.out.println("💀 Stockfish Level 16 (KILLER) - SKILL MASSIMO + DEPTH " + depth + (isOpening ? " OPENING" : ""));
                } else { // Level 17
                    int moveTime = isOpening ? 3000 : 15000;
                    searchCommand = "go movetime " + moveTime;
                    System.out.println("👑 Stockfish Level 17 (MONDIALE) - SKILL MASSIMO + " + moveTime/1000 + "s" + (isOpening ? " OPENING" : " BEAST MODE"));
                }
                timeoutSeconds = isOpening ? 5 : 20;
            }
            
            // Imposta posizione e calcola
            processWriter.println("position fen " + fen);
            processWriter.println(searchCommand);
            
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<String> future = executor.submit(() -> {
                try {
                    String line;
                    while ((line = processReader.readLine()) != null) {
                        if (line.startsWith("bestmove")) {
                            String[] parts = line.split(" ");
                            if (parts.length >= 2) {
                                return parts[1]; // La mossa è il secondo elemento
                            }
                        }
                    }
                    return null;
                } catch (Exception e) {
                    return null;
                }
            });
            
            String move = future.get(timeoutSeconds, TimeUnit.SECONDS);
            executor.shutdown();
            
            System.out.println("♟️ Stockfish Level " + skillLevel + " mossa: " + move);
            return move;
            
        } catch (Exception e) {
            System.out.println("❌ Errore durante calcolo mossa Level " + skillLevel + ": " + e.getMessage());
            return null;
        }
    }
    
    public boolean isAvailable() {
        return isInitialized && stockfishProcess != null && stockfishProcess.isAlive();
    }
    
    public void shutdown() {
        if (stockfishProcess != null) {
            try {
                if (processWriter != null) {
                    processWriter.println("quit");
                    processWriter.close();
                }
                if (processReader != null) {
                    processReader.close();
                }
                stockfishProcess.waitFor(2, TimeUnit.SECONDS);
                if (stockfishProcess.isAlive()) {
                    stockfishProcess.destroyForcibly();
                }
                System.out.println("✅ Stockfish chiuso correttamente");
            } catch (Exception e) {
                System.out.println("❌ Errore chiusura Stockfish: " + e.getMessage());
            }
        }
    }
    
    // 🚀 METODO PER RILEVARE LE APERTURE
    private boolean isOpeningPosition(String fen) {
        try {
            // Analizza il FEN per determinare se siamo ancora nell'opening
            String[] fenParts = fen.split(" ");
            String position = fenParts[0];
            int halfMoveClock = Integer.parseInt(fenParts[4]);
            int fullMoveNumber = Integer.parseInt(fenParts[5]);
            
            // Consideriamo opening le prime 10 mosse (20 half-moves) 
            // O se pochi pezzi sono stati mossi
            boolean earlyGame = fullMoveNumber <= 10;
            boolean fewMoves = halfMoveClock <= 20;
            
            // Conta i pezzi sviluppati (non nelle posizioni iniziali)
            int developedPieces = countDevelopedPieces(position);
            boolean stillDeveloping = developedPieces <= 6;
            
            return earlyGame || (fewMoves && stillDeveloping);
            
        } catch (Exception e) {
            // In caso di errore, assumiamo che non sia opening
            return false;
        }
    }
    
    private int countDevelopedPieces(String position) {
        // Conta i pezzi che si sono mossi dalle posizioni iniziali
        int developed = 0;
        String[] ranks = position.split("/");
        
        // Controlla la prima fila (neri)
        String blackBackRank = ranks[0];
        if (!blackBackRank.equals("rnbqkbnr")) developed++;
        
        // Controlla la seconda fila (pedoni neri)
        String blackPawnRank = ranks[1]; 
        if (!blackPawnRank.equals("pppppppp")) developed++;
        
        // Controlla la settima fila (pedoni bianchi) 
        String whitePawnRank = ranks[6];
        if (!whitePawnRank.equals("PPPPPPPP")) developed++;
        
        // Controlla l'ottava fila (bianchi)
        String whiteBackRank = ranks[7];
        if (!whiteBackRank.equals("RNBQKBNR")) developed++;
        
        return developed;
    }
}
