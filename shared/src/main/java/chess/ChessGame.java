package chess;

import java.util.Collection;

/**
 * A class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {

    private ChessBoard board;
    private TeamColor teamTurn;
    private ChessMove lastMove;

    // Track castling rights
    private boolean whiteKingMoved = false;
    private boolean whiteRookAMoved = false; // Queenside (col 1)
    private boolean whiteRookHMoved = false; // Kingside (col 8)
    private boolean blackKingMoved = false;
    private boolean blackRookAMoved = false; // Queenside (col 1)
    private boolean blackRookHMoved = false; // Kingside (col 8)

    public ChessGame() {
        this.board = new ChessBoard();
        this.board.resetBoard();
        this.teamTurn = TeamColor.WHITE;
        this.lastMove = null;
        this.whiteKingMoved = false;
        this.whiteRookAMoved = false;
        this.whiteRookHMoved = false;
        this.blackKingMoved = false;
        this.blackRookAMoved = false;
        this.blackRookHMoved = false;
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return teamTurn;
    }

    /**
     * Sets which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        this.teamTurn = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    /**
     * Gets all valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);
        if (piece == null) {
            return null;
        }

        Collection<ChessMove> pieceMoves = piece.pieceMoves(board, startPosition);
        Collection<ChessMove> validMoves = new java.util.ArrayList<>();

        // Filter standard piece moves for check
        for (ChessMove move : pieceMoves) {
            if (simulateAndVerify(move, piece.getTeamColor())) {
                validMoves.add(move);
            }
        }

        // Generate Castling moves for King
        if (piece.getPieceType() == ChessPiece.PieceType.KING) {
            addValidCastlingMoves(startPosition, piece.getTeamColor(), validMoves);
        }

        // Generate En Passant moves for Pawn
        if (piece.getPieceType() == ChessPiece.PieceType.PAWN) {
            addValidEnPassantMoves(startPosition, piece.getTeamColor(), validMoves);
        }

        return validMoves;
    }

    /**
     * Makes a move in the chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        ChessPosition start = move.getStartPosition();
        
        ChessPiece piece = board.getPiece(start);
        if (piece == null) {
            throw new InvalidMoveException("No piece at start position");
        }
        
        if (piece.getTeamColor() != teamTurn) {
            throw new InvalidMoveException("Not your turn");
        }
        
        Collection<ChessMove> valid = validMoves(start);
        if (valid == null || !valid.contains(move)) {
            throw new InvalidMoveException("Invalid move");
        }
        
        // Execute the move on the board
        executeMove(move, piece);
        
        // Update castling rights if a King or Rook moves, or if a Rook is captured at its starting square
        updateCastlingRightsAfterMove(move, piece);
        
        // Set lastMove
        lastMove = move;
        
        // Switch turn
        teamTurn = (teamTurn == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }

    private void executeMove(ChessMove move, ChessPiece piece) {
        ChessPosition start = move.getStartPosition();
        ChessPosition end = move.getEndPosition();
        
        // Detect En Passant
        boolean isEnPassant = false;
        if (piece.getPieceType() == ChessPiece.PieceType.PAWN) {
            if (start.getColumn() != end.getColumn() && board.getPiece(end) == null) {
                isEnPassant = true;
            }
        }
        
        // Detect Castling
        boolean isCastling = false;
        if (piece.getPieceType() == ChessPiece.PieceType.KING) {
            if (Math.abs(start.getColumn() - end.getColumn()) == 2) {
                isCastling = true;
            }
        }
        
        if (isEnPassant) {
            // Remove the captured pawn
            ChessPosition capturedPos = new ChessPosition(start.getRow(), end.getColumn());
            board.addPiece(capturedPos, null);
            
            // Move pawn
            board.addPiece(end, piece);
            board.addPiece(start, null);
        } else if (isCastling) {
            // Move King
            board.addPiece(end, piece);
            board.addPiece(start, null);
            
            // Move Rook
            int row = start.getRow();
            if (end.getColumn() == 7) { // King-side
                ChessPiece rook = board.getPiece(new ChessPosition(row, 8));
                board.addPiece(new ChessPosition(row, 6), rook);
                board.addPiece(new ChessPosition(row, 8), null);
            } else { // Queen-side
                ChessPiece rook = board.getPiece(new ChessPosition(row, 1));
                board.addPiece(new ChessPosition(row, 4), rook);
                board.addPiece(new ChessPosition(row, 1), null);
            }
        } else {
            // Standard move (with potential promotion)
            if (move.getPromotionPiece() != null) {
                ChessPiece promoted = new ChessPiece(piece.getTeamColor(), move.getPromotionPiece());
                board.addPiece(end, promoted);
            } else {
                board.addPiece(end, piece);
            }
            board.addPiece(start, null);
        }
    }

    private void updateCastlingRightsAfterMove(ChessMove move, ChessPiece piece) {
        ChessPosition start = move.getStartPosition();
        ChessPosition end = move.getEndPosition();
        
        // King moves
        if (piece.getPieceType() == ChessPiece.PieceType.KING) {
            if (piece.getTeamColor() == TeamColor.WHITE) {
                whiteKingMoved = true;
            } else {
                blackKingMoved = true;
            }
        }
        
        // Rook moves (checking starting positions)
        if (start.getRow() == 1 && start.getColumn() == 1) {
            whiteRookAMoved = true;
        }
        if (start.getRow() == 1 && start.getColumn() == 8) {
            whiteRookHMoved = true;
        }
        if (start.getRow() == 8 && start.getColumn() == 1) {
            blackRookAMoved = true;
        }
        if (start.getRow() == 8 && start.getColumn() == 8) {
            blackRookHMoved = true;
        }
        
        // Rook captures (checking target positions)
        if (end.getRow() == 1 && end.getColumn() == 1) {
            whiteRookAMoved = true;
        }
        if (end.getRow() == 1 && end.getColumn() == 8) {
            whiteRookHMoved = true;
        }
        if (end.getRow() == 8 && end.getColumn() == 1) {
            blackRookAMoved = true;
        }
        if (end.getRow() == 8 && end.getColumn() == 8) {
            blackRookHMoved = true;
        }
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition kingPos = findKing(teamColor);
        if (kingPos == null) {
            return false;
        }
        
        // Check if any opponent piece can capture the king
        TeamColor opponentColor = (teamColor == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece piece = board.getPiece(pos);
                if (piece != null && piece.getTeamColor() == opponentColor) {
                    Collection<ChessMove> moves = piece.pieceMoves(board, pos);
                    for (ChessMove m : moves) {
                        if (m.getEndPosition().equals(kingPos)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private ChessPosition findKing(TeamColor teamColor) {
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece piece = board.getPiece(pos);
                if (piece != null && piece.getPieceType() == ChessPiece.PieceType.KING && piece.getTeamColor() == teamColor) {
                    return pos;
                }
            }
        }
        return null;
    }

    private boolean isSquareAttacked(TeamColor teamColor, ChessPosition position) {
        TeamColor opponentColor = (teamColor == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece piece = board.getPiece(pos);
                if (piece != null && piece.getTeamColor() == opponentColor) {
                    Collection<ChessMove> moves = piece.pieceMoves(board, pos);
                    for (ChessMove m : moves) {
                        if (m.getEndPosition().equals(position)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean simulateAndVerify(ChessMove move, TeamColor color) {
        ChessPosition start = move.getStartPosition();
        ChessPosition end = move.getEndPosition();
        
        ChessPiece movingPiece = board.getPiece(start);
        ChessPiece targetPiece = board.getPiece(end);
        
        // Apply temporary move
        board.addPiece(end, movingPiece);
        board.addPiece(start, null);
        
        boolean inCheck = isInCheck(color);
        
        // Restore board
        board.addPiece(start, movingPiece);
        board.addPiece(end, targetPiece);
        
        return !inCheck;
    }

    private boolean simulateEnPassantAndVerify(ChessMove move, TeamColor color) {
        ChessPosition start = move.getStartPosition();
        ChessPosition end = move.getEndPosition();
        
        // For en passant, the captured pawn is at (start.getRow(), end.getColumn())
        ChessPosition capturedPos = new ChessPosition(start.getRow(), end.getColumn());
        
        ChessPiece movingPiece = board.getPiece(start);
        ChessPiece capturedPiece = board.getPiece(capturedPos);
        
        // Apply temporary move
        board.addPiece(end, movingPiece);
        board.addPiece(start, null);
        board.addPiece(capturedPos, null);
        
        boolean inCheck = isInCheck(color);
        
        // Restore board
        board.addPiece(start, movingPiece);
        board.addPiece(end, null);
        board.addPiece(capturedPos, capturedPiece);
        
        return !inCheck;
    }

    private void addValidEnPassantMoves(ChessPosition startPosition, TeamColor color, Collection<ChessMove> validMoves) {
        if (lastMove == null) return;
        
        ChessPiece opponentPawn = board.getPiece(lastMove.getEndPosition());
        if (opponentPawn == null || opponentPawn.getPieceType() != ChessPiece.PieceType.PAWN || opponentPawn.getTeamColor() == color) {
            return;
        }
        
        // Must be a double-step move
        int startRow = lastMove.getStartPosition().getRow();
        int endRow = lastMove.getEndPosition().getRow();
        if (Math.abs(startRow - endRow) != 2) {
            return;
        }
        
        // Our pawn must be on the same row as the opponent pawn's end position
        if (startPosition.getRow() != endRow) {
            return;
        }
        
        // Columns must be adjacent
        if (Math.abs(startPosition.getColumn() - lastMove.getEndPosition().getColumn()) != 1) {
            return;
        }
        
        // The destination is the square passed over by the opponent's pawn
        int direction = (color == TeamColor.WHITE) ? 1 : -1;
        ChessPosition destination = new ChessPosition(endRow + direction, lastMove.getEndPosition().getColumn());
        
        ChessMove epMove = new ChessMove(startPosition, destination, null);
        
        // Simulate to verify it doesn't leave the king in check
        if (simulateEnPassantAndVerify(epMove, color)) {
            validMoves.add(epMove);
        }
    }

    private void addValidCastlingMoves(ChessPosition startPosition, TeamColor color, Collection<ChessMove> validMoves) {
        // King must be at its starting position and must not have moved
        if (color == TeamColor.WHITE) {
            if (whiteKingMoved || startPosition.getRow() != 1 || startPosition.getColumn() != 5) {
                return;
            }
            if (isInCheck(TeamColor.WHITE)) {
                return;
            }
            
            // White King-side Castle (to col 7)
            if (!whiteRookHMoved) {
                // Check if squares between them are empty (F1, G1)
                if (board.getPiece(new ChessPosition(1, 6)) == null && board.getPiece(new ChessPosition(1, 7)) == null) {
                    // Check that the king does not cross or land on a square under attack (F1, G1)
                    if (!isSquareAttacked(TeamColor.WHITE, new ChessPosition(1, 6)) &&
                        !isSquareAttacked(TeamColor.WHITE, new ChessPosition(1, 7))) {
                        validMoves.add(new ChessMove(startPosition, new ChessPosition(1, 7), null));
                    }
                }
            }
            
            // White Queen-side Castle (to col 3)
            if (!whiteRookAMoved) {
                // Check if squares between them are empty (B1, C1, D1)
                if (board.getPiece(new ChessPosition(1, 2)) == null &&
                    board.getPiece(new ChessPosition(1, 3)) == null &&
                    board.getPiece(new ChessPosition(1, 4)) == null) {
                    // Check that the king does not cross or land on a square under attack (C1, D1)
                    if (!isSquareAttacked(TeamColor.WHITE, new ChessPosition(1, 3)) &&
                        !isSquareAttacked(TeamColor.WHITE, new ChessPosition(1, 4))) {
                        validMoves.add(new ChessMove(startPosition, new ChessPosition(1, 3), null));
                    }
                }
            }
        } else {
            // Black
            if (blackKingMoved || startPosition.getRow() != 8 || startPosition.getColumn() != 5) {
                return;
            }
            if (isInCheck(TeamColor.BLACK)) {
                return;
            }
            
            // Black King-side Castle (to col 7)
            if (!blackRookHMoved) {
                // Check if squares between them are empty (F8, G8)
                if (board.getPiece(new ChessPosition(8, 6)) == null && board.getPiece(new ChessPosition(8, 7)) == null) {
                    // Check that the king does not cross or land on a square under attack (F8, G8)
                    if (!isSquareAttacked(TeamColor.BLACK, new ChessPosition(8, 6)) &&
                        !isSquareAttacked(TeamColor.BLACK, new ChessPosition(8, 7))) {
                        validMoves.add(new ChessMove(startPosition, new ChessPosition(8, 7), null));
                    }
                }
            }
            
            // Black Queen-side Castle (to col 3)
            if (!blackRookAMoved) {
                // Check if squares between them are empty (B8, C8, D8)
                if (board.getPiece(new ChessPosition(8, 2)) == null &&
                    board.getPiece(new ChessPosition(8, 3)) == null &&
                    board.getPiece(new ChessPosition(8, 4)) == null) {
                    // Check that the king does not cross or land on a square under attack (C8, D8)
                    if (!isSquareAttacked(TeamColor.BLACK, new ChessPosition(8, 3)) &&
                        !isSquareAttacked(TeamColor.BLACK, new ChessPosition(8, 4))) {
                        validMoves.add(new ChessMove(startPosition, new ChessPosition(8, 3), null));
                    }
                }
            }
        }
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        if (!isInCheck(teamColor)) {
            return false;
        }
        return hasNoValidMoves(teamColor);
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves while not in check.
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        if (isInCheck(teamColor)) {
            return false;
        }
        return hasNoValidMoves(teamColor);
    }

    private boolean hasNoValidMoves(TeamColor teamColor) {
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece piece = board.getPiece(pos);
                if (piece != null && piece.getTeamColor() == teamColor) {
                    Collection<ChessMove> moves = validMoves(pos);
                    if (moves != null && !moves.isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Sets this game's chessboard to a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
        inferCastlingFlags();
    }

    private void inferCastlingFlags() {
        if (board == null) return;
        
        // White King
        ChessPiece wKing = board.getPiece(new ChessPosition(1, 5));
        whiteKingMoved = !(wKing != null && wKing.getPieceType() == ChessPiece.PieceType.KING && wKing.getTeamColor() == TeamColor.WHITE);
        
        // White Queenside Rook
        ChessPiece wRookA = board.getPiece(new ChessPosition(1, 1));
        whiteRookAMoved = !(wRookA != null && wRookA.getPieceType() == ChessPiece.PieceType.ROOK && wRookA.getTeamColor() == TeamColor.WHITE);
        
        // White Kingside Rook
        ChessPiece wRookH = board.getPiece(new ChessPosition(1, 8));
        whiteRookHMoved = !(wRookH != null && wRookH.getPieceType() == ChessPiece.PieceType.ROOK && wRookH.getTeamColor() == TeamColor.WHITE);
        
        // Black King
        ChessPiece bKing = board.getPiece(new ChessPosition(8, 5));
        blackKingMoved = !(bKing != null && bKing.getPieceType() == ChessPiece.PieceType.KING && bKing.getTeamColor() == TeamColor.BLACK);
        
        // Black Queenside Rook
        ChessPiece bRookA = board.getPiece(new ChessPosition(8, 1));
        blackRookAMoved = !(bRookA != null && bRookA.getPieceType() == ChessPiece.PieceType.ROOK && bRookA.getTeamColor() == TeamColor.BLACK);
        
        // Black Kingside Rook
        ChessPiece bRookH = board.getPiece(new ChessPosition(8, 8));
        blackRookHMoved = !(bRookH != null && bRookH.getPieceType() == ChessPiece.PieceType.ROOK && bRookH.getTeamColor() == TeamColor.BLACK);
        
        lastMove = null;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return board;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChessGame chessGame = (ChessGame) o;
        return whiteKingMoved == chessGame.whiteKingMoved &&
                whiteRookAMoved == chessGame.whiteRookAMoved &&
                whiteRookHMoved == chessGame.whiteRookHMoved &&
                blackKingMoved == chessGame.blackKingMoved &&
                blackRookAMoved == chessGame.blackRookAMoved &&
                blackRookHMoved == chessGame.blackRookHMoved &&
                java.util.Objects.equals(board, chessGame.board) &&
                teamTurn == chessGame.teamTurn &&
                java.util.Objects.equals(lastMove, chessGame.lastMove);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(board, teamTurn, lastMove,
                whiteKingMoved, whiteRookAMoved, whiteRookHMoved,
                blackKingMoved, blackRookAMoved, blackRookHMoved);
    }
}

