package server;

import com.google.gson.Gson;
import dataaccess.*;
import io.javalin.*;
import io.javalin.http.Context;
import model.AuthData;
import model.GameData;
import service.*;

import java.util.Collection;
import java.util.Map;

public class Server {

    private final Javalin javalin;
    private final DataAccess dataAccess;
    private final ClearService clearService;
    private final UserService userService;
    private final GameService gameService;
    private final Gson gson = new Gson();

    public Server() {
        DataAccess dataAccess;
        try {
            dataAccess = new MySqlDataAccess();
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to initialize database: " + e.getMessage(), e);
        }
        this.dataAccess = dataAccess;
        clearService = new ClearService(dataAccess);
        userService = new UserService(dataAccess);
        gameService = new GameService(dataAccess);

        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        // Register your endpoints and exception handlers here.
        javalin.delete("/db", this::clearHandler);
        javalin.post("/user", this::registerHandler);
        javalin.post("/session", this::loginHandler);
        javalin.delete("/session", this::logoutHandler);
        javalin.get("/game", this::listGamesHandler);
        javalin.post("/game", this::createGameHandler);
        javalin.put("/game", this::joinGameHandler);

        // Exception handlers
        javalin.exception(BadRequestException.class, (e, ctx) ->
                sendError(ctx, 400, e.getMessage()));
        javalin.exception(UnauthorizedException.class, (e, ctx) ->
                sendError(ctx, 401, e.getMessage()));
        javalin.exception(AlreadyTakenException.class, (e, ctx) ->
                sendError(ctx, 403, e.getMessage()));
        javalin.exception(DataAccessException.class, (e, ctx) ->
                sendError(ctx, 500, e.getMessage()));
        javalin.exception(Exception.class, (e, ctx) ->
                sendError(ctx, 500, e.getMessage()));
    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }

    // ---- Handler Methods ----

    private void clearHandler(Context ctx) throws DataAccessException {
        clearService.clear();
        ctx.json("{}");
    }

    private void registerHandler(Context ctx) throws Exception {
        var body = gson.fromJson(ctx.body(), UserRequest.class);
        AuthData auth = userService.register(
                body != null ? body.username : null,
                body != null ? body.password : null,
                body != null ? body.email : null);
        ctx.json(gson.toJson(Map.of("username", auth.username(), "authToken", auth.authToken())));
    }

    private void loginHandler(Context ctx) throws Exception {
        var body = gson.fromJson(ctx.body(), UserRequest.class);
        AuthData auth = userService.login(
                body != null ? body.username : null,
                body != null ? body.password : null);
        ctx.json(gson.toJson(Map.of("username", auth.username(), "authToken", auth.authToken())));
    }

    private void logoutHandler(Context ctx) throws Exception {
        String authToken = ctx.header("authorization");
        userService.logout(authToken);
        ctx.json("{}");
    }

    private void listGamesHandler(Context ctx) throws Exception {
        String authToken = ctx.header("authorization");
        Collection<GameData> games = gameService.listGames(authToken);
        ctx.json(gson.toJson(Map.of("games", games)));
    }

    private void createGameHandler(Context ctx) throws Exception {
        String authToken = ctx.header("authorization");
        var body = gson.fromJson(ctx.body(), GameRequest.class);
        int gameID = gameService.createGame(authToken, body != null ? body.gameName : null);
        ctx.json(gson.toJson(Map.of("gameID", gameID)));
    }

    private void joinGameHandler(Context ctx) throws Exception {
        String authToken = ctx.header("authorization");
        var body = gson.fromJson(ctx.body(), JoinRequest.class);
        gameService.joinGame(
                authToken,
                body != null ? body.playerColor : null,
                body != null ? body.gameID : null);
        ctx.json("{}");
    }

    private void sendError(Context ctx, int status, String message) {
        ctx.status(status);
        ctx.json(gson.toJson(Map.of("message", "Error: " + message)));
    }

    // ---- Inner request record types ----

    private static class UserRequest {
        String username;
        String password;
        String email;
    }

    private static class GameRequest {
        String gameName;
    }

    private static class JoinRequest {
        String playerColor;
        Integer gameID;
    }
}
