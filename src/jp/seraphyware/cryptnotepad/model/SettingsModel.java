package jp.seraphyware.cryptnotepad.model;

import java.io.Serializable;
import java.util.Arrays;

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
            throw new IllegalArgumentException();
        }
        this.encoding = encoding.trim();
    }

    public void setKeyFile(String keyFile) {
        this.keyFile = keyFile;
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
