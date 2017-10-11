package cz.anty.purkynka.exceptions;

import java.io.IOException;

/**
 * Created by anty on 6/21/17.
 *
 * @author anty
 */

public class LoginExpiredException extends IOException {

    public LoginExpiredException() {
        super();
    }

    public LoginExpiredException(String detailMessage) {
        super(detailMessage);
    }
}
