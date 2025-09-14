package com.chess.chessverse.service;

import org.springframework.stereotype.Service;

@Service
public class ClassicChessLogic {
    
    /**
     * Aggiorna il FEN eseguendo una mossa, gestendo arrocco, en passant e altre regole speciali
     */
    public String updateFEN(String fen, String from, String to, String promotion) {
        try {
            String[] parts = fen.split(" ");
            String board = parts[0];
            String turn = parts[1];
            String castlingRights = parts[2];
            String enPassantTarget = parts[3];
            int halfmoveClock = Integer.parseInt(parts[4]);
            int fullmoveNumber = Integer.parseInt(parts[5]);
            
            // Converti coordinate algebraiche a indici
            int fromCol = from.charAt(0) - 'a';
            int fromRow = 8 - (from.charAt(1) - '0');
            int toCol = to.charAt(0) - 'a';
            int toRow = 8 - (to.charAt(1) - '0');
            
            // Converti FEN in array 2D
            String[][] chessBoard = fenToBoard(board);
            String piece = chessBoard[fromRow][fromCol];
            
            boolean isWhite = Character.isUpperCase(piece.charAt(0));
            boolean isCapture = chessBoard[toRow][toCol] != null;
            boolean isPawnMove = piece.toLowerCase().charAt(0) == 'p';
            
            // Gestisci arrocco
            if (piece.toLowerCase().charAt(0) == 'k' && Math.abs(toCol - fromCol) == 2) {
                if (canCastle(fen, from, to)) {
                    // Esegui arrocco
                    chessBoard[fromRow][fromCol] = null;
                    chessBoard[toRow][toCol] = piece;
                    
                    // Muovi la torre
                    if (toCol == 6) { // Arrocco corto
                        String rook = chessBoard[fromRow][7];
                        chessBoard[fromRow][7] = null;
                        chessBoard[fromRow][5] = rook;
                    } else { // Arrocco lungo
                        String rook = chessBoard[fromRow][0];
                        chessBoard[fromRow][0] = null;
                        chessBoard[fromRow][3] = rook;
                    }
                }
            }
            // Gestisci en passant
            else if (isPawnMove && toCol != fromCol && chessBoard[toRow][toCol] == null) {
                if (canEnPassant(fen, from, to)) {
                    // Esegui en passant
                    chessBoard[fromRow][fromCol] = null;
                    chessBoard[toRow][toCol] = piece;
                    // Rimuovi il pedone catturato
                    chessBoard[fromRow][toCol] = null;
                }
            }
            // Mossa normale
            else {
                chessBoard[fromRow][fromCol] = null;
                
                // Gestisci promozione pedone
                if (isPawnMove && ((isWhite && toRow == 0) || (!isWhite && toRow == 7))) {
                    if (promotion != null && !promotion.isEmpty()) {
                        char promotionPiece = promotion.charAt(0);
                        piece = isWhite ? String.valueOf(Character.toUpperCase(promotionPiece)) : 
                                         String.valueOf(Character.toLowerCase(promotionPiece));
                    } else {
                        piece = isWhite ? "Q" : "q";
                    }
                }
                
                chessBoard[toRow][toCol] = piece;
            }
            
            // Aggiorna diritti di arrocco
            String newCastlingRights = updateCastlingRights(castlingRights, from, to, piece);
            
            // Aggiorna en passant target
            String newEnPassantTarget = updateEnPassantTarget(from, to, piece);
            
            // Aggiorna halfmove clock
            int newHalfmoveClock = (isCapture || isPawnMove) ? 0 : halfmoveClock + 1;
            
            // Aggiorna fullmove number
            int newFullmoveNumber = turn.equals("b") ? fullmoveNumber + 1 : fullmoveNumber;
            
            // Cambia turno
            String newTurn = turn.equals("w") ? "b" : "w";
            
            // Converti board in FEN
            String newBoard = boardToFen(chessBoard);
            
            return newBoard + " " + newTurn + " " + newCastlingRights + " " + 
                   newEnPassantTarget + " " + newHalfmoveClock + " " + newFullmoveNumber;
                   
        } catch (Exception e) {
            return fen; // Ritorna FEN originale in caso di errore
        }
    }
    
    /**
     * Verifica se l'arrocco è possibile
     */
    public boolean canCastle(String fen, String from, String to) {
        String[] parts = fen.split(" ");
        String board = parts[0];
        String turn = parts[1];
        String castlingRights = parts[2];
        
        // Converti coordinate
        int fromCol = from.charAt(0) - 'a';
        int fromRow = 8 - (from.charAt(1) - '0');
        int toCol = to.charAt(0) - 'a';
        
        boolean isWhite = turn.equals("w");
        boolean isKingside = toCol == 6;
        boolean isQueenside = toCol == 2;
        
        // Verifica diritti di arrocco
        if (isWhite) {
            if (isKingside && !castlingRights.contains("K")) return false;
            if (isQueenside && !castlingRights.contains("Q")) return false;
        } else {
            if (isKingside && !castlingRights.contains("k")) return false;
            if (isQueenside && !castlingRights.contains("q")) return false;
        }
        
        // Verifica che le caselle siano libere
        String[][] chessBoard = fenToBoard(board);
        
        if (isKingside) {
            if (chessBoard[fromRow][5] != null || chessBoard[fromRow][6] != null) return false;
        } else {
            if (chessBoard[fromRow][1] != null || chessBoard[fromRow][2] != null || chessBoard[fromRow][3] != null) return false;
        }
        
        // Verifica che il re non sia sotto scacco e non passi attraverso caselle attaccate
        if (isSquareUnderAttack(fen, from, !isWhite)) return false;
        
        String middleSquare = isKingside ? 
            String.valueOf((char)('a' + 5)) + from.charAt(1) :
            String.valueOf((char)('a' + 3)) + from.charAt(1);
        
        if (isSquareUnderAttack(fen, middleSquare, !isWhite)) return false;
        if (isSquareUnderAttack(fen, to, !isWhite)) return false;
        
        return true;
    }
    
    /**
     * Verifica se l'en passant è possibile
     */
    public boolean canEnPassant(String fen, String from, String to) {
        String[] parts = fen.split(" ");
        String enPassantTarget = parts[3];
        
        // Se non c'è target en passant, non è possibile
        if (enPassantTarget.equals("-")) return false;
        
        // Verifica che la mossa corrisponda al target en passant
        return to.equals(enPassantTarget);
    }
    
    /**
     * Converte FEN board in array 2D
     */
    private String[][] fenToBoard(String fen) {
        String[][] board = new String[8][8];
        String[] rows = fen.split("/");
        
        for (int i = 0; i < 8; i++) {
            String row = rows[i];
            int col = 0;
            
            for (char c : row.toCharArray()) {
                if (Character.isDigit(c)) {
                    int emptySquares = c - '0';
                    for (int j = 0; j < emptySquares; j++) {
                        board[i][col++] = null;
                    }
                } else {
                    board[i][col++] = String.valueOf(c);
                }
            }
        }
        
        return board;
    }
    
    /**
     * Converte array 2D in FEN board
     */
    private String boardToFen(String[][] board) {
        StringBuilder fen = new StringBuilder();
        
        for (int i = 0; i < 8; i++) {
            if (i > 0) fen.append("/");
            
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
        }
        
        return fen.toString();
    }
    
    /**
     * Verifica se una casella è sotto attacco
     */
    private boolean isSquareUnderAttack(String fen, String square, boolean byWhite) {
        // Implementazione semplificata - verifica solo attacchi diretti
        String[] parts = fen.split(" ");
        String board = parts[0];
        String[][] chessBoard = fenToBoard(board);
        
        int targetCol = square.charAt(0) - 'a';
        int targetRow = 8 - (square.charAt(1) - '0');
        
        // Cerca pezzi che possono attaccare la casella target
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                String piece = chessBoard[row][col];
                if (piece == null) continue;
                
                boolean isPieceWhite = Character.isUpperCase(piece.charAt(0));
                if (isPieceWhite != byWhite) continue;
                
                // Verifica se questo pezzo può attaccare la casella target
                if (canPieceAttack(chessBoard, row, col, targetRow, targetCol, piece.toLowerCase().charAt(0))) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Verifica se un pezzo può attaccare una casella specifica
     */
    private boolean canPieceAttack(String[][] board, int fromRow, int fromCol, int toRow, int toCol, char pieceType) {
        int deltaRow = toRow - fromRow;
        int deltaCol = toCol - fromCol;
        
        switch (pieceType) {
            case 'p': // Pedone
                boolean isWhite = Character.isUpperCase(board[fromRow][fromCol].charAt(0));
                int direction = isWhite ? -1 : 1;
                return deltaRow == direction && Math.abs(deltaCol) == 1;
                
            case 'r': // Torre
                return (deltaRow == 0 || deltaCol == 0) && isPathClear(board, fromRow, fromCol, toRow, toCol);
                
            case 'n': // Cavallo
                return (Math.abs(deltaRow) == 2 && Math.abs(deltaCol) == 1) || 
                       (Math.abs(deltaRow) == 1 && Math.abs(deltaCol) == 2);
                
            case 'b': // Alfiere
                return Math.abs(deltaRow) == Math.abs(deltaCol) && isPathClear(board, fromRow, fromCol, toRow, toCol);
                
            case 'q': // Regina
                return ((deltaRow == 0 || deltaCol == 0) || (Math.abs(deltaRow) == Math.abs(deltaCol))) &&
                       isPathClear(board, fromRow, fromCol, toRow, toCol);
                
            case 'k': // Re
                return Math.abs(deltaRow) <= 1 && Math.abs(deltaCol) <= 1;
        }
        
        return false;
    }
    
    /**
     * Verifica se il percorso tra due caselle è libero
     */
    private boolean isPathClear(String[][] board, int fromRow, int fromCol, int toRow, int toCol) {
        int deltaRow = Integer.signum(toRow - fromRow);
        int deltaCol = Integer.signum(toCol - fromCol);
        
        int currentRow = fromRow + deltaRow;
        int currentCol = fromCol + deltaCol;
        
        while (currentRow != toRow || currentCol != toCol) {
            if (board[currentRow][currentCol] != null) {
                return false;
            }
            currentRow += deltaRow;
            currentCol += deltaCol;
        }
        
        return true;
    }
    
    /**
     * Aggiorna i diritti di arrocco dopo una mossa
     */
    private String updateCastlingRights(String castlingRights, String from, String to, String piece) {
        StringBuilder newRights = new StringBuilder(castlingRights);
        
        // Se il re si muove, perde tutti i diritti di arrocco
        if (piece.toLowerCase().charAt(0) == 'k') {
            if (Character.isUpperCase(piece.charAt(0))) {
                newRights = new StringBuilder(newRights.toString().replace("K", "").replace("Q", ""));
            } else {
                newRights = new StringBuilder(newRights.toString().replace("k", "").replace("q", ""));
            }
        }
        
        // Se una torre si muove o viene catturata, perde il diritto di arrocco
        if (from.equals("a1") || to.equals("a1")) newRights = new StringBuilder(newRights.toString().replace("Q", ""));
        if (from.equals("h1") || to.equals("h1")) newRights = new StringBuilder(newRights.toString().replace("K", ""));
        if (from.equals("a8") || to.equals("a8")) newRights = new StringBuilder(newRights.toString().replace("q", ""));
        if (from.equals("h8") || to.equals("h8")) newRights = new StringBuilder(newRights.toString().replace("k", ""));
        
        return newRights.length() > 0 ? newRights.toString() : "-";
    }
    
    /**
     * Aggiorna il target en passant dopo una mossa
     */
    private String updateEnPassantTarget(String from, String to, String piece) {
        // Se un pedone si muove di due caselle, imposta en passant target
        if (piece.toLowerCase().charAt(0) == 'p') {
            int fromRow = 8 - (from.charAt(1) - '0');
            int toRow = 8 - (to.charAt(1) - '0');
            
            if (Math.abs(toRow - fromRow) == 2) {
                int targetRow = (fromRow + toRow) / 2;
                char targetCol = from.charAt(0);
                return String.valueOf(targetCol) + (8 - targetRow);
            }
        }
        
        return "-";
    }
}
