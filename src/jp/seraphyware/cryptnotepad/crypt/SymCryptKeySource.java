package jp.seraphyware.cryptnotepad.crypt;

import java.beans.PropertyChangeListener;

/**
 * 共通暗号化キー生成のデータ元.
 * 
 * @author seraphy
 */
public interface SymCryptKeySource {

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);

    void addPropertyChangeListener(String propertyName,
            PropertyChangeListener listener);

    void removePropertyChangeListener(String propertyName,
            PropertyChangeListener listener);

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
