package cz.anty.purkynka.exceptions;

import java.io.IOException;

/**
 * Created by anty on 6/21/17.
 *
 * @author anty
 */

public class NotLoggedInException extends IOException {

    public NotLoggedInException() {
        super();
    }

    public NotLoggedInException(String detailMessage) {
        super(detailMessage);
    }
}
