
package com.chess.chessverse.service;

import org.springframework.stereotype.Service;
import java.io.*;
import java.util.concurrent.*;

@Service("stockfishSimple")
public class StockfishSimpleService {

    /**
     * Ottiene la valutazione (score) per una posizione FEN usando Stockfish 17 (max skill, max tempo ragionevole)
     * Restituisce una stringa tipo "+1.23" (centipawn) o "#-3" (mate in 3 per il nero)
     * Esegue solo la valutazione, senza handshake UCI (che avviene solo in initialize)
     */
    public String getEvaluation(String fen) {
        if (!isInitialized || stockfishProcess == null || !stockfishProcess.isAlive()) {
            return null;
        }
        try {
            processWriter.println("setoption name Skill Level value 20");
            processWriter.println("position fen " + fen);
            int moveTime = isOpeningPosition(fen) ? 2000 : 10000;
            processWriter.println("go movetime " + moveTime);
            processWriter.flush();

            final BufferedReader reader = processReader;
            final String[] lastScore = {null};
            final boolean[] gotBestmove = {false};
            final StringBuilder debugLog = new StringBuilder();

            Thread t = new Thread(() -> {
                try {
                    String l;
                    while ((l = reader.readLine()) != null) {
                        debugLog.append(l).append("\n");
                        if (l.contains("score ")) {
                            int idx = l.indexOf("score ");
                            String sub = l.substring(idx + 6);
                            if (sub.startsWith("cp ")) {
                                String[] parts = sub.split(" ");
                                if (parts.length >= 2) {
                                    try {
                                        int cp = Integer.parseInt(parts[1]);
                                        double pawns = cp / 100.0;
                                        lastScore[0] = String.format("%+.2f", pawns);
                                    } catch (Exception ignore) {}
                                }
                            } else if (sub.startsWith("mate ")) {
                                String[] parts = sub.split(" ");
                                if (parts.length >= 2) {
                                    try {
                                        int mate = Integer.parseInt(parts[1]);
                                        lastScore[0] = (mate > 0 ? "#" : "#-") + Math.abs(mate);
                                    } catch (Exception ignore) {}
                                }
                            }
                        }
                        if (l.startsWith("bestmove ")) {
                            gotBestmove[0] = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    debugLog.append("[EXC] ").append(e.getMessage()).append("\n");
                }
            });
            t.start();
            t.join(moveTime + 3000); // Attendi massimo tempo + margine
            if (t.isAlive()) t.interrupt();
            System.out.println("[StockfishSimpleService] Eval debug log:\n" + debugLog);
            return lastScore[0];
        } catch (Exception e) {
            System.out.println("❌ Errore durante valutazione posizione: " + e.getMessage());
            return null;
        }
    }
    
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

            // Handshake UCI SOLO UNA VOLTA
            processWriter.println("uci");
            processWriter.flush();
            String line;
            boolean gotUciOk = false;
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 5000) {
                if (processReader.ready()) {
                    line = processReader.readLine();
                    System.out.println("[StockfishSimpleService][UCI] " + line);
                    if (line != null && line.trim().equals("uciok")) {
                        gotUciOk = true;
                        break;
                    }
                }
            }
            if (!gotUciOk) {
                System.out.println("[StockfishSimpleService] UCI handshake fallito");
                return;
            }

            // Invia 'isready' e attendi 'readyok'
            processWriter.println("isready");
            processWriter.flush();
            boolean gotReadyOk = false;
            start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 5000) {
                if (processReader.ready()) {
                    line = processReader.readLine();
                    System.out.println("[StockfishSimpleService][READY] " + line);
                    if (line != null && line.trim().equals("readyok")) {
                        gotReadyOk = true;
                        break;
                    }
                }
            }
            if (!gotReadyOk) {
                System.out.println("[StockfishSimpleService] isready handshake fallito");
                return;
            }

            isInitialized = true;
            System.out.println("✅ Stockfish inizializzato correttamente!");
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
