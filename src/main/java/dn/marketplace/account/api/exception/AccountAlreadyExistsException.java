package dn.marketplace.account.api.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AccountAlreadyExistsException extends  RuntimeException {

    public AccountAlreadyExistsException(String message) {
        super(message);
    }

    public AccountAlreadyExistsException() {
    }
}
