package jp.seraphyware.cryptnotepad.model;

import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.Arrays;

import javax.swing.event.SwingPropertyChangeSupport;

import jp.seraphyware.cryptnotepad.crypt.SymCryptKeySource;

/**
 * キーファイルとパスフレーズ、文字コードなどの設定情報を保持するモデル.
 * 
 * @author seraphy
 */
public class SettingsModel implements Serializable, SymCryptKeySource {

    private static final long serialVersionUID = -1494835954166827007L;

    /**
     * デフォルトの文字コード
     */
    private static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * プロパティ変更サポート
     */
    private SwingPropertyChangeSupport propChange = new SwingPropertyChangeSupport(
            this);

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propChange.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propChange.removePropertyChangeListener(listener);
    }

    @Override
    public void addPropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        propChange.addPropertyChangeListener(propertyName, listener);
    }

    @Override
    public void removePropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        propChange.removePropertyChangeListener(propertyName, listener);
    }

    /**
     * 文字コード
     */
    private String encoding = DEFAULT_ENCODING;

    /**
     * キーファイル
     */
    private String keyFile;

    /**
     * パスフレーズ
     */
    private volatile char[] passphrase = new char[0];

    public String getEncoding() {
        return encoding;
    }

    @Override
    public String getKeyFile() {
        return keyFile;
    }

    @Override
    public char[] getPassphrase() {
        return passphrase;
    }

    /*
     * 有効であるか?
     */
    @Override
    public boolean isValid() {
        return passphrase != null && passphrase.length > 0;
    }

    public void setEncoding(String encoding) {
        if (encoding == null || encoding.trim().length() == 0) {
            encoding = DEFAULT_ENCODING;
        }
        String oldValue = this.encoding;
        this.encoding = encoding.trim();

        propChange.firePropertyChange("encoding", oldValue, this.encoding);
    }

    public void setKeyFile(String keyFile) {
        String oldValue = this.keyFile;
        this.keyFile = keyFile;

        propChange.firePropertyChange("keyFile", oldValue, keyFile);
    }

    /**
     * パスフレーズを設定する.
     * 
     * @param passphrase
     *            パスフレーズ、nullは空とみなす.
     */
    public void setPassphrase(char[] passphrase) {
        if (passphrase == null) {
            passphrase = new char[0];
        }

        // メモリ上からパスフレーズを消し去る
        Arrays.fill(this.passphrase, '@');

        this.passphrase = passphrase;

        // パスフレーズ変更を通知する.
        // (実際の内容はイベントに含めない.)
        propChange.firePropertyChange("passphrase", null, "*");
    }

    /**
     * 保持しているデータを消去する.
     */
    public void clear() {
        setEncoding(DEFAULT_ENCODING);
        setKeyFile(null);
        setPassphrase(null);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(getClass().getSimpleName());
        buf.append("@");
        buf.append(Integer.toHexString(System.identityHashCode(this)));
        buf.append("(encoding=").append(encoding);
        buf.append(", keyFile=").append(keyFile);
        buf.append(", passphrase=").append(passphrase.length > 0 ? "***" : "");
        buf.append(")");
        return buf.toString();
    }
}
