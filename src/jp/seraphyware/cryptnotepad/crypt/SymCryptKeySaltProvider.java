package jp.seraphyware.cryptnotepad.crypt;

/**
 * パスフレーズによる対象キー生成用のソルトを提供する.
 * 
 * @author seraphy
 */
public interface SymCryptKeySaltProvider {

    /**
     * ソルト.<br>
     * 必ず非nullで、且つ、8バイト以上となる.<br>
     * 
     * @return ソルト用配列
     */
    byte[] getSalt();

}
