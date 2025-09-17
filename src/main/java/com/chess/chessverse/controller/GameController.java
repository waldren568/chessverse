package com.chess.chessverse.controller;

import com.chess.chessverse.model.Game;
import com.chess.chessverse.model.Move;
import com.chess.chessverse.repository.GameRepository;
import com.chess.chessverse.service.ChessGameService;
import com.chess.chessverse.service.ClassicChessLogic;
import com.chess.chessverse.service.StockfishService;
import com.chess.chessverse.service.StockfishSimpleService;
import java.util.Map;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api")
public class GameController {
    
    @Autowired
    private StockfishService stockfishService;
    
    @Autowired
    @Qualifier("stockfishSimple")
    private StockfishSimpleService stockfishSimpleService;
    
    // Classe per rappresentare lo stato del gioco
    private static class GameState {
        boolean isCheck;
        boolean isGameOver;
        String message;
        String winner;
        String reason;
        
        GameState(boolean isCheck, boolean isGameOver, String message) {
            this.isCheck = isCheck;
            this.isGameOver = isGameOver;
            this.message = message;
            this.winner = null;
            this.reason = null;
        }
        
        GameState(boolean isCheck, boolean isGameOver, String message, String winner, String reason) {
            this.isCheck = isCheck;
            this.isGameOver = isGameOver;
            this.message = message;
            this.winner = winner;
            this.reason = reason;
        }
    }
    
    @PostMapping("/game/move")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> move(@RequestBody Map<String, String> payload) {
        String fen = payload.get("fen");
        String from = payload.get("from");
        String to = payload.get("to");
        String promotion = payload.get("promotion");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (isValidMove(fen, from, to, promotion)) {
                String newFen = makeMove(fen, from, to, promotion);
                
                // Controlla lo stato del gioco
                GameState gameState = analyzeGameState(newFen);
                
                response.put("success", true);
                response.put("fen", newFen);
                response.put("status", gameState.message);
                response.put("gameOver", gameState.isGameOver);
                response.put("inCheck", gameState.isCheck);
                
                // ✨ NUOVE INFORMAZIONI: Aggiungi dettagli su check, pin e attaccanti
                response.put("boardAnalysis", getBoardAnalysis(newFen));
                
                if (gameState.isGameOver) {
                    response.put("winner", gameState.winner);
                    response.put("reason", gameState.reason);
                }
            } else {
                response.put("success", false);
                response.put("error", "Mossa non valida");
                return ResponseEntity.status(HttpStatus.OK).body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Errore interno: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    // ✨ NUOVO METODO: Analizza la scacchiera per fornire informazioni visive
    @PostMapping("/game/analyze")
    @ResponseBody
    public Map<String, Object> analyzeBoardState(@RequestBody Map<String, String> payload) {
        String fen = payload.get("fen");
        return getBoardAnalysis(fen);
    }
    
    // Endpoint per controllare se il re è sotto scacco
    @PostMapping("/game/check")
    @ResponseBody
    public Map<String, Object> checkForCheck(@RequestBody Map<String, String> payload) {
        String fen = payload.get("fen");
        Map<String, Object> response = new HashMap<>();
        
        try {
            String[] parts = fen.split(" ");
            boolean isWhiteTurn = parts[1].equals("w");
            
            // Trova il re del giocatore di turno
            String kingSquare = findKingSquare(fen, isWhiteTurn);
            
            // Controlla se ci sono pezzi che attaccano il re
            java.util.List<int[]> attackers = getAttackingPieces(fen, isWhiteTurn);
            boolean inCheck = !attackers.isEmpty();
            
            response.put("inCheck", inCheck);
            response.put("sideInCheck", isWhiteTurn ? "white" : "black");
            response.put("kingSquare", kingSquare);
            
            if (inCheck) {
                java.util.List<String> attackingSquares = new java.util.ArrayList<>();
                for (int[] pos : attackers) {
                    attackingSquares.add(coordinateToAlgebraic(pos[1], pos[0]));
                }
                response.put("attackingSquares", attackingSquares);
            }
            
        } catch (Exception e) {
            response.put("inCheck", false);
            response.put("error", "Errore nel controllo check: " + e.getMessage());
        }
        
        return response;
    }
    
    private Map<String, Object> getBoardAnalysis(String fen) {
        Map<String, Object> analysis = new HashMap<>();
        
        String[] parts = fen.split(" ");
        boolean isWhiteTurn = parts[1].equals("w");
        
        // Trova pezzi che attaccano il re
        java.util.List<int[]> attackers = getAttackingPieces(fen, isWhiteTurn);
        java.util.List<String> attackingSquares = new java.util.ArrayList<>();
        for (int[] pos : attackers) {
            attackingSquares.add(coordinateToAlgebraic(pos[1], pos[0]));
        }
        
        // Trova il re sotto scacco
        String kingSquare = findKingSquare(fen, isWhiteTurn);
        
        // Trova pezzi inchiodati
        java.util.List<String> pinnedSquares = findPinnedPieces(fen, isWhiteTurn);
        
        analysis.put("attackingPieces", attackingSquares);
        analysis.put("kingInCheck", kingSquare != null && !attackers.isEmpty() ? kingSquare : null);
        analysis.put("isDoubleCheck", attackers.size() >= 2);
        analysis.put("pinnedPieces", pinnedSquares);
        
        return analysis;
    }
    
    private String coordinateToAlgebraic(int col, int row) {
        return "" + (char)('a' + col) + (8 - row);
    }
    
    private String findKingSquare(String fen, boolean isWhiteTurn) {
        String[][] board = fenToBoard(fen.split(" ")[0]);
        char kingPiece = isWhiteTurn ? 'K' : 'k';
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board[i][j] != null && board[i][j].charAt(0) == kingPiece) {
                    return coordinateToAlgebraic(j, i);
                }
            }
        }
        return null;
    }
    
    private java.util.List<String> findPinnedPieces(String fen, boolean isWhiteTurn) {
        java.util.List<String> pinnedPieces = new java.util.ArrayList<>();
        String[][] board = fenToBoard(fen.split(" ")[0]);
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                String piece = board[i][j];
                if (piece != null) {
                    boolean isPieceWhite = Character.isUpperCase(piece.charAt(0));
                    if (isPieceWhite == isWhiteTurn) { // È nostro pezzo
                        if (isPiecePinned(fen, i, j, isWhiteTurn)) {
                            pinnedPieces.add(coordinateToAlgebraic(j, i));
                        }
                    }
                }
            }
        }
        return pinnedPieces;
    }

    private boolean isValidMove(String fen, String from, String to, String promotion) {
        try {
            if (from == null || to == null || from.equals(to)) {
                return false;
            }
            
            String[] parts = fen.split(" ");
            String board = parts[0];
            String currentTurn = parts[1]; // "w" per bianco, "b" per nero
            
            // Converti coordinate
            int fromCol = from.charAt(0) - 'a';
            int fromRow = 8 - (from.charAt(1) - '0');
            int toCol = to.charAt(0) - 'a';
            int toRow = 8 - (to.charAt(1) - '0');
            
            // Verifica coordinate valide
            if (fromRow < 0 || fromRow > 7 || fromCol < 0 || fromCol > 7 ||
                toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7) {
                return false;
            }
            
            // Converti FEN in array 2D
            String[][] chessBoard = fenToBoard(board);
            
            // Verifica che ci sia un pezzo nella casella di partenza
            String piece = chessBoard[fromRow][fromCol];
            if (piece == null) {
                return false;
            }
            
            // Verifica che il pezzo appartenga al giocatore di turno
            boolean isPieceWhite = Character.isUpperCase(piece.charAt(0));
            boolean isTurnWhite = currentTurn.equals("w");
            
            if (isPieceWhite != isTurnWhite) {
                return false; // Non puoi muovere pezzi dell'avversario
            }
            
            // Verifica che non catturi il proprio pezzo
            String targetPiece = chessBoard[toRow][toCol];
            if (targetPiece != null) {
                boolean isTargetWhite = Character.isUpperCase(targetPiece.charAt(0));
                if (isPieceWhite == isTargetWhite) {
                    return false; // Non puoi catturare i tuoi pezzi
                }
            }
            
            // Usa ClassicChessLogic per validazione completa con arrocco e en passant
            ClassicChessLogic logic = new ClassicChessLogic();
            
            // Controlla arrocco
            if (piece.toLowerCase().charAt(0) == 'k' && Math.abs(toCol - fromCol) == 2) {
                return logic.canCastle(fen, from, to);
            }
            
            // Controlla en passant
            boolean isPawn = piece.toLowerCase().charAt(0) == 'p';
            if (isPawn && toCol != fromCol && targetPiece == null) {
                return logic.canEnPassant(fen, from, to);
            }
            
            // Validazione normale per altri pezzi
            if (!isValidPieceMove(piece.toLowerCase().charAt(0), fromRow, fromCol, toRow, toCol, chessBoard)) {
                return false;
            }
            
            // ✨ CONTROLLO AVANZATO: Verifica che la mossa sia legale considerando pin e check
            return isMoveLegal(fen, fromRow, fromCol, toRow, toCol, isTurnWhite);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isValidPieceMove(char pieceType, int fromRow, int fromCol, int toRow, int toCol, String[][] board) {
        int rowDiff = Math.abs(toRow - fromRow);
        int colDiff = Math.abs(toCol - fromCol);
        
        switch (pieceType) {
            case 'p': // Pedone
                return isValidPawnMove(fromRow, fromCol, toRow, toCol, board);
            case 'r': // Torre
                return (rowDiff == 0 || colDiff == 0) && isPathClear(fromRow, fromCol, toRow, toCol, board);
            case 'n': // Cavallo
                return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2);
            case 'b': // Alfiere
                return rowDiff == colDiff && isPathClear(fromRow, fromCol, toRow, toCol, board);
            case 'q': // Regina
                return ((rowDiff == 0 || colDiff == 0) || (rowDiff == colDiff)) && 
                       isPathClear(fromRow, fromCol, toRow, toCol, board);
            case 'k': // Re
                return rowDiff <= 1 && colDiff <= 1;
            default:
                return false;
        }
    }
    
    private boolean isValidPawnMove(int fromRow, int fromCol, int toRow, int toCol, String[][] board) {
        String piece = board[fromRow][fromCol];
        boolean isWhite = Character.isUpperCase(piece.charAt(0));
        int direction = isWhite ? -1 : 1; // Bianco va verso l'alto (righe decrescenti), nero verso il basso
        
        int rowDiff = toRow - fromRow;
        int colDiff = Math.abs(toCol - fromCol);
        
        // Movimento in avanti
        if (colDiff == 0) {
            // Mossa semplice
            if (rowDiff == direction && board[toRow][toCol] == null) {
                return true;
            }
            // Mossa doppia dalla posizione iniziale
            if ((isWhite && fromRow == 6) || (!isWhite && fromRow == 1)) {
                return rowDiff == 2 * direction && board[toRow][toCol] == null;
            }
        }
        // Cattura diagonale
        else if (colDiff == 1 && rowDiff == direction && board[toRow][toCol] != null) {
            return true;
        }
        
        return false;
    }
    
    private boolean isPathClear(int fromRow, int fromCol, int toRow, int toCol, String[][] board) {
        int rowStep = Integer.compare(toRow, fromRow);
        int colStep = Integer.compare(toCol, fromCol);
        
        int currentRow = fromRow + rowStep;
        int currentCol = fromCol + colStep;
        
        while (currentRow != toRow || currentCol != toCol) {
            if (board[currentRow][currentCol] != null) {
                return false; // Percorso bloccato
            }
            currentRow += rowStep;
            currentCol += colStep;
        }
        
        return true;
    }
    
    private String makeMove(String fen, String from, String to, String promotion) {
        try {
            // Usa ClassicChessLogic per gestire arrocco, en passant e aggiornare il FEN correttamente
            ClassicChessLogic logic = new ClassicChessLogic();
            return logic.updateFEN(fen, from, to, promotion);
        } catch (Exception e) {
            // Se c'è un errore, ritorna il FEN originale
            return fen;
        }
    }
    
    private String[][] fenToBoard(String fen) {
        String[][] board = new String[8][8];
        String[] rows = fen.split("/");
        
        for (int i = 0; i < 8; i++) {
            String row = rows[i];
            int col = 0;
            for (char c : row.toCharArray()) {
                if (Character.isDigit(c)) {
                    col += c - '0'; // Spazi vuoti
                } else {
                    board[i][col] = String.valueOf(c);
                    col++;
                }
            }
        }
        return board;
    }
    
    private String boardToFen(String[][] board) {
        StringBuilder fen = new StringBuilder();
        
        for (int i = 0; i < 8; i++) {
            int emptyCount = 0;
            for (int j = 0; j < 8; j++) {
                if (board[i][j] == null) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(board[i][j]);
                }
            }
            if (emptyCount > 0) {
                fen.append(emptyCount);
            }
            if (i < 7) fen.append("/");
        }
        
        return fen.toString();
    }
    
    private GameState analyzeGameState(String fen) {
        String[] parts = fen.split(" ");
        String currentTurn = parts[1];
        boolean isWhiteTurn = currentTurn.equals("w");
        
        // Controlla se il re è sotto scacco
        java.util.List<int[]> attackers = getAttackingPieces(fen, isWhiteTurn);
        boolean isInCheck = !attackers.isEmpty();
        boolean isInDoubleCheck = attackers.size() >= 2;
        
        // Se è in scacco, controlla se è scacco matto
        if (isInCheck) {
            boolean isCheckmate = isCheckmate(fen, isWhiteTurn);
            if (isCheckmate) {
                String winner = isWhiteTurn ? "black" : "white";
                GameState state = new GameState(true, true, "Scacco Matto! Vince il " + (isWhiteTurn ? "Nero" : "Bianco"));
                state.winner = winner;
                state.reason = "checkmate";
                return state;
            } else {
                // Fornisci informazioni dettagliate sul tipo di scacco
                String checkMessage = isInDoubleCheck ? 
                    "Doppio Scacco al " + (isWhiteTurn ? "Bianco" : "Nero") + "!" :
                    "Scacco al " + (isWhiteTurn ? "Bianco" : "Nero");
                return new GameState(true, false, checkMessage);
            }
        }
        
        // Controlla stallo
        boolean isStalemate = isStalemate(fen, isWhiteTurn);
        if (isStalemate) {
            GameState state = new GameState(false, true, "Stallo! Pareggio");
            state.winner = null;
            state.reason = "stalemate";
            return state;
        }
        
        // Gioco normale
        String turnMessage = "Turno del " + (isWhiteTurn ? "Bianco" : "Nero");
        return new GameState(false, false, turnMessage);
    }
    
    private boolean isKingInCheck(String fen, boolean isWhiteTurn) {
        return getAttackingPieces(fen, isWhiteTurn).size() > 0;
    }
    
    // Restituisce la lista dei pezzi che stanno attaccando il re
    private java.util.List<int[]> getAttackingPieces(String fen, boolean isWhiteTurn) {
        java.util.List<int[]> attackers = new java.util.ArrayList<>();
        String[][] board = fenToBoard(fen.split(" ")[0]);
        
        // Trova la posizione del re
        int kingRow = -1, kingCol = -1;
        char kingPiece = isWhiteTurn ? 'K' : 'k';
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board[i][j] != null && board[i][j].charAt(0) == kingPiece) {
                    kingRow = i;
                    kingCol = j;
                    break;
                }
            }
        }
        
        if (kingRow == -1) return attackers; // Re non trovato
        
        // Controlla se qualche pezzo nemico può attaccare il re
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                String piece = board[i][j];
                if (piece != null) {
                    boolean isPieceWhite = Character.isUpperCase(piece.charAt(0));
                    if (isPieceWhite != isWhiteTurn) { // Pezzo nemico
                        char pieceType = piece.toLowerCase().charAt(0);
                        if (isValidPieceMove(pieceType, i, j, kingRow, kingCol, board)) {
                            attackers.add(new int[]{i, j}); // Aggiungi attaccante
                        }
                    }
                }
            }
        }
        
        return attackers;
    }
    
    // Controlla se c'è double check (re attaccato da 2+ pezzi)
    private boolean isDoubleCheck(String fen, boolean isWhiteTurn) {
        return getAttackingPieces(fen, isWhiteTurn).size() >= 2;
    }
    
    // Controlla se un pezzo è inchiodato (pinned)
    private boolean isPiecePinned(String fen, int pieceRow, int pieceCol, boolean isWhiteTurn) {
        String[][] board = fenToBoard(fen.split(" ")[0]);
        
        // Trova il re
        int kingRow = -1, kingCol = -1;
        char kingPiece = isWhiteTurn ? 'K' : 'k';
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board[i][j] != null && board[i][j].charAt(0) == kingPiece) {
                    kingRow = i;
                    kingCol = j;
                    break;
                }
            }
        }
        
        if (kingRow == -1) return false;
        
        // Se il pezzo e il re non sono sulla stessa linea/diagonale, non può essere pinned
        boolean sameLine = (pieceRow == kingRow || pieceCol == kingCol);
        boolean sameDiagonal = (Math.abs(pieceRow - kingRow) == Math.abs(pieceCol - kingCol));
        
        if (!sameLine && !sameDiagonal) return false;
        
        // Controlla se c'è un pezzo nemico sulla stessa linea dietro il nostro pezzo
        int rowDir = Integer.compare(pieceRow - kingRow, 0);
        int colDir = Integer.compare(pieceCol - kingCol, 0);
        
        // Vai oltre il pezzo nella stessa direzione
        int checkRow = pieceRow + rowDir;
        int checkCol = pieceCol + colDir;
        
        while (checkRow >= 0 && checkRow < 8 && checkCol >= 0 && checkCol < 8) {
            String piece = board[checkRow][checkCol];
            if (piece != null) {
                boolean isPieceWhite = Character.isUpperCase(piece.charAt(0));
                if (isPieceWhite != isWhiteTurn) { // Pezzo nemico
                    char pieceType = piece.toLowerCase().charAt(0);
                    // Se è una regina, torre (su linee) o alfiere (su diagonali) può fare pin
                    if (pieceType == 'q' || 
                        (sameLine && pieceType == 'r') || 
                        (sameDiagonal && pieceType == 'b')) {
                        return true; // Il pezzo è inchiodato
                    }
                }
                break; // Un pezzo blocca, non c'è pin
            }
            checkRow += rowDir;
            checkCol += colDir;
        }
        
        return false;
    }
    
    // Controlla se una mossa è legale considerando pin e check
    private boolean isMoveLegal(String fen, int fromRow, int fromCol, int toRow, int toCol, boolean isWhiteTurn) {
        String[][] board = fenToBoard(fen.split(" ")[0]);
        
        // Simula la mossa
        String movingPiece = board[fromRow][fromCol];
        String capturedPiece = board[toRow][toCol];
        
        board[toRow][toCol] = movingPiece;
        board[fromRow][fromCol] = null;
        
        // Controlla se il re è ancora sotto scacco dopo la mossa
        boolean stillInCheck = isKingInCheckOnBoard(board, isWhiteTurn);
        
        // Ripristina la posizione
        board[fromRow][fromCol] = movingPiece;
        board[toRow][toCol] = capturedPiece;
        
        return !stillInCheck;
    }
    
    // Versione di isKingInCheck che lavora direttamente su una board
    private boolean isKingInCheckOnBoard(String[][] board, boolean isWhiteTurn) {
        // Trova la posizione del re
        int kingRow = -1, kingCol = -1;
        char kingPiece = isWhiteTurn ? 'K' : 'k';
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board[i][j] != null && board[i][j].charAt(0) == kingPiece) {
                    kingRow = i;
                    kingCol = j;
                    break;
                }
            }
        }
        
        if (kingRow == -1) return false; // Re non trovato
        
        // Controlla se qualche pezzo nemico può attaccare il re
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                String piece = board[i][j];
                if (piece != null) {
                    boolean isPieceWhite = Character.isUpperCase(piece.charAt(0));
                    if (isPieceWhite != isWhiteTurn) { // Pezzo nemico
                        char pieceType = piece.toLowerCase().charAt(0);
                        if (isValidPieceMove(pieceType, i, j, kingRow, kingCol, board)) {
                            return true; // Il re è sotto attacco
                        }
                    }
                }
            }
        }
        
        return false;
    }

    private boolean isCheckmate(String fen, boolean isWhiteTurn) {
        // Prova tutte le mosse possibili per vedere se ce n'è una che elimina lo scacco
        String[][] board = fenToBoard(fen.split(" ")[0]);
        
        for (int fromRow = 0; fromRow < 8; fromRow++) {
            for (int fromCol = 0; fromCol < 8; fromCol++) {
                String piece = board[fromRow][fromCol];
                if (piece != null) {
                    boolean isPieceWhite = Character.isUpperCase(piece.charAt(0));
                    if (isPieceWhite == isWhiteTurn) { // È un nostro pezzo
                        for (int toRow = 0; toRow < 8; toRow++) {
                            for (int toCol = 0; toCol < 8; toCol++) {
                                String from = "" + (char)('a' + fromCol) + (8 - fromRow);
                                String to = "" + (char)('a' + toCol) + (8 - toRow);
                                
                                if (isValidMove(fen, from, to, "q")) { // Prova la mossa
                                    String newFen = makeMove(fen, from, to, "q");
                                    if (!isKingInCheck(newFen, isWhiteTurn)) {
                                        return false; // Trovata una mossa che elimina lo scacco
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return true; // Nessuna mossa può eliminare lo scacco = scacco matto
    }
    
    private boolean isStalemate(String fen, boolean isWhiteTurn) {
        // Simile al checkmate ma senza essere in scacco
        if (isKingInCheck(fen, isWhiteTurn)) return false; // Non può essere stallo se è in scacco
        
        String[][] board = fenToBoard(fen.split(" ")[0]);
        
        for (int fromRow = 0; fromRow < 8; fromRow++) {
            for (int fromCol = 0; fromCol < 8; fromCol++) {
                String piece = board[fromRow][fromCol];
                if (piece != null) {
                    boolean isPieceWhite = Character.isUpperCase(piece.charAt(0));
                    if (isPieceWhite == isWhiteTurn) {
                        for (int toRow = 0; toRow < 8; toRow++) {
                            for (int toCol = 0; toCol < 8; toCol++) {
                                String from = "" + (char)('a' + fromCol) + (8 - fromRow);
                                String to = "" + (char)('a' + toCol) + (8 - toRow);
                                
                                if (isValidMove(fen, from, to, "q")) {
                                    return false; // Trovata una mossa legale
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return true; // Nessuna mossa legale = stallo
    }
    
    private String getCurrentTurn(String fen) {
        String turn = fen.split(" ")[1];
        return turn.equals("w") ? "Turno del Bianco" : "Turno del Nero";
    }
    
    @GetMapping("/games")
    public List<Game> getAllGames() {
        // Implementazione placeholder
        return List.of();
    }
    
    @PostMapping("/game/bot-move")
    @ResponseBody
    public Map<String, Object> botMove(@RequestBody Map<String, String> payload) {
        String fen = payload.get("fen");
        String level = payload.get("level");
        String botType = payload.get("botType");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String bestMove = null;
            String engineUsed = "Random Bot";
            int skillLevel = 1; // Default
            
            // Parse del livello Stockfish
            if (level != null && !level.equals("random")) {
                try {
                    skillLevel = Integer.parseInt(level);
                } catch (NumberFormatException e) {
                    skillLevel = 1;
                }
            }
            
            // Determina quale engine usare in base al botType
            if ("random".equals(botType) || "random".equals(level)) {
                bestMove = findRandomValidMove(fen);
                engineUsed = "Random Bot";
            } 
            // Usa Stockfish per tutti gli altri casi
            else {
                // Prova prima il nuovo Stockfish Simple con livello configurabile
                if (stockfishSimpleService.isAvailable()) {
                    bestMove = stockfishSimpleService.getBestMoveWithSkillLevel(fen, skillLevel);
                    engineUsed = "Stockfish " + skillLevel;
                }
                // Fallback al servizio originale
                else if (stockfishService.isAvailable()) {
                    bestMove = stockfishService.getBestMove(fen);
                    engineUsed = "Stockfish " + skillLevel;
                }
                
                // Fallback al bot casuale se Stockfish non è disponibile
                if (bestMove == null) {
                    bestMove = findRandomValidMove(fen);
                    engineUsed = "Random Bot (Fallback)";
                }
            }
            
            if (bestMove != null) {
                Map<String, String> moveObj = new HashMap<>();
                moveObj.put("from", bestMove.substring(0, 2));
                moveObj.put("to", bestMove.substring(2, 4));
                if (bestMove.length() > 4) {
                    moveObj.put("promotion", bestMove.substring(4, 5));
                }
                
                response.put("success", true);
                response.put("move", moveObj);
                response.put("level", skillLevel);
                response.put("botType", botType);
                response.put("engine", engineUsed);
                response.put("engineInfo", stockfishService.getEngineInfo());
            } else {
                response.put("success", false);
                response.put("error", "Nessuna mossa valida trovata");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Errore interno: " + e.getMessage());
        }
        
        return response;
    }
    
    private String findRandomValidMove(String fen) {
        // Implementazione semplificata: troviamo tutte le mosse valide e ne scegliamo una a caso
        String[][] board = fenToBoard(fen.split(" ")[0]);
        String[] fenParts = fen.split(" ");
        boolean isWhiteTurn = "w".equals(fenParts[1]);
        
        java.util.List<String> validMoves = new java.util.ArrayList<>();
        
        // Trova tutti i pezzi del giocatore corrente (il bot gioca sempre con i neri)
        for (int fromRow = 0; fromRow < 8; fromRow++) {
            for (int fromCol = 0; fromCol < 8; fromCol++) {
                String piece = board[fromRow][fromCol];
                if (piece != null) {
                    boolean isPieceWhite = Character.isUpperCase(piece.charAt(0));
                    // Il bot muove sempre i pezzi neri, quindi cerchiamo solo pezzi neri (!isPieceWhite)
                    // E deve essere il turno del nero (!isWhiteTurn)
                    if (isPieceWhite || isWhiteTurn) continue; // Salta se è un pezzo bianco o se è il turno del bianco
                    
                    // Prova tutte le caselle come destinazione
                    for (int toRow = 0; toRow < 8; toRow++) {
                        for (int toCol = 0; toCol < 8; toCol++) {
                            if (fromRow == toRow && fromCol == toCol) continue;
                            
                            String fromSquare = coordinateToAlgebraic(fromCol, fromRow);
                            String toSquare = coordinateToAlgebraic(toCol, toRow);
                            
                            // Controlla se la mossa è valida
                            if (isValidMove(fen, fromSquare, toSquare, "q")) {
                                validMoves.add(fromSquare + toSquare);
                            }
                        }
                    }
                }
            }
        }
        
        if (validMoves.isEmpty()) {
            return null;
        }
        
        // Restituisci una mossa casuale
        java.util.Random random = new java.util.Random();
        return validMoves.get(random.nextInt(validMoves.size()));
    }

    // Endpoint per ottenere la valutazione Stockfish 17 per una FEN
    @PostMapping("/eval")
    @ResponseBody
    public Map<String, Object> getEvaluation(@RequestBody Map<String, String> payload) {
        String fen = payload.get("fen");
        Map<String, Object> response = new HashMap<>();
        if (fen == null || fen.trim().isEmpty()) {
            response.put("success", false);
            response.put("error", "FEN mancante");
            return response;
        }
        String eval = stockfishSimpleService.getEvaluation(fen);
        response.put("success", eval != null);
        response.put("eval", eval);
        return response;
    }

}
