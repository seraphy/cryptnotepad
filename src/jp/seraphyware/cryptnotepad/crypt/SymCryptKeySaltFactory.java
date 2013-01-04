package jp.seraphyware.cryptnotepad.crypt;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * パスフレーズによる対象キー生成用のソルトを生成するファクトリクラス.
 * 
 * @author seraphy
 */
public class SymCryptKeySaltFactory {

    /**
     * ロガー.<br>
     */
    private static final Logger logger = Logger
            .getLogger(SymCryptKeySaltFactory.class.getName());

    /**
     * ファイルのハッシュ計算用
     */
    private CalcurateFileHash calcFileHash = new CalcurateFileHash();

    /**
     * デフォルトのソルトを取得する.
     * 
     * @return ソルト
     */
    private static byte[] getDefaultSalt() {
        return new byte[] { -69, 104, 126, 20, 13, 45, -105, -116 };
    }

    /**
     * ファイル名またはURLを指定して、その対応するソルトを取得する.<br>
     * nullまたは空文字の場合はデフォルトのソルトを用いる.<br>
     * 
     * @param fileName
     *            ファイル名またはURL、もしくはnullまたは空文字
     * @return ソルト(必ず8バイト以上が返されます.)
     */
    public SymCryptKeySaltProvider getSaltProvider(String fileName) {
        byte[] salt = null;
        try {
            // ファイルのSHA512を計算する.
            salt = calcFileHash.getFileHash(fileName);

        } catch (Exception ex) {
            // デバッグ用にソルト値計算失敗を記録するが、
            // 本番時には記録しないほうが望ましいと思われる.
            logger.log(Level.FINE, "fileHashError: " + ex, ex);
        }

        if (salt == null) {
            // ファイルの指定がないか、もしくは
            // ハッシュ値の計算に失敗した場合は、デフォルトのソルトを用いる.
            salt = getDefaultSalt();
        }

        final byte[] resultSalt = salt;
        return new SymCryptKeySaltProvider() {
            @Override
            public byte[] getSalt() {
                return resultSalt;
            }
        };
    }
}
