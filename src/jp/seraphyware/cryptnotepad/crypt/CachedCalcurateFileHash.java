package jp.seraphyware.cryptnotepad.crypt;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ファイルのハッシュ値計算をキャッシュする拡張.
 * 
 * @author seraphy
 */
public class CachedCalcurateFileHash extends CalcurateFileHash {

    /**
     * ロガー.<br>
     */
    private static final Logger logger = Logger
            .getLogger(CachedCalcurateFileHash.class.getName());

    /**
     * ファイルに対するハッシュ計算結果を保持するクラス.
     */
    private static class Result {

        /**
         * ファイルの最終更新日
         */
        private long lastModified;

        /**
         * ハッシュ値
         */
        private byte[] hash;

        public byte[] getHash() {
            return hash;
        }

        public long getLastModified() {
            return lastModified;
        }

        public void setHash(byte[] hash) {
            this.hash = hash;
        }

        public void setLastModified(long lastModified) {
            this.lastModified = lastModified;
        }
    }

    /**
     * URIに対するハッシュの計算結果を保持する.
     */
    private HashMap<URI, Result> cache = new HashMap<URI, Result>();

    /*
     * キャッシュするよう拡張している.
     */
    @Override
    public byte[] getFileHash(URL url) throws IOException {
        if (url == null) {
            return null;
        }

        // URLからURIに変換する.
        // ※ URLをキーに設定すると、equalsの呼び出しのたびに
        // ネットワークアクセスが発生する可能性があるため、いったん、URIに変換してキーとする.
        URI uri;
        try {
            uri = url.toURI();

        } catch (URISyntaxException ex) {
            logger.log(Level.FINE, "uri-error: " + ex, ex);
            throw new IOException("uri-error: " + ex, ex);
        }

        // キャッシュを検索する.
        boolean modified = false;
        Result result = cache.get(uri);
        if (result == null) {
            // 該当なければキャッシュを作成する.
            result = new Result();
            cache.put(uri, result);

            // 新規の場合は変更あり
            modified = true;
        }

        // コンテンツの更新を確認する.
        URLConnection conn = openConnection(url);

        long lastModified = conn.getLastModified();
        if (!modified) {
            // 最終更新日を比較して変更の有無を見る.
            modified = (lastModified != result.getLastModified());
        }

        result.setLastModified(lastModified);

        if (modified) {
            // 新規もしくは変更がある場合は、ロードを試行する.
            byte[] hash;
            InputStream is = conn.getInputStream();
            try {
                hash = getFileHash(is);
            } finally {
                is.close();
            }

            // ハッシュ値を設定する.
            result.setHash(hash);
        }

        return result.getHash();
    }

    /**
     * キャッシュをクリアする.
     */
    public void clear() {
        cache.clear();
    }
}