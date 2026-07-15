package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * MySQL-backed implementation of {@link DataAccess}.
 * On construction, the database and all required tables are created if they do not already exist.
 */
public class MySqlDataAccess implements DataAccess {

    private static final Gson GSON = new Gson();

    private static final String[] CREATE_STATEMENTS = {
            """
            CREATE TABLE IF NOT EXISTS users (
              username VARCHAR(256) NOT NULL,
              password VARCHAR(256) NOT NULL,
              email    VARCHAR(256) NOT NULL,
              PRIMARY KEY (username)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS games (
              gameID    INT          NOT NULL AUTO_INCREMENT,
              whiteUser VARCHAR(256),
              blackUser VARCHAR(256),
              gameName  VARCHAR(256) NOT NULL,
              game      TEXT         NOT NULL,
              PRIMARY KEY (gameID)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS auth_tokens (
              authToken VARCHAR(256) NOT NULL,
              username  VARCHAR(256) NOT NULL,
              PRIMARY KEY (authToken)
            )
            """
    };

    public MySqlDataAccess() throws DataAccessException {
        configureDatabase();
    }

    private void configureDatabase() throws DataAccessException {
        DatabaseManager.createDatabase();
        try (var conn = DatabaseManager.getConnection()) {
            for (String statement : CREATE_STATEMENTS) {
                try (var ps = conn.prepareStatement(statement)) {
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to configure database: " + ex.getMessage(), ex);
        }
    }

    // -------------------------------------------------------------------------
    // Clear
    // -------------------------------------------------------------------------

    @Override
    public void clear() throws DataAccessException {
        String[] truncates = {"DELETE FROM auth_tokens", "DELETE FROM games", "DELETE FROM users"};
        try (var conn = DatabaseManager.getConnection()) {
            for (String sql : truncates) {
                try (var ps = conn.prepareStatement(sql)) {
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to clear database: " + ex.getMessage(), ex);
        }
    }

    // -------------------------------------------------------------------------
    // User
    // -------------------------------------------------------------------------

    @Override
    public void createUser(UserData user) throws DataAccessException {
        if (getUser(user.username()) != null) {
            throw new AlreadyTakenException("User already exists: " + user.username());
        }
        String hashedPassword = BCrypt.hashpw(user.password(), BCrypt.gensalt());
        String sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.username());
            ps.setString(2, hashedPassword);
            ps.setString(3, user.email());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to create user: " + ex.getMessage(), ex);
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        String sql = "SELECT username, password, email FROM users WHERE username = ?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserData(rs.getString("username"), rs.getString("password"), rs.getString("email"));
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to get user: " + ex.getMessage(), ex);
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Game
    // -------------------------------------------------------------------------

    @Override
    public int createGame(String gameName) throws DataAccessException {
        if (gameName == null) {
            throw new DataAccessException("game name cannot be null");
        }
        String gameJson = GSON.toJson(new ChessGame());
        String sql = "INSERT INTO games (whiteUser, blackUser, gameName, game) VALUES (NULL, NULL, ?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, gameName);
            ps.setString(2, gameJson);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to create game: " + ex.getMessage(), ex);
        }
        throw new DataAccessException("failed to retrieve generated game ID");
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        String sql = "SELECT gameID, whiteUser, blackUser, gameName, game FROM games WHERE gameID = ?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, gameID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return readGame(rs);
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to get game: " + ex.getMessage(), ex);
        }
        return null;
    }

    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        var games = new ArrayList<GameData>();
        String sql = "SELECT gameID, whiteUser, blackUser, gameName, game FROM games";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                games.add(readGame(rs));
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to list games: " + ex.getMessage(), ex);
        }
        return games;
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        if (getGame(game.gameID()) == null) {
            throw new DataAccessException("Game not found: " + game.gameID());
        }
        String gameJson = GSON.toJson(game.game());
        String sql = "UPDATE games SET whiteUser = ?, blackUser = ?, gameName = ?, game = ? WHERE gameID = ?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, game.whiteUsername());
            ps.setString(2, game.blackUsername());
            ps.setString(3, game.gameName());
            ps.setString(4, gameJson);
            ps.setInt(5, game.gameID());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to update game: " + ex.getMessage(), ex);
        }
    }

    private GameData readGame(ResultSet rs) throws SQLException {
        int id = rs.getInt("gameID");
        String white = rs.getString("whiteUser");
        String black = rs.getString("blackUser");
        String name = rs.getString("gameName");
        String json = rs.getString("game");
        ChessGame chessGame = GSON.fromJson(json, ChessGame.class);
        return new GameData(id, white, black, name, chessGame);
    }

    // -------------------------------------------------------------------------
    // Auth
    // -------------------------------------------------------------------------

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        String sql = "INSERT INTO auth_tokens (authToken, username) VALUES (?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, auth.authToken());
            ps.setString(2, auth.username());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to create auth token: " + ex.getMessage(), ex);
        }
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        String sql = "SELECT authToken, username FROM auth_tokens WHERE authToken = ?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, authToken);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new AuthData(rs.getString("authToken"), rs.getString("username"));
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to get auth token: " + ex.getMessage(), ex);
        }
        return null;
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        String sql = "DELETE FROM auth_tokens WHERE authToken = ?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, authToken);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to delete auth token: " + ex.getMessage(), ex);
        }
    }
}
