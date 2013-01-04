package jp.seraphyware.cryptnotepad.crypt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;


/**
 * 暗号化・複合化用のユーテリティ.<br>
 * 
 * @author seraphy
 */
public final class CryptUtils {
    
    /**
     * バッファサイズ.<br>
     */
    private static int bufferSize = 16 * 1024;

    /**
     * プライベートコンストラクタ
     */
    private CryptUtils() {
        super();
    }
    
    public static int getBufferSize() {
        return bufferSize;
    }
    
    public static void setBufferSize(int bufferSize) {
        CryptUtils.bufferSize = bufferSize;
    }
    
    /**
     * 暗号化する.
     * @param skey 対称暗号化キー
     * @param is 暗号化するデータの入力元
     * @param os 暗号化したデータの出力先
     * @throws IOException 失敗
     */
    public static void encrypt(SecretKey skey, InputStream is, OutputStream os)
            throws IOException {
        if (is == null || os == null || skey == null) {
            throw new IllegalArgumentException();
        }

        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skey);

        } catch (GeneralSecurityException ex) {
            throw new RuntimeException(ex);
        }

        // イニシャルベクターを出力する.
        // IVはブロックサイズと等しく、AESのブロックサイズは128bit(16bytes)
        byte[] iv = cipher.getIV();
        os.write(iv);
        
        // 入力ストリームから読み込んで暗号化し出力ストリームに転送する.
        transfer(cipher, is, os);
    }

    /**
     * 復号化する.
     * @param skey 対称暗号化キー
     * @param is 暗号化されたデータの入力元
     * @param os 復号化されたデータの出力先
     * @throws IOException 失敗
     */
    public static void decrypt(SecretKey skey, InputStream is, OutputStream os)
            throws IOException {
        if (is == null || os == null || skey == null) {
            throw new IllegalArgumentException();
        }

        // IVを読み込む (AESのブロックサイズと等しく、128Bit)
        byte[] iv = new byte[16]; // 128bit
        int ivSize = is.read(iv);
        if (iv.length != ivSize) {
            throw new IOException("invalid data.");
        }
        
        // 復号化準備
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skey, new IvParameterSpec(iv));

        } catch (GeneralSecurityException ex) {
            throw new RuntimeException(ex);
        }
        
        // 入力ストリームから読み込んで復号化し出力ストリームに転送する.
        transfer(cipher, is, os);
    }
    
    /**
     * 暗号化・復号化器に対して入力ストリームの内容を入れて、その結果を出力ストリームに転送する.
     * @param cipher 暗号化・復号化器
     * @param is 入力ストリーム
     * @param os 出力ストリーム
     * @throws IOException
     */
    public static void transfer(Cipher cipher, InputStream is, OutputStream os)
            throws IOException {
        if (cipher == null || is == null || os == null) {
            throw new IllegalArgumentException();
        }
        // 入力ストリームから読み込んで暗号/復号化し出力ストリームに転送する.
        byte[] buf = new byte[getBufferSize()];
        for (;;) {
            int rd = is.read(buf);
            if (rd < 0) {
                // 終端処理
                try {
                    byte[] encbuf = cipher.doFinal();
                    os.write(encbuf);

                } catch (GeneralSecurityException ex) {
                    throw new IOException(ex);
                }
                break;
            }

            // 暗号/複合化して出力する.
            byte[] encbuf = cipher.update(buf, 0, rd);
            os.write(encbuf);
        }
    }
    
    /**
     * ファイルを消去する.<br>
     * 
     * @param file ファイル
     * @throws IOException 失敗
     */
    public static void erase(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException();
        }
        
        // ランダムバッファ作成
        SecureRandom rng = new SecureRandom();
        int bufsiz = getBufferSize();
        byte[] buf = new byte[bufsiz];
        
        // 現在のファイルの内容をランダム値で上書きする.
        // (バッファなしモード = ただちに書き込み)
        RandomAccessFile fh = new RandomAccessFile(file, "rws");
        try {
            long filelen = fh.length();
            long pos = 0;
            while (pos < filelen) {
                // ランダム値を生成する.
                rng.nextBytes(buf);

                // 書き込みサイズの算定.
                long outlen = filelen - pos;
                if (bufsiz < outlen) {
                    outlen = bufsiz;
                }

                // 書き込み
                fh.write(buf, 0, (int)outlen);
                pos += outlen;
            }
            
        } finally {
            fh.close();
        }
        
        // ファイルエントリの削除
        file.delete();
    }
}
