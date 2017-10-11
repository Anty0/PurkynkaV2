package cz.anty.purkynka.exceptions;

import java.net.ConnectException;

/**
 * Created by anty on 9.6.15.
 *
 * @author anty
 */
public class WrongLoginDataException extends ConnectException {

    public WrongLoginDataException() {
        super();
    }

    public WrongLoginDataException(String detailMessage) {
        super(detailMessage);
    }
}
