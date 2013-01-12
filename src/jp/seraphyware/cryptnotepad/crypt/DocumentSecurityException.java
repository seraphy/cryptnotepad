package jp.seraphyware.cryptnotepad.crypt;

import java.io.IOException;

/**
 * パスフレーズが一致しないかドキュメントが破損している場合の例外
 * 
 * @author seraphy
 */
public class DocumentSecurityException extends IOException {

    private static final long serialVersionUID = 659066980514660275L;

    public DocumentSecurityException() {
        super();
    }

    public DocumentSecurityException(String msg) {
        super(msg);
    }

    public DocumentSecurityException(String msg, Throwable ex) {
        super(msg, ex);
    }

    public DocumentSecurityException(Throwable ex) {
        super(ex);
    }
}
