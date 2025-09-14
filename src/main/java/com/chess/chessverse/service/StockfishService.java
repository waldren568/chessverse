package com.chess.chessverse.service;

import org.springframework.stereotype.Service;
import java.io.*;
import java.util.concurrent.TimeUnit;

@Service
public class StockfishService {
    
    private Process stockfishProcess;
    private BufferedReader processReader;
    private OutputStreamWriter processWriter;
    private boolean isInitialized = false;
    
    /**
     * Inizializza Stockfish se disponibile nel sistema
     */
    public void initialize() {
        try {
            System.out.println("🚀 Inizializzazione Stockfish...");
            
            // Debug: mostra la directory corrente
            String currentDir = System.getProperty("user.dir");
            System.out.println("📁 Directory corrente: " + currentDir);
            
            // Prova diversi percorsi comuni per Stockfish
            String[] possiblePaths = {
                System.getProperty("user.dir") + "\\stockfish.exe",  // Percorso assoluto Windows
                System.getProperty("user.dir") + "/stockfish.exe",   // Percorso assoluto Unix
                ".\\stockfish.exe",         // Windows directory corrente
                "./stockfish.exe",          // Unix-style directory corrente
                "stockfish.exe",            // Windows nella PATH
                "stockfish",                // Unix nel PATH
                "/usr/local/bin/stockfish", // Linux/Mac standard
                "/usr/bin/stockfish"        // Linux alternativo
            };
            
            System.out.println("🔍 Percorsi da testare:");
            for (int i = 0; i < possiblePaths.length; i++) {
                System.out.println("  " + (i+1) + ". " + possiblePaths[i]);
                File file = new File(possiblePaths[i]);
                System.out.println("     Esiste: " + file.exists() + ", Leggibile: " + file.canRead() + ", Eseguibile: " + file.canExecute());
            }
            
            for (String path : possiblePaths) {
                try {
                    System.out.println("🔄 Tentativo con: " + path);
                    
                    // Verifica e imposta i permessi di esecuzione se necessario
                    File stockfishFile = new File(path);
                    if (stockfishFile.exists() && !stockfishFile.canExecute()) {
                        System.out.println("📋 Impostazione permessi di esecuzione...");
                        stockfishFile.setExecutable(true);
                    }
                    
                    // Test semplice: prova solo ad avviare il processo
                    ProcessBuilder pb = new ProcessBuilder(path);
                    pb.redirectErrorStream(true);
                    stockfishProcess = pb.start();
                    
                    // Aspetta un attimo che il processo si avvii
                    Thread.sleep(100);
                    
                    // Verifica se il processo è ancora vivo
                    if (!stockfishProcess.isAlive()) {
                        System.out.println("     ❌ Il processo Stockfish è terminato immediatamente");
                        continue;
                    }
                    
                    processReader = new BufferedReader(new InputStreamReader(stockfishProcess.getInputStream()));
                    processWriter = new OutputStreamWriter(stockfishProcess.getOutputStream());
                    
                    // Test inizializzazione UCI più semplice
                    System.out.println("     📤 Invio comando UCI...");
                    processWriter.write("uci\n");
                    processWriter.flush();
                    
                    // Leggi per 2 secondi per vedere cosa arriva
                    boolean foundUciOk = false;
                    long startTime = System.currentTimeMillis();
                    StringBuilder allOutput = new StringBuilder();
                    
                    while (System.currentTimeMillis() - startTime < 2000) {
                        if (processReader.ready()) {
                            String line = processReader.readLine();
                            if (line != null) {
                                allOutput.append(line).append("\n");
                                System.out.println("     � << " + line);
                                if (line.trim().equals("uciok")) {
                                    foundUciOk = true;
                                    break;
                                }
                            }
                        } else {
                            Thread.sleep(50);
                        }
                    }
                    
                    if (foundUciOk) {
                        System.out.println("     ✅ UCI OK ricevuto!");
                        
                        // Configura per principianti
                        processWriter.write("setoption name Skill Level value 0\n");
                        processWriter.flush();
                        processWriter.write("setoption name UCI_LimitStrength value true\n");
                        processWriter.flush();
                        processWriter.write("setoption name UCI_Elo value 800\n");
                        processWriter.flush();
                        processWriter.write("isready\n");
                        processWriter.flush();
                        
                        // Aspetta readyok
                        startTime = System.currentTimeMillis();
                        while (System.currentTimeMillis() - startTime < 1000) {
                            if (processReader.ready()) {
                                String line = processReader.readLine();
                                if (line != null && line.trim().equals("readyok")) {
                                    break;
                                }
                            } else {
                                Thread.sleep(50);
                            }
                        }
                        
                        isInitialized = true;
                        System.out.println("✅ Stockfish inizializzato correttamente con livello principiante");
                        return;
                    } else {
                        System.out.println("     ❌ Non ricevuto UCI OK. Output totale:");
                        System.out.println(allOutput.toString());
                    }
                } catch (Exception e) {
                    System.out.println("     ❌ Errore con " + path + ": " + e.getMessage());
                    // Chiudi il processo se aperto
                    if (stockfishProcess != null && stockfishProcess.isAlive()) {
                        stockfishProcess.destroy();
                    }
                    continue;
                }
            }
            
            System.out.println("⚠️ Stockfish non trovato. Usando bot random come fallback.");
            System.out.println("💡 Stockfish non disponibile - usando bot casuale come fallback");
            System.out.println("📝 Per usare Stockfish, installa il motore e assicurati che sia nel PATH");
            
        } catch (Exception e) {
            System.out.println("⚠️ Errore inizializzazione Stockfish: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Ottiene la migliore mossa da Stockfish per la posizione data
     */
    public String getBestMove(String fen) {
        if (!isInitialized) {
            return null; // Usa fallback
        }
        
        try {
            // Imposta la posizione
            sendCommand("position fen " + fen);
            
            // Cerca per tempo limitato (livello principiante)
            sendCommand("go movetime 500"); // Solo 500ms di calcolo
            
            String response = readUntil("bestmove", 2000);
            
            if (response.contains("bestmove")) {
                String[] parts = response.split("\\s+");
                for (int i = 0; i < parts.length - 1; i++) {
                    if ("bestmove".equals(parts[i]) && parts.length > i + 1) {
                        String move = parts[i + 1];
                        if (move.length() >= 4 && !move.equals("(none)")) {
                            return move; // Formato: e2e4, g1f3, etc.
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println("⚠️ Errore ottenendo mossa da Stockfish: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Converte mossa UCI (e2e4) in formato algebrico (from: e2, to: e4)
     */
    public String[] parseUCIMove(String uciMove) {
        if (uciMove == null || uciMove.length() < 4) {
            return null;
        }
        
        String from = uciMove.substring(0, 2);
        String to = uciMove.substring(2, 4);
        String promotion = null;
        
        // Gestisci promozione
        if (uciMove.length() == 5) {
            promotion = uciMove.substring(4, 5);
        }
        
        return new String[]{from, to, promotion};
    }
    
    /**
     * Verifica se Stockfish è disponibile e inizializzato
     */
    public boolean isAvailable() {
        return isInitialized;
    }
    
    /**
     * Ottiene informazioni su Stockfish
     */
    public String getEngineInfo() {
        if (!isInitialized) {
            return "Stockfish non disponibile - usando bot random";
        }
        return "Stockfish Level 1 (ELO ~800) - Modalità Principiante";
    }
    
    // ========== METODI PRIVATI ==========
    
    private void sendCommand(String command) throws IOException {
        processWriter.write(command + "\n");
        processWriter.flush();
    }
    
    private String readUntil(String expectedResponse, int timeoutMs) throws IOException {
        StringBuilder response = new StringBuilder();
        long startTime = System.currentTimeMillis();
        
        System.out.println("     🕐 Attendo risposta con '" + expectedResponse + "' per " + timeoutMs + "ms...");
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (processReader.ready()) {
                String line = processReader.readLine();
                if (line != null) {
                    System.out.println("     📋 Linea ricevuta: [" + line + "]");
                    response.append(line).append("\n");
                    if (line.contains(expectedResponse)) {
                        System.out.println("     ✅ Trovata risposta attesa!");
                        break;
                    }
                }
            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("     ⏱️ Tempo trascorso: " + elapsed + "ms");
        
        return response.toString();
    }
    
    /**
     * Chiude Stockfish quando l'applicazione si spegne
     */
    public void shutdown() {
        try {
            if (stockfishProcess != null) {
                sendCommand("quit");
                stockfishProcess.destroy();
                if (!stockfishProcess.waitFor(2, TimeUnit.SECONDS)) {
                    stockfishProcess.destroyForcibly();
                }
            }
        } catch (Exception e) {
            System.out.println("Errore chiusura Stockfish: " + e.getMessage());
        }
    }
}
