package jp.seraphyware.cryptnotepad.crypt;

import java.io.IOException;

/**
 * 暗号化・復号化がキャンセルされたことを示す例外.<br>
 * 
 * @author seraphy
 */
public class CipherCancelException extends IOException {

    private static final long serialVersionUID = 6152502962451548056L;

    public CipherCancelException() {
        super();
    }

    public CipherCancelException(String msg) {
        super(msg);
    }

    public CipherCancelException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public CipherCancelException(Throwable cause) {
        super(cause);
    }
}
