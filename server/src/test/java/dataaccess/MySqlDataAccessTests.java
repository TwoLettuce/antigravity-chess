package dataaccess;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.*;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MySqlDataAccess}.
 * Positive and negative test cases for each public DAO method.
 * Clear only requires a positive test per the rubric.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MySqlDataAccessTests {

    private static MySqlDataAccess dao;

    @BeforeAll
    static void setup() throws DataAccessException {
        dao = new MySqlDataAccess();
    }

    @BeforeEach
    void clearDb() throws DataAccessException {
        dao.clear();
    }

    // =========================================================================
    // Clear
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("Clear – removes all data")
    void clearPositive() throws DataAccessException {
        dao.createUser(new UserData("alice", "pw", "alice@example.com"));
        dao.createGame("Game1");
        dao.createAuth(new AuthData("token1", "alice"));

        dao.clear();

        assertNull(dao.getUser("alice"), "User should be gone after clear");
        assertTrue(dao.listGames().isEmpty(), "Games should be gone after clear");
        assertNull(dao.getAuth("token1"), "Auth token should be gone after clear");
    }

    // =========================================================================
    // createUser
    // =========================================================================

    @Test
    @Order(2)
    @DisplayName("createUser – success")
    void createUserPositive() throws DataAccessException {
        dao.createUser(new UserData("bob", "secret", "bob@example.com"));
        UserData fetched = dao.getUser("bob");
        assertNotNull(fetched, "User should exist after creation");
        assertEquals("bob", fetched.username());
        assertEquals("bob@example.com", fetched.email());
    }

    @Test
    @Order(3)
    @DisplayName("createUser – duplicate username throws AlreadyTakenException")
    void createUserNegativeDuplicate() throws DataAccessException {
        dao.createUser(new UserData("charlie", "pw", "charlie@example.com"));
        assertThrows(AlreadyTakenException.class,
                () -> dao.createUser(new UserData("charlie", "other", "c2@example.com")),
                "Duplicate username should throw AlreadyTakenException");
    }

    // =========================================================================
    // getUser
    // =========================================================================

    @Test
    @Order(4)
    @DisplayName("getUser – returns existing user")
    void getUserPositive() throws DataAccessException {
        dao.createUser(new UserData("diana", "pw", "diana@example.com"));
        UserData user = dao.getUser("diana");
        assertNotNull(user);
        assertEquals("diana", user.username());
    }

    @Test
    @Order(5)
    @DisplayName("getUser – returns null for non-existent user")
    void getUserNegative() throws DataAccessException {
        UserData user = dao.getUser("nobody");
        assertNull(user, "getUser should return null when user does not exist");
    }

    // =========================================================================
    // createGame
    // =========================================================================

    @Test
    @Order(6)
    @DisplayName("createGame – returns valid positive ID")
    void createGamePositive() throws DataAccessException {
        int id = dao.createGame("Epic Chess Match");
        assertTrue(id > 0, "Game ID should be a positive integer");
        GameData game = dao.getGame(id);
        assertNotNull(game);
        assertEquals("Epic Chess Match", game.gameName());
    }

    @Test
    @Order(7)
    @DisplayName("createGame – null name throws DataAccessException")
    void createGameNegativeNullName() {
        assertThrows(DataAccessException.class,
                () -> dao.createGame(null),
                "Null game name should throw DataAccessException");
    }

    // =========================================================================
    // getGame
    // =========================================================================

    @Test
    @Order(8)
    @DisplayName("getGame – returns correct game")
    void getGamePositive() throws DataAccessException {
        int id = dao.createGame("Retrieval Test");
        GameData game = dao.getGame(id);
        assertNotNull(game);
        assertEquals(id, game.gameID());
        assertEquals("Retrieval Test", game.gameName());
        assertNotNull(game.game(), "ChessGame object should not be null");
    }

    @Test
    @Order(9)
    @DisplayName("getGame – returns null for non-existent game ID")
    void getGameNegative() throws DataAccessException {
        GameData game = dao.getGame(Integer.MAX_VALUE);
        assertNull(game, "getGame should return null for unknown game ID");
    }

    // =========================================================================
    // listGames
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("listGames – returns correct number of games")
    void listGamesPositive() throws DataAccessException {
        dao.createGame("Game A");
        dao.createGame("Game B");
        dao.createGame("Game C");
        Collection<GameData> games = dao.listGames();
        assertEquals(3, games.size(), "listGames should return all created games");
    }

    @Test
    @Order(11)
    @DisplayName("listGames – returns empty collection when no games exist")
    void listGamesEmpty() throws DataAccessException {
        Collection<GameData> games = dao.listGames();
        assertNotNull(games);
        assertTrue(games.isEmpty(), "listGames should be empty when no games have been created");
    }

    // =========================================================================
    // updateGame
    // =========================================================================

    @Test
    @Order(12)
    @DisplayName("updateGame – persists updated game state")
    void updateGamePositive() throws DataAccessException {
        dao.createUser(new UserData("eve", "pw", "eve@example.com"));
        int id = dao.createGame("Update Test");
        GameData original = dao.getGame(id);

        // Simulate making a move: update white username
        GameData updated = new GameData(id, "eve", null, original.gameName(), original.game());
        dao.updateGame(updated);

        GameData fetched = dao.getGame(id);
        assertNotNull(fetched);
        assertEquals("eve", fetched.whiteUsername(), "White username should be updated");
    }

    @Test
    @Order(13)
    @DisplayName("updateGame – throws DataAccessException for non-existent game")
    void updateGameNegative() {
        ChessGame fakeGame = new ChessGame();
        GameData nonExistent = new GameData(Integer.MAX_VALUE, null, null, "Ghost Game", fakeGame);
        assertThrows(DataAccessException.class,
                () -> dao.updateGame(nonExistent),
                "updateGame should throw DataAccessException for a game that does not exist");
    }

    // =========================================================================
    // updateGame – board/move state persisted (extra board test per rubric)
    // =========================================================================

    @Test
    @Order(14)
    @DisplayName("updateGame – chess move state is persisted and restored")
    void updateGameBoardStatePersisted() throws Exception {
        int id = dao.createGame("Board State Test");
        GameData original = dao.getGame(id);

        // Make a real move: e2 -> e4 (white pawn)
        ChessGame game = original.game();
        game.makeMove(new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null));

        GameData withMove = new GameData(id, null, null, original.gameName(), game);
        dao.updateGame(withMove);

        GameData fetched = dao.getGame(id);
        assertNotNull(fetched);
        // Verify the board state: pawn should now be at e4 (row 4, col 5)
        assertNotNull(fetched.game().getBoard().getPiece(new ChessPosition(4, 5)),
                "Pawn should be at e4 after move is persisted and restored");
        assertNull(fetched.game().getBoard().getPiece(new ChessPosition(2, 5)),
                "e2 should be empty after the pawn moved");
    }

    // =========================================================================
    // createAuth
    // =========================================================================

    @Test
    @Order(15)
    @DisplayName("createAuth – auth token is created successfully")
    void createAuthPositive() throws DataAccessException {
        dao.createAuth(new AuthData("unique-token-abc", "frank"));
        AuthData auth = dao.getAuth("unique-token-abc");
        assertNotNull(auth, "Auth token should be retrievable after creation");
        assertEquals("frank", auth.username());
    }

    @Test
    @Order(16)
    @DisplayName("createAuth – duplicate token throws DataAccessException")
    void createAuthNegativeDuplicate() throws DataAccessException {
        dao.createAuth(new AuthData("dup-token", "grace"));
        assertThrows(DataAccessException.class,
                () -> dao.createAuth(new AuthData("dup-token", "henry")),
                "Duplicate auth token should throw DataAccessException");
    }

    // =========================================================================
    // getAuth
    // =========================================================================

    @Test
    @Order(17)
    @DisplayName("getAuth – returns correct auth data")
    void getAuthPositive() throws DataAccessException {
        dao.createAuth(new AuthData("valid-token", "iris"));
        AuthData auth = dao.getAuth("valid-token");
        assertNotNull(auth);
        assertEquals("valid-token", auth.authToken());
        assertEquals("iris", auth.username());
    }

    @Test
    @Order(18)
    @DisplayName("getAuth – returns null for non-existent token")
    void getAuthNegative() throws DataAccessException {
        AuthData auth = dao.getAuth("nonexistent-token");
        assertNull(auth, "getAuth should return null for a token that does not exist");
    }

    // =========================================================================
    // deleteAuth
    // =========================================================================

    @Test
    @Order(19)
    @DisplayName("deleteAuth – token is removed")
    void deleteAuthPositive() throws DataAccessException {
        dao.createAuth(new AuthData("temp-token", "jack"));
        dao.deleteAuth("temp-token");
        assertNull(dao.getAuth("temp-token"), "Auth token should be null after deletion");
    }

    @Test
    @Order(20)
    @DisplayName("deleteAuth – deleting non-existent token does not throw")
    void deleteAuthNonExistentNoThrow() {
        assertDoesNotThrow(() -> dao.deleteAuth("no-such-token"),
                "Deleting a non-existent auth token should not throw an exception");
    }
}
