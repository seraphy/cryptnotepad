package jp.seraphyware.cryptnotepad.crypt;

import java.util.EventListener;

/**
 * SymCipherのインベトハンドラ.<br>
 * 
 * @author seraphy
 */
public interface SymCipherEventListener extends EventListener {

    /**
     * 暗号化解除前に呼び出されます.<br>
     * 
     * @param e
     */
    void preEncryption(SymCipherEvent e);

    /**
     * 暗号化前に呼び出されます.<br>
     * 
     * @param e
     */
    void preDecryption(SymCipherEvent e);
}
