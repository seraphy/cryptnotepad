package jp.seraphyware.cryptnotepad.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jp.seraphyware.cryptnotepad.crypt.SymCipher;
import jp.seraphyware.cryptnotepad.crypt.SymCipherEvent;
import jp.seraphyware.cryptnotepad.crypt.SymCipherEventListener;

/**
 * ドキュメントの制御クラス.
 * 
 * @author seraphy
 */
public class DocumentController implements SymCipherEventListener {

    /**
     * パスフレーズの要求または確認用のUIハンドラ.<br>
     */
    public interface PassphraseUIProvider {

        /**
         * パスフレーズの入力が必要な場合に呼び出されます.<br>
         * 
         * @param settingsModel
         *            モデル
         * @return 続行する場合はtrue、キャンセルする場合はfalse
         */
        boolean requirePassphrase(SettingsModel settingsModel);

        /**
         * パスフレーズの照合が必要な場合に呼び出されます.<br>
         * 
         * @param settingsModel
         *            モデル
         * @return 続行する場合はtrue、キャンセルする場合はfalse
         */
        boolean verifyPassphrase(SettingsModel settingsModel);

        /**
         * ドキュメントがセキュリティ上の理由が開けなかった場合.<br>
         * (ハンドルした場合は例外はスローされません.)<br>
         * 
         * @param file
         *            対象ファイル
         * @param cause
         *            例外
         * @return ハンドルされた場合はtrue、そうでなければfalse
         */
        boolean securityError(File file, Throwable cause);

        /**
         * ファイルが作成または変更されたことを通知する.
         * 
         * @param oldFile
         *            nullの場合は新規作成
         * @param newFile
         *            nullの場合は削除
         */
        void fileUpdated(File oldFile, File newFile);
    }

    /**
     * ロガー.<br>
     */
    private static final Logger logger = Logger
            .getLogger(DocumentController.class.getName());

    /**
     * アプリケーション設定
     */
    private ApplicationSettings appConfig;

    /**
     * 設定モデル
     */
    private SettingsModel settingsModel;

    /**
     * 暗号化
     */
    private SymCipher symCipher;

    /**
     * パスフレーズの確認済みフラグ.
     */
    private boolean passphraseVerified;

    /**
     * パスフレーズのUIプロバイダ
     */
    private PassphraseUIProvider passphraseUiProvider;

    /**
     * コンストラクタ
     */
    public DocumentController() {
        // アプリケーション設定
        appConfig = ApplicationSettings.getInstance();

        // 設定モデルインスタンスの構築
        settingsModel = new SettingsModel();

        // 暗号化・復号化器の構築
        symCipher = new SymCipher(settingsModel);

        // パスフレーズ変更イベント検知
        settingsModel.addPropertyChangeListener("passphrase",
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        // パスフレーズが変更された場合はパスワード確認済みをリセットする.
                        passphraseVerified = false;
                    }
                });

        // 暗号化・復号化前イベント
        symCipher.addSymCipherEventListener(this);
    }

    public void dispose() {
        settingsModel.clear();
    }

    public void setPassphraseUiProvider(
            PassphraseUIProvider passphraseUiProvider) {
        this.passphraseUiProvider = passphraseUiProvider;
    }

    public PassphraseUIProvider getPassphraseUiProvider() {
        return passphraseUiProvider;
    }

    public SettingsModel getSettingsModel() {
        return settingsModel;
    }

    public SymCipher getSymCipher() {
        return symCipher;
    }

    @Override
    public void preDecryption(SymCipherEvent e) {
        // パスフレーズをチェックし、必要ならばUIでパスフレーズを入力してもらう.
        checkPassphrase(e);
    }

    @Override
    public void preEncryption(SymCipherEvent e) {
        // パスフレーズをチェックし、必要ならばUIでパスフレーズを入力してもらう.
        checkPassphrase(e);
        if (e.isCancel()) {
            // キャンセルされたら、処理を中断する.
            return;
        }

        if (!passphraseVerified) {
            // パスフレーズ設定後の最初の保存では、パスフレーズを照合を必要とする.
            boolean ret = false;
            if (passphraseUiProvider != null) {
                ret = passphraseUiProvider.verifyPassphrase(settingsModel);
            }
            if (!ret) {
                // 入力されなかった場合.
                e.cancel();

            } else {
                // パスフレーズの照合済み.
                passphraseVerified = true;
            }
        }
    }

    /**
     * 例外の通知を受けた場合.
     */
    @Override
    public void preThrowException(SymCipherEvent e) {
        if (!e.isModeEncryption()) {
            // 復号化時の例外の場合
            File file = e.getFile();
            Throwable cause = e.getCause();
            logger.log(Level.INFO, "document decryption failed. " + file, cause);

            // UIに例外を通知する.
            boolean handled = true;
            if (passphraseUiProvider != null) {
                handled = passphraseUiProvider.securityError(file, cause);
            }
            if (handled) {
                // UI側で例外メッセージが表示された場合は
                // 呼び出し元で例外をスローする必要はないのでキャンセルする.
                e.setCancel(true);
            }

            // パスフレーズが誤りである可能性が高いためリセットしておく.
            settingsModel.setPassphrase(null);
        }
    }

    /**
     * パスフレーズをチェックし、必要ならばUIでパスフレーズを入力してもらう.<br>
     * UIで処理のキャンセルが指示された場合は、それを呼び出し元に伝搬する.<br>
     * 
     * @param e
     *            イベント
     */
    protected void checkPassphrase(SymCipherEvent e) {
        if (!settingsModel.isValid() && passphraseUiProvider != null) {
            boolean ret = false;
            if (passphraseUiProvider != null) {
                ret = passphraseUiProvider.requirePassphrase(settingsModel);
            }
            if (!ret || !settingsModel.isValid()) {
                // キャンセルされたか、まだパスフレーズが確認できなければ処理をキャンセルする.
                e.cancel();
            }
        }
    }

    /**
     * 拡張子からContent-Type(疑似)を返す.<br>
     * 不明な場合はnullを返す.<br>
     * 
     * @param file
     *            ファイル
     * @return 拡張子から判定されるContent-Type(疑似)、不明ならばnull
     */
    public String detectContentType(File file) {
        if (file == null) {
            return null;
        }

        // ファイルの拡張子を取り出す.
        String lcFileName = file.getName().toLowerCase();
        int pt = lcFileName.lastIndexOf('.');
        String ext;
        if (pt > 0) {
            ext = lcFileName.substring(pt + 1);
        } else {
            ext = lcFileName;
        }

        if (isInExtensions(ext, appConfig.getExtensionsForText())) {
            if (ext.equals("txt")) {
                return "text/plain";
            }
            return "text/" + ext;
        }

        if (isInExtensions(ext, appConfig.getExtensionsForPicture())) {
            if (ext.equals("jpg")) {
                return "image/jpeg";
            }
            return "image/" + ext;
        }

        if (isInExtensions(ext, appConfig.getExtensionsForBinary())) {
            return "application/" + ext;
        }

        // 不明
        return null;
    }

    /**
     * カンマ区切りの拡張子リストのなかに指定された拡張子があるか?
     * 
     * @param ext
     *            拡張子
     * @param extensions
     *            カンマ区切りの拡張子のリスト
     * @return リストに含まれる場合はtrue
     */
    protected boolean isInExtensions(String ext, String extensions) {
        if (ext == null || extensions == null) {
            return false;
        }
        for (String extension : extensions.split(",")) {
            extension = extension.trim().toLowerCase();
            if (ext.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * バイナリデータを読み取ります.<br>
     * ファイルが存在しない場合はnullを返します.<br>
     * 
     * @param file
     *            ファイル
     * @return バイナリデータ、もしくはnull
     * @throws IOException
     *             失敗
     */
    public byte[] loadBinary(File file) throws IOException {
        if (file == null || !file.exists()) {
            return null;
        }

        int len = (int) file.length();
        byte[] buf = new byte[len];

        RandomAccessFile fh = new RandomAccessFile(file, "r");
        try {
            fh.seek(0);
            fh.readFully(buf);

        } finally {
            fh.close();
        }

        return buf;
    }

    /**
     * テキストファイルをロードする.<br>
     * 
     * @param file
     *            ファイル
     * @param encoding
     *            文字コード、nullの場合は設定に従う.
     * @return 読み込まれたテキスト
     * @throws IOException
     *             失敗
     */
    public String loadText(File file, String encoding) throws IOException {
        if (file == null || !file.exists()) {
            return "";
        }

        if (encoding == null || encoding.trim().length() == 0) {
            encoding = settingsModel.getEncoding();
        }

        StringBuilder buf = new StringBuilder();
        char[] cbuf = new char[1024];
        InputStreamReader reader = new InputStreamReader(new FileInputStream(
                file), encoding);
        try {
            for (;;) {
                int rd = reader.read(cbuf);
                if (rd < 0) {
                    break;
                }
                buf.append(cbuf, 0, rd);
            }
        } finally {
            reader.close();
        }
        return buf.toString();
    }

    /**
     * ファイルを復号化してコンテンツを取得する.<br>
     * コンテンツがテキストであれば文字列が返される.<br>
     * コンテンツが画像であればBufferedImageが返される.<br>
     * それ以外のバイナリデータであればApplicationDataが返される.<br>
     * ファイルがなければnullが返される.<br>
     * 
     * @param file
     *            ファイル
     * @return 復号化されたテキストまたは画像またはApplicationData、もしくはnull
     * @throws IOException
     *             失敗
     */
    public Object decrypt(File file) throws IOException {
        byte[] data = symCipher.decrypt(file);
        if (data == null) {
            // ファイルが存在しない場合、
            // もしくは存在しないとみなす場合.
            return null;
        }

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

        // 画像データの場合か、それ以外の場合
        byte[] buf = new byte[length];
        System.arraycopy(data, offset, buf, 0, length);
        return new ApplicationData(contentType, buf);
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

        FileUpdateNotifier notifier = new FileUpdateNotifier(file);
        try {
            if (text == null) {
                text = "";
            }

            String encoding = settingsModel.getEncoding();
            byte[] data = text.getBytes(encoding);

            encrypt(file, data, "text/plain; charset=" + encoding);

        } finally {
            notifier.checkAndNotify();
        }
    }

    /**
     * 平文でテキストをファイルに保存します.
     * 
     * @param file
     *            ファイル
     * @param text
     *            テキスト
     * @throws IOException
     */
    public void savePlainText(File file, String text) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException();
        }

        FileUpdateNotifier notifier = new FileUpdateNotifier(file);
        try {
            if (text == null) {
                text = "";
            }

            String encoding = settingsModel.getEncoding();
            byte[] data = text.getBytes(encoding);

            OutputStream os = new BufferedOutputStream(new FileOutputStream(
                    file));
            try {
                os.write(data);

            } finally {
                os.close();
            }

        } finally {
            notifier.checkAndNotify();
        }
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

        FileUpdateNotifier notifier = new FileUpdateNotifier(file);
        try {
            symCipher.encrypt(buf, file);

        } finally {
            notifier.checkAndNotify();
        }
    }

    /**
     * 平文でバイナリデータをファイルに保存します.
     * 
     * @param file
     *            ファイル
     * @param data
     *            バイナリデータ、nullの場合は空とみなす.
     * @throws IOException
     */
    public void savePlainBinary(File file, byte[] data) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException();
        }

        FileUpdateNotifier notifier = new FileUpdateNotifier(file);
        try {
            if (data == null) {
                data = new byte[0];
            }

            OutputStream os = new BufferedOutputStream(new FileOutputStream(
                    file));
            try {
                os.write(data);

            } finally {
                os.close();
            }

        } finally {
            notifier.checkAndNotify();
        }
    }

    /**
     * ファイルの作成または更新を検知するヘルパークラス.<br>
     */
    private class FileUpdateNotifier {

        private File file;

        private boolean exists;

        FileUpdateNotifier(File file) {
            this.file = file;
            if (file != null) {
                exists = file.exists();
            }
        }

        void checkAndNotify() {
            if (file != null && file.exists()) {
                if (passphraseUiProvider != null) {
                    File oldFile = exists ? file : null;
                    passphraseUiProvider.fileUpdated(oldFile, file);
                }
            }
        }
    }
}
