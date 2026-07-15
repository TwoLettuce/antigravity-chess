package service;

import dataaccess.*;
import model.AuthData;
import model.UserData;

import java.util.UUID;

public class UserService {
    private final DataAccess dataAccess;

    public UserService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public AuthData register(String username, String password, String email)
            throws DataAccessException, BadRequestException, AlreadyTakenException {
        if (username == null || username.isBlank() ||
            password == null || password.isBlank() ||
            email == null || email.isBlank()) {
            throw new BadRequestException("Missing required fields");
        }

        if (dataAccess.getUser(username) != null) {
            throw new AlreadyTakenException("Username already taken");
        }

        dataAccess.createUser(new UserData(username, password, email));
        String token = UUID.randomUUID().toString();
        AuthData auth = new AuthData(token, username);
        dataAccess.createAuth(auth);
        return auth;
    }

    public AuthData login(String username, String password)
            throws DataAccessException, BadRequestException, UnauthorizedException {
        if (username == null || username.isBlank() ||
            password == null || password.isBlank()) {
            throw new BadRequestException("Missing username or password");
        }

        UserData user = dataAccess.getUser(username);
        if (user == null || !user.password().equals(password)) {
            throw new UnauthorizedException("Invalid username or password");
        }

        String token = UUID.randomUUID().toString();
        AuthData auth = new AuthData(token, username);
        dataAccess.createAuth(auth);
        return auth;
    }

    public void logout(String authToken)
            throws DataAccessException, UnauthorizedException {
        AuthData auth = dataAccess.getAuth(authToken);
        if (auth == null) {
            throw new UnauthorizedException("Invalid auth token");
        }
        dataAccess.deleteAuth(authToken);
    }
}
