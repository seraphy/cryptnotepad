package jp.seraphyware.cryptnotepad.crypt;

import java.security.GeneralSecurityException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * パスワードベース(PBKDF2WithHmacSHA1)を用いて、
 * AES用の対称暗号化キーを生成する.<br>
 * 
 * @author seraphy
 */
public class SymCryptKeyFactory {
    
    /**
     * 生成するキーのビット長.<br>
     * AES暗号では鍵長は128ビット・192ビット・256ビットのいずれか.<br>
     * ただし、128より大きなキー長はポリシーファイルの書き換えが必要.<br>
     * http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html
     */
    private int keySize = 128;
    
    /**
     * パスワード生成のための繰り返し回数.<br>
     * 最低でも1000回以上繰り返すことが望ましい.<br>
     */
    private int iterationCount = 45522;
    
    public int getKeySize() {
        return keySize;
    }

    public void setKeySize(int keySize) {
        if (keySize <= 0) {
            throw new IllegalArgumentException();
        }
        this.keySize = keySize;
    }
    
    public int getIterationCount() {
        return iterationCount;
    }

    public void setIterationCount(int iterationCount) {
        if (iterationCount <= 0) {
            throw new IllegalArgumentException();
        }
        this.iterationCount = iterationCount;
    }
    
    /**
     * パスフレーズとソルトプロバイダを指定して対称暗号化キーを生成する.
     * @param passphrase パスフレーズ
     * @param saltProvider ソルトプロバイダ
     * @return 対称暗号化キー
     */
    public SecretKey createKey(char[] passphrase, SymCryptKeySaltProvider saltProvider) {
        if (passphrase == null || passphrase.length == 0 || saltProvider == null) {
            throw new IllegalArgumentException("パスフレーズまたはソルトの指定がありません.");
        }

        try {
            byte[] salt = saltProvider.getSalt();
            SecretKeyFactory factory = SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA1");

            // PBKDF2WithHmacSHA1でキーを生成する.
            KeySpec spec = new PBEKeySpec(passphrase, salt,
                    getIterationCount(), getKeySize());
            SecretKey tmp = factory.generateSecret(spec);

            // 生成されたキーをAES用に変換しなおす.
            return new SecretKeySpec(tmp.getEncoded(), "AES");

        } catch (GeneralSecurityException ex) {
            // キー生成やアルゴリズムが選択できない場合は、環境の問題である.
            throw new RuntimeException(ex);
        }
    }   
}
