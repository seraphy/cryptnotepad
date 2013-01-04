package jp.seraphyware.cryptnotepad.crypt;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ファイルのハッシュ値(SHA512)を計算する.
 * 
 * @author seraphy
 */
public class CalcurateFileHash {

    /**
     * ロガー.<br>
     */
    private static final Logger logger = Logger
            .getLogger(CalcurateFileHash.class.getName());

    /**
     * バッファサイズ
     */
    private int bufsiz = 16 * 1024; // 16kb

    /**
     * バッファサイズを指定する.
     * 
     * @param bufsiz
     */
    public void setBufferSize(int bufsiz) {
        if (bufsiz < 1) {
            throw new IllegalArgumentException();
        }
        this.bufsiz = bufsiz;
    }

    public int getBufferSize() {
        return bufsiz;
    }

    /**
     * ファイル名またはURLを示す文字列を指定して、そのファイルのハッシュ値を求める. nullまたは空の場合はnullを返す.
     * 
     * @param fileName
     *            ファイル名またはURL
     * @return ハッシュ値またはnull
     * @throws IOException
     */
    public byte[] getFileHash(String fileName) throws IOException {
        if (fileName == null || fileName.trim().length() == 0) {
            return null;
        }

        URL url;
        if (fileName.startsWith("http:") || fileName.startsWith("https:")
                || fileName.startsWith("file:")) {
            url = new URL(fileName);

        } else {
            File file = new File(fileName);
            url = file.toURI().toURL();
        }

        return getFileHash(url);
    }

    /**
     * URLを指定して、そのファイルのハッシュ値を求める. nullの場合はnullを返す.
     * 
     * @param url
     *            ファイル名またはURL
     * @return ハッシュ値またはnull
     * @throws IOException
     */
    public byte[] getFileHash(URL url) throws IOException {
        if (url == null) {
            return null;
        }

        URLConnection conn = openConnection(url);
        InputStream is = conn.getInputStream();
        try {
            return getFileHash(is);

        } finally {
            is.close();
        }
    }

    /**
     * 指定したURLに対するURLConnectionを開く. プロキシの設定があればプロキシ経由で開かれる.
     * 
     * @param url
     *            接続先
     * @return URLConnectionのインスタンス.
     * @throws IOException
     *             失敗した場合
     */
    protected URLConnection openConnection(URL url) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException();
        }
        try {
            Proxy proxy = getDefaultProxy(url.toURI());
            URLConnection conn;
            if (proxy == null) {
                // プロキシ指定指定なければ直接 (ファイルなど)
                conn = url.openConnection();
            } else {
                logger.log(Level.FINE, "use proxy=" + proxy);
                conn = url.openConnection(proxy);
            }
            return conn;

        } catch (URISyntaxException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * 指定したURIに対するプロキシ設定を取得する. システムプロパティ「java.net.useSystemProxies」がtrueであれば、
     * システム標準のプロキシが取得される. プロキシがなければDIRECTを返す. プロトコルがファイルなどの場合はnullを返す.
     * 
     * @param uri
     *            URI
     * @return プロキシ設定. なければnull
     */
    protected Proxy getDefaultProxy(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException();
        }
        String schema = uri.getScheme();
        if (!"http".equals(schema) && !"https".equals(schema)) {
            // http/https以外の場合はプロキシはない.
            return null;
        }
        try {
            ProxySelector proxySelector = ProxySelector.getDefault();
            List<Proxy> proxies = proxySelector.select(uri);
            if (proxies == null || proxies.size() == 0) {
                // プロキシなし
                return Proxy.NO_PROXY;
            }
            return proxies.get(0);

        } catch (Exception ex) {
            logger.log(Level.FINE, "keyFile error: " + ex, ex);
            return null;
        }
    }

    /**
     * 入力ストリームを指定して、そのファイルのハッシュ値を求める. nullの場合はnullを返す.
     * 
     * @param is
     *            入力ストリーム
     * @return ハッシュ値またはnull
     * @throws IOException
     */
    public byte[] getFileHash(InputStream is) throws IOException {
        if (is == null) {
            return null;
        }

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-512");

        } catch (NoSuchAlgorithmException ex) {
            // アルゴリズムがみつからない = 環境の問題
            throw new RuntimeException(ex);
        }

        // ストリームを読み取りハッシュを計算する.
        BufferedInputStream bis = new BufferedInputStream(is, getBufferSize());
        try {
            byte[] buf = new byte[getBufferSize()];
            for (;;) {
                int rd = bis.read(buf);
                if (rd < 0) {
                    break;
                }
                digest.update(buf, 0, rd);
            }

        } finally {
            bis.close();
        }
        return digest.digest();
    }
}
