package jp.seraphyware.cryptnotepad.crypt;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.crypto.SecretKey;
import javax.swing.event.EventListenerList;

/**
 * パスフレーズとファイル名を指定して、暗号化・復号化する.
 * 
 * @author seraphy
 */
public class SymCipher {

    /**
     * イベントリスナのリスト
     */
    private final EventListenerList listeners = new EventListenerList();

    /**
     * ソルトのファクトリ
     */
    private SymCryptKeySaltFactory keySaltFactory;

    /**
     * キーのファクトリ
     */
    private SymCryptKeyFactory keyFactory;

    /**
     * パスフレーズとソルトファイルのソース
     */
    private SymCryptKeySource keySource;

    /**
     * コンストラクタ
     * 
     * @param keySource
     *            パスフレーズとソルトファイルのソース
     */
    public SymCipher(SymCryptKeySource keySource) {
        if (keySource == null) {
            throw new IllegalArgumentException();
        }
        this.keySource = keySource;

        keySaltFactory = new SymCryptKeySaltFactory();
        keyFactory = new SymCryptKeyFactory();

        // キーファイル変更によってソルトのキャッシュをクリアする.
        keySource.addPropertyChangeListener("keyFile",
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        keySaltFactory.clearCaches();
                    }
                });
    }

    /**
     * イベントリスナーを登録します.
     * 
     * @param l
     */
    public void addSymCipherEventListener(SymCipherEventListener l) {
        if (l != null) {
            listeners.add(SymCipherEventListener.class, l);
        }
    }

    /**
     * イベントリスナーを登録解除します.
     * 
     * @param l
     */
    public void removeSymCipherEventListener(SymCipherEventListener l) {
        if (l != null) {
            listeners.remove(SymCipherEventListener.class, l);
        }
    }

    /**
     * 暗号化前イベントの通知
     * 
     * @param e
     *            イベント
     */
    protected SymCipherEvent firePreEncryption(SymCipherEvent e) {
        for (SymCipherEventListener l : listeners
                .getListeners(SymCipherEventListener.class)) {
            l.preEncryption(e);
        }
        return e;
    }

    /**
     * 復号化前イベントの通知
     * 
     * @param e
     *            イベント
     */
    protected SymCipherEvent firePreDecryption(SymCipherEvent e) {
        for (SymCipherEventListener l : listeners
                .getListeners(SymCipherEventListener.class)) {
            l.preDecryption(e);
        }
        return e;
    }

    /**
     * 対称暗号化キーを生成する.
     * 
     * @return 対称暗号化キー
     */
    protected SecretKey createSecretKey() {
        char[] passphrase = keySource.getPassphrase();
        SymCryptKeySaltProvider saltProvider = keySaltFactory
                .getSaltProvider(keySource.getKeyFile());

        return keyFactory.createKey(passphrase, saltProvider);
    }

    /**
     * バイナリデータをファイルに暗号化して書き込む.
     * 
     * @param data
     *            暗号化するデータ
     * @param file
     *            書き込み先ファイル
     * @throws IOException
     *             失敗
     */
    public void encrypt(byte[] data, File file) throws IOException {
        if (data == null) {
            data = new byte[0];
        }
        if (file == null) {
            throw new IllegalArgumentException();
        }

        if (firePreEncryption(new SymCipherEvent(this)).isCancel()) {
            throw new CipherCancelException();
        }

        SecretKey skey = createSecretKey();

        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        OutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        try {
            CryptUtils.encrypt(skey, bis, bos);

        } finally {
            bos.close();
        }
    }

    /**
     * 暗号化されたファイルを指定してバイナリデータを復元する. ファイルがなければ空のデータを返す.
     * 
     * @param file
     *            暗号化されたファイル
     * @return データ
     * @throws IOException
     *             失敗
     */
    public byte[] decrypt(File file) throws IOException {
        if (file == null || !file.exists()) {
            return null;
        }

        if (firePreDecryption(new SymCipherEvent(this)).isCancel()) {
            throw new CipherCancelException();
        }

        SecretKey skey = createSecretKey();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        InputStream bis = new BufferedInputStream(new FileInputStream(file));
        try {
            CryptUtils.decrypt(skey, bis, bos);

        } finally {
            bis.close();
        }

        return bos.toByteArray();
    }

    /**
     * ファイルを安全に削除する. すでにファイルが存在しなければ何もしない.
     * 
     * @param file
     *            削除するファイル.
     * @throws IOException
     */
    public void delete(File file) throws IOException {
        if (file == null || !file.exists()) {
            return;
        }
        CryptUtils.erase(file);
    }
}
