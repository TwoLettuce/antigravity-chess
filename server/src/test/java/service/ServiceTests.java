package service;

import dataaccess.*;
import model.AuthData;
import model.GameData;
import org.junit.jupiter.api.*;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceTests {

    private static DataAccess dataAccess;
    private static ClearService clearService;
    private static UserService userService;
    private static GameService gameService;

    @BeforeEach
    public void setup() throws DataAccessException {
        dataAccess = new MemoryDataAccess();
        clearService = new ClearService(dataAccess);
        userService = new UserService(dataAccess);
        gameService = new GameService(dataAccess);
    }

    // ---- ClearService Tests ----

    @Test
    @Order(1)
    @DisplayName("Clear: success clears all data")
    public void clearSuccess() throws DataAccessException {
        userService.register("alice", "pass", "alice@test.com");
        clearService.clear();
        assertNull(dataAccess.getUser("alice"), "User should be cleared");
        assertTrue(dataAccess.listGames().isEmpty(), "Games should be cleared");
    }

    // ---- UserService: register ----

    @Test
    @Order(2)
    @DisplayName("Register: success returns authToken")
    public void registerSuccess() throws DataAccessException {
        AuthData auth = userService.register("alice", "pass", "alice@test.com");
        assertNotNull(auth);
        assertEquals("alice", auth.username());
        assertNotNull(auth.authToken());
    }

    @Test
    @Order(3)
    @DisplayName("Register: duplicate username throws AlreadyTakenException")
    public void registerDuplicateUser() throws DataAccessException {
        userService.register("alice", "pass", "alice@test.com");
        assertThrows(AlreadyTakenException.class,
                () -> userService.register("alice", "pass2", "alice2@test.com"),
                "Should throw AlreadyTakenException for duplicate username");
    }

    @Test
    @Order(4)
    @DisplayName("Register: missing username throws BadRequestException")
    public void registerMissingFields() {
        assertThrows(BadRequestException.class,
                () -> userService.register(null, "pass", "a@a.com"),
                "Should throw BadRequestException when username is missing");
        assertThrows(BadRequestException.class,
                () -> userService.register("alice", null, "a@a.com"),
                "Should throw BadRequestException when password is missing");
        assertThrows(BadRequestException.class,
                () -> userService.register("alice", "pass", null),
                "Should throw BadRequestException when email is missing");
    }

    // ---- UserService: login ----

    @Test
    @Order(5)
    @DisplayName("Login: success returns new authToken")
    public void loginSuccess() throws DataAccessException {
        userService.register("alice", "pass", "alice@test.com");
        AuthData auth = userService.login("alice", "pass");
        assertNotNull(auth);
        assertEquals("alice", auth.username());
        assertNotNull(auth.authToken());
    }

    @Test
    @Order(6)
    @DisplayName("Login: wrong password throws UnauthorizedException")
    public void loginWrongPassword() throws DataAccessException {
        userService.register("alice", "pass", "alice@test.com");
        assertThrows(UnauthorizedException.class,
                () -> userService.login("alice", "wrongpass"),
                "Should throw UnauthorizedException for wrong password");
    }

    @Test
    @Order(7)
    @DisplayName("Login: non-existent user throws UnauthorizedException")
    public void loginNonExistentUser() {
        assertThrows(UnauthorizedException.class,
                () -> userService.login("nobody", "pass"),
                "Should throw UnauthorizedException for non-existent user");
    }

    // ---- UserService: logout ----

    @Test
    @Order(8)
    @DisplayName("Logout: success removes authToken")
    public void logoutSuccess() throws DataAccessException {
        AuthData auth = userService.register("alice", "pass", "alice@test.com");
        userService.logout(auth.authToken());
        assertNull(dataAccess.getAuth(auth.authToken()), "AuthToken should be deleted after logout");
    }

    @Test
    @Order(9)
    @DisplayName("Logout: invalid token throws UnauthorizedException")
    public void logoutInvalidToken() {
        assertThrows(UnauthorizedException.class,
                () -> userService.logout("bad-token"),
                "Should throw UnauthorizedException for invalid authToken");
    }

    // ---- GameService: listGames ----

    @Test
    @Order(10)
    @DisplayName("ListGames: success returns empty list initially")
    public void listGamesEmpty() throws DataAccessException {
        AuthData auth = userService.register("alice", "pass", "alice@test.com");
        Collection<GameData> games = gameService.listGames(auth.authToken());
        assertNotNull(games);
        assertTrue(games.isEmpty(), "Game list should be empty initially");
    }

    @Test
    @Order(11)
    @DisplayName("ListGames: unauthorized token throws UnauthorizedException")
    public void listGamesUnauthorized() {
        assertThrows(UnauthorizedException.class,
                () -> gameService.listGames("invalid-token"),
                "Should throw UnauthorizedException for bad token");
    }

    // ---- GameService: createGame ----

    @Test
    @Order(12)
    @DisplayName("CreateGame: success returns positive game ID")
    public void createGameSuccess() throws DataAccessException {
        AuthData auth = userService.register("alice", "pass", "alice@test.com");
        int gameID = gameService.createGame(auth.authToken(), "MyGame");
        assertTrue(gameID > 0, "Game ID should be positive");
        assertNotNull(dataAccess.getGame(gameID), "Created game should exist in data store");
    }

    @Test
    @Order(13)
    @DisplayName("CreateGame: null game name throws BadRequestException")
    public void createGameNullName() throws DataAccessException {
        AuthData auth = userService.register("alice", "pass", "alice@test.com");
        assertThrows(BadRequestException.class,
                () -> gameService.createGame(auth.authToken(), null),
                "Should throw BadRequestException for null game name");
    }

    @Test
    @Order(14)
    @DisplayName("CreateGame: unauthorized throws UnauthorizedException")
    public void createGameUnauthorized() {
        assertThrows(UnauthorizedException.class,
                () -> gameService.createGame("bad-token", "MyGame"),
                "Should throw UnauthorizedException for bad token");
    }

    // ---- GameService: joinGame ----

    @Test
    @Order(15)
    @DisplayName("JoinGame: success assigns player as white")
    public void joinGameSuccessWhite() throws DataAccessException {
        AuthData auth = userService.register("alice", "pass", "alice@test.com");
        int gameID = gameService.createGame(auth.authToken(), "JoinTest");
        gameService.joinGame(auth.authToken(), "WHITE", gameID);
        GameData game = dataAccess.getGame(gameID);
        assertEquals("alice", game.whiteUsername(), "White player should be alice");
        assertNull(game.blackUsername(), "Black player should be null");
    }

    @Test
    @Order(16)
    @DisplayName("JoinGame: steal color throws AlreadyTakenException")
    public void joinGameColorTaken() throws DataAccessException {
        AuthData authA = userService.register("alice", "pass", "alice@test.com");
        AuthData authB = userService.register("bob", "pass", "bob@test.com");
        int gameID = gameService.createGame(authA.authToken(), "TakenGame");
        gameService.joinGame(authA.authToken(), "WHITE", gameID);
        assertThrows(AlreadyTakenException.class,
                () -> gameService.joinGame(authB.authToken(), "WHITE", gameID),
                "Should throw AlreadyTakenException when color already taken");
    }

    @Test
    @Order(17)
    @DisplayName("JoinGame: invalid color throws BadRequestException")
    public void joinGameInvalidColor() throws DataAccessException {
        AuthData auth = userService.register("alice", "pass", "alice@test.com");
        int gameID = gameService.createGame(auth.authToken(), "ColorTest");
        assertThrows(BadRequestException.class,
                () -> gameService.joinGame(auth.authToken(), "GREEN", gameID),
                "Should throw BadRequestException for invalid color");
        assertThrows(BadRequestException.class,
                () -> gameService.joinGame(auth.authToken(), null, gameID),
                "Should throw BadRequestException for null color");
    }

    @Test
    @Order(18)
    @DisplayName("JoinGame: null gameID throws BadRequestException")
    public void joinGameNullId() throws DataAccessException {
        AuthData auth = userService.register("alice", "pass", "alice@test.com");
        assertThrows(BadRequestException.class,
                () -> gameService.joinGame(auth.authToken(), "WHITE", null),
                "Should throw BadRequestException for null game ID");
    }

    @Test
    @Order(19)
    @DisplayName("JoinGame: unauthorized throws UnauthorizedException")
    public void joinGameUnauthorized() throws DataAccessException {
        AuthData auth = userService.register("alice", "pass", "alice@test.com");
        int gameID = gameService.createGame(auth.authToken(), "AuthTest");
        assertThrows(UnauthorizedException.class,
                () -> gameService.joinGame("bad-token", "WHITE", gameID),
                "Should throw UnauthorizedException for bad auth token");
    }
}
