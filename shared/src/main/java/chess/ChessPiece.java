package chess;

import java.util.Collection;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

    private final ChessGame.TeamColor pieceColor;
    private final PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        switch (type) {
            case KING:
                return kingMoves(board, myPosition);
            case QUEEN:
                return queenMoves(board, myPosition);
            case BISHOP:
                return bishopMoves(board, myPosition);
            case KNIGHT:
                return knightMoves(board, myPosition);
            case ROOK:
                return rookMoves(board, myPosition);
            case PAWN:
                return pawnMoves(board, myPosition);
            default:
                throw new IllegalArgumentException("Unknown piece type: " + type);
        }
    }

    private boolean onBoard(int r, int c) {
        return r >= 1 && r <= 8 && c >= 1 && c <= 8;
    }

    private Collection<ChessMove> kingMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> moves = new java.util.ArrayList<>();
        int row = myPosition.getRow();
        int col = myPosition.getColumn();
        int[] dr = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dc = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int i = 0; i < 8; i++) {
            int newR = row + dr[i];
            int newC = col + dc[i];
            if (onBoard(newR, newC)) {
                ChessPosition target = new ChessPosition(newR, newC);
                ChessPiece targetPiece = board.getPiece(target);
                if (targetPiece == null || targetPiece.getTeamColor() != this.pieceColor) {
                    moves.add(new ChessMove(myPosition, target, null));
                }
            }
        }
        return moves;
    }

    private Collection<ChessMove> knightMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> moves = new java.util.ArrayList<>();
        int row = myPosition.getRow();
        int col = myPosition.getColumn();
        int[] dr = {-2, -2, -1, -1, 1, 1, 2, 2};
        int[] dc = {-1, 1, -2, 2, -2, 2, -1, 1};

        for (int i = 0; i < 8; i++) {
            int newR = row + dr[i];
            int newC = col + dc[i];
            if (onBoard(newR, newC)) {
                ChessPosition target = new ChessPosition(newR, newC);
                ChessPiece targetPiece = board.getPiece(target);
                if (targetPiece == null || targetPiece.getTeamColor() != this.pieceColor) {
                    moves.add(new ChessMove(myPosition, target, null));
                }
            }
        }
        return moves;
    }

    private Collection<ChessMove> slidingMoves(ChessBoard board, ChessPosition myPosition, int[] dr, int[] dc) {
        Collection<ChessMove> moves = new java.util.ArrayList<>();
        int row = myPosition.getRow();
        int col = myPosition.getColumn();

        for (int i = 0; i < dr.length; i++) {
            int step = 1;
            while (true) {
                int newR = row + dr[i] * step;
                int newC = col + dc[i] * step;
                if (!onBoard(newR, newC)) {
                    break;
                }
                ChessPosition target = new ChessPosition(newR, newC);
                ChessPiece targetPiece = board.getPiece(target);
                if (targetPiece == null) {
                    moves.add(new ChessMove(myPosition, target, null));
                } else {
                    if (targetPiece.getTeamColor() != this.pieceColor) {
                        moves.add(new ChessMove(myPosition, target, null));
                    }
                    break; // Blocked in this direction
                }
                step++;
            }
        }
        return moves;
    }

    private Collection<ChessMove> rookMoves(ChessBoard board, ChessPosition myPosition) {
        return slidingMoves(board, myPosition, new int[]{1, -1, 0, 0}, new int[]{0, 0, 1, -1});
    }

    private Collection<ChessMove> bishopMoves(ChessBoard board, ChessPosition myPosition) {
        return slidingMoves(board, myPosition, new int[]{1, 1, -1, -1}, new int[]{1, -1, 1, -1});
    }

    private Collection<ChessMove> queenMoves(ChessBoard board, ChessPosition myPosition) {
        return slidingMoves(board, myPosition, new int[]{1, -1, 0, 0, 1, 1, -1, -1}, new int[]{0, 0, 1, -1, 1, -1, 1, -1});
    }

    private Collection<ChessMove> pawnMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> moves = new java.util.ArrayList<>();
        int row = myPosition.getRow();
        int col = myPosition.getColumn();
        int direction = (pieceColor == ChessGame.TeamColor.WHITE) ? 1 : -1;
        int startRow = (pieceColor == ChessGame.TeamColor.WHITE) ? 2 : 7;
        int promoRow = (pieceColor == ChessGame.TeamColor.WHITE) ? 8 : 1;

        // 1. Single step forward
        int forwardR = row + direction;
        if (onBoard(forwardR, col)) {
            ChessPosition targetForward = new ChessPosition(forwardR, col);
            if (board.getPiece(targetForward) == null) {
                addPawnMove(moves, myPosition, targetForward, promoRow);

                // 2. Double step forward (only if single step is empty and we are at starting row)
                if (row == startRow) {
                    int doubleForwardR = row + 2 * direction;
                    if (onBoard(doubleForwardR, col)) {
                        ChessPosition targetDouble = new ChessPosition(doubleForwardR, col);
                        if (board.getPiece(targetDouble) == null) {
                            moves.add(new ChessMove(myPosition, targetDouble, null));
                        }
                    }
                }
            }
        }

        // 3. Captures
        int[] captureCols = {col - 1, col + 1};
        for (int c : captureCols) {
            if (onBoard(forwardR, c)) {
                ChessPosition targetCapture = new ChessPosition(forwardR, c);
                ChessPiece targetPiece = board.getPiece(targetCapture);
                if (targetPiece != null && targetPiece.getTeamColor() != this.pieceColor) {
                    addPawnMove(moves, myPosition, targetCapture, promoRow);
                }
            }
        }

        return moves;
    }

    private void addPawnMove(Collection<ChessMove> moves, ChessPosition from, ChessPosition to, int promoRow) {
        if (to.getRow() == promoRow) {
            moves.add(new ChessMove(from, to, PieceType.QUEEN));
            moves.add(new ChessMove(from, to, PieceType.ROOK));
            moves.add(new ChessMove(from, to, PieceType.BISHOP));
            moves.add(new ChessMove(from, to, PieceType.KNIGHT));
        } else {
            moves.add(new ChessMove(from, to, null));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChessPiece that = (ChessPiece) o;
        return pieceColor == that.pieceColor && type == that.type;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(pieceColor, type);
    }

    @Override
    public String toString() {
        return (pieceColor == ChessGame.TeamColor.WHITE ? "W" : "B") + type;
    }
}
