package br.jus.tjpi.agendatelefonica.exception;

public class UserNotInGroupException extends RuntimeException {

    public UserNotInGroupException(String message) {
        super(message);
    }
}
