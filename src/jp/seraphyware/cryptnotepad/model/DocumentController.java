package jp.seraphyware.cryptnotepad.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import jp.seraphyware.cryptnotepad.crypt.SymCipher;

/**
 * ドキュメントの制御クラス.
 * 
 * @author seraphy
 */
public class DocumentController {

    /**
     * ロガー.<br>
     */
    private static final Logger logger = Logger
            .getLogger(DocumentController.class.getName());

    /**
     * 設定モデル
     */
    private SettingsModel settingsModel;

    /**
     * 暗号化
     */
    private SymCipher symCipher;

    /**
     * コンストラクタ
     */
    public DocumentController() {
        // 設定モデルインスタンスの構築
        settingsModel = new SettingsModel();

        // 暗号化・復号化器の構築
        symCipher = new SymCipher(settingsModel);
    }

    public void dispose() {
        settingsModel.clear();
    }

    public SettingsModel getSettingsModel() {
        return settingsModel;
    }

    public SymCipher getSymCipher() {
        return symCipher;
    }

    /**
     * ファイルを復号化してテキストを取得する. ファイルがなければ空文字が返される.
     * 
     * @param file
     *            ファイル
     * @return 復号化されたテキスト
     * @throws IOException
     *             失敗
     */
    public Object decrypt(File file) throws IOException {
        if (file == null || !file.exists()) {
            return "";
        }

        byte[] data = symCipher.decrypt(file);
        HashMap<String, String> headers = new HashMap<String, String>();

        int offset = parseHeader(data, headers);
        logger.log(Level.FINE, "headers=" + headers);

        int length = Integer.parseInt(headers.get("content-length"));

        String contentType = headers.get("content-type");

        // 文字列データの場合
        if (contentType.startsWith("text/")) {
            // 文字コードの取得
            String encoding = null;
            int pt = contentType.indexOf(';');
            if (pt > 0) {
                String args = contentType.substring(pt + 1);
                Map<String, String> argMap = new HashMap<String, String>();
                parseKeyValue(args, '=', argMap);
                logger.log(Level.FINE, "contentType(args)=" + headers);

                encoding = argMap.get("charset");
            }

            if (encoding == null || encoding.trim().length() == 0) {
                // 未指定の場合は現在のエンコーディング指定を使用する.
                encoding = settingsModel.getEncoding();
            }

            return new String(data, offset, length, encoding);
        }

        // 画像データの場合
        if (contentType.startsWith("image/")) {
            ImageIO.setUseCache(false);
            ByteArrayInputStream is = new ByteArrayInputStream(data, offset,
                    length);
            try {
                return ImageIO.read(is);
            } finally {
                is.close();
            }
        }

        // それ以外の場合
        return new ApplicationData(contentType, data);
    }

    /**
     * データからヘッダ部を解析し、ボディ部へのインデックスを戻り値とする.
     * 
     * @param data
     *            データ
     * @param headers
     *            解析したヘッダを格納するマップ、ヘッダのキーはすべて小文字にそろえられる
     * @return ボディ部へのインデックス
     */
    protected int parseHeader(byte[] data, Map<String, String> headers) {
        if (headers == null) {
            throw new IllegalArgumentException();
        }
        if (data == null) {
            data = new byte[0];
        }

        // 現在位置
        int pos = 0;

        // データの長さ
        int len = data.length;

        // ヘッダとして認識された行のリスト
        ArrayList<String> headerLines = new ArrayList<String>();

        // 現在処理中のヘッダ行を編集するためのバッファ
        StringBuilder strbuf = new StringBuilder();

        // ヘッダを読み取るためのバッファ
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        // データからヘッダ部を取り出すループ.
        while (pos < len) {
            byte c = data[pos++];
            if (c == '\r') {
                // CRは無視する. (簡略化のため)
                continue;
            }

            // 行終端判定
            if (c == '\n') {
                if (buf.size() == 0) {
                    // 行頭で終端であれば、ヘッダの取得終了とする.
                    break;
                }

                // ヘッダ行
                String line;
                try {
                    line = new String(buf.toByteArray(), "UTF-8");

                } catch (UnsupportedEncodingException ex) {
                    throw new RuntimeException(ex);
                }

                if (line.startsWith(" ") || line.startsWith("\t")) {
                    // タブまたは空白で始まる場合は前の継続行
                    strbuf.append(line.trim());

                } else {
                    // 前のヘッダ行を確定して次の行に進む
                    if (strbuf.length() > 0) {
                        headerLines.add(strbuf.toString());
                        strbuf.setLength(0);
                    }
                    strbuf.append(line);
                }

                // バッファのリセット
                buf.reset();
                continue;
            }

            // バッファに追加
            buf.write(c);
        }

        // 最後の処理中のヘッダ行を確定する.
        if (strbuf.length() > 0) {
            headerLines.add(strbuf.toString());
            strbuf.setLength(0);
        }

        // ヘッダの解析
        for (String line : headerLines) {
            parseKeyValue(line, ':', headers);
        }

        // ボディ位置を返す.
        return pos;
    }

    /**
     * 「key:value」形式をキーと値に分解してマップに追加する. キーは小文字にそろえられる.
     * 
     * @param line
     *            「key:value」の文字列
     * @param splitChar
     *            区切り文字
     * @param map
     *            格納先のマップ
     */
    protected void parseKeyValue(String line, char splitChar,
            Map<String, String> map) {
        if (map == null) {
            throw new IllegalArgumentException();
        }
        if (line == null || line.length() == 0) {
            // nullまたは空文字は何もしない.
            return;
        }
        int pt = line.indexOf(splitChar);
        if (pt > 0) {
            String name = line.substring(0, pt).trim().toLowerCase();
            String value = line.substring(pt + 1).trim();
            map.put(name, value);
        } else {
            map.put(line, "");
        }
    }

    /**
     * ファイルを暗号化してテキストを保存する.
     * 
     * @param file
     *            保存先ファイル
     * @param text
     *            暗号化するテキスト
     * @throws IOException
     *             失敗
     */
    public void encryptText(File file, String text) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException();
        }
        if (text == null) {
            text = "";
        }

        byte[] data = text.getBytes(settingsModel.getEncoding());

        String encoding = settingsModel.getEncoding();
        encrypt(file, data, "text/plain; charset=" + encoding);
    }

    /**
     * MIMEを指定してデータを暗号化して保存する.
     * 
     * @param file
     *            保存先ファイル名
     * @param data
     *            データ
     * @param mime
     *            データの形式を表すMIMEタイプ
     * @throws IOException
     *             失敗
     */
    public void encrypt(File file, byte[] data, String mime) throws IOException {
        if (file == null || mime == null || mime.length() == 0) {
            throw new IllegalArgumentException();
        }
        if (data == null) {
            data = new byte[0];
        }

        byte[] buf;
        {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                bos.write(("Content-Type: " + mime + "\r\n").getBytes("UTF-8"));
                bos.write(("Content-Length: " + data.length + "\r\n")
                        .getBytes("UTF-8"));
                bos.write("\r\n".getBytes("UTF-8"));
                bos.write(data);

            } finally {
                bos.close();
            }
            buf = bos.toByteArray();
        }

        symCipher.encrypt(buf, file);
    }
}
