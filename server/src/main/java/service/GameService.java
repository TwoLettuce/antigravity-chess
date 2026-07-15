package service;

import dataaccess.*;
import model.AuthData;
import model.GameData;

import java.util.Collection;

public class GameService {
    private final DataAccess dataAccess;

    public GameService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    private AuthData authenticate(String authToken) throws DataAccessException, UnauthorizedException {
        if (authToken == null || authToken.isBlank()) {
            throw new UnauthorizedException("Missing auth token");
        }
        AuthData auth = dataAccess.getAuth(authToken);
        if (auth == null) {
            throw new UnauthorizedException("Invalid auth token");
        }
        return auth;
    }

    public Collection<GameData> listGames(String authToken)
            throws DataAccessException, UnauthorizedException {
        authenticate(authToken);
        return dataAccess.listGames();
    }

    public int createGame(String authToken, String gameName)
            throws DataAccessException, BadRequestException, UnauthorizedException {
        authenticate(authToken);
        if (gameName == null || gameName.isBlank()) {
            throw new BadRequestException("Game name is required");
        }
        return dataAccess.createGame(gameName);
    }

    public void joinGame(String authToken, String playerColor, Integer gameID)
            throws DataAccessException, BadRequestException, UnauthorizedException, AlreadyTakenException {
        AuthData auth = authenticate(authToken);

        if (gameID == null) {
            throw new BadRequestException("Game ID is required");
        }

        // Validate playerColor is exactly WHITE or BLACK
        if (playerColor == null || (!playerColor.equals("WHITE") && !playerColor.equals("BLACK"))) {
            throw new BadRequestException("Invalid player color: must be WHITE or BLACK");
        }

        GameData game = dataAccess.getGame(gameID);
        if (game == null) {
            throw new BadRequestException("Game not found");
        }

        String username = auth.username();
        GameData updated;
        if (playerColor.equals("WHITE")) {
            if (game.whiteUsername() != null) {
                throw new AlreadyTakenException("White spot is already taken");
            }
            updated = new GameData(game.gameID(), username, game.blackUsername(), game.gameName(), game.game());
        } else {
            if (game.blackUsername() != null) {
                throw new AlreadyTakenException("Black spot is already taken");
            }
            updated = new GameData(game.gameID(), game.whiteUsername(), username, game.gameName(), game.game());
        }
        dataAccess.updateGame(updated);
    }
}
