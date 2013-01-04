package jp.seraphyware.cryptnotepad.crypt;

/**
 * 共通暗号化キー生成のデータ元.
 * 
 * @author seraphy
 */
public interface SymCryptKeySource {

    /**
     * キーファイル
     * 
     * @return
     */
    String getKeyFile();

    /**
     * パスフレーズ
     * 
     * @return
     */
    char[] getPassphrase();
    
    /**
     * 有効であるか?
     * @return 有効であればtrue
     */
    boolean isValid();
}
