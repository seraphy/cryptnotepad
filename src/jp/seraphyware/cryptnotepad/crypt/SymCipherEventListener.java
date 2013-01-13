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
    
    /**
     * 例外発生時に例外をスローする前に呼び出されます.
     * イベントに対するキャンセルの設定は無視されます.
     * @param e
     */
    void preThrowException(SymCipherEvent e);
}
