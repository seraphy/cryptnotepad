package jp.seraphyware.cryptnotepad.model;

import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.event.SwingPropertyChangeSupport;

/**
 * アプリケーション設定.
 * 
 * @author seraphy
 */
public final class ApplicationSettings implements Serializable {

    private static final long serialVersionUID = -492505181503113803L;

    /**
     * ロガー.<br>
     */
    private static final Logger logger = Logger
            .getLogger(ApplicationSettings.class.getName());

    /**
     * シングルトン
     */
    private static final ApplicationSettings singleton = new ApplicationSettings();

    /**
     * プロパティ変更通知のサポート
     */
    private final SwingPropertyChangeSupport propChange = new SwingPropertyChangeSupport(
            this);

    /**
     * 文字コード
     */
    private String encoding = "UTF-8";

    /**
     * キーファイル
     */
    private String keyFile;

    /**
     * 最後に使用したディレクトリ
     */
    private File lastUseDir;

    /**
     * 最後にインポートに使用した文字コード
     */
    private String lastUseImportTextEncoding;

    /**
     * メインフレームの幅
     */
    private int mainFrameWidth = 600;

    /**
     * メインフレームの高さ
     */
    private int mainFrameHeight = 400;

    /**
     * フォント名
     */
    private String fontName;

    /**
     * フォントサイズ
     */
    private int fontSize;

    /**
     * プライベートコンストラクタ
     */
    private ApplicationSettings() {
        super();
    }

    public static ApplicationSettings getInstance() {
        return singleton;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propChange.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propChange.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        propChange.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        propChange.removePropertyChangeListener(propertyName, listener);
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getKeyFile() {
        return keyFile;
    }

    public void setKeyFile(String keyFile) {
        String oldValue = this.keyFile;
        this.keyFile = keyFile;
        propChange.firePropertyChange("keyFile", oldValue, keyFile);
    }

    public File getLastUseDir() {
        return lastUseDir;
    }

    public void setLastUseDir(File lastUseDir) {
        File oldValue = this.lastUseDir;
        this.lastUseDir = lastUseDir;
        propChange.firePropertyChange("lastUseDir", oldValue, lastUseDir);
    }

    public String getLastUseImportTextEncoding() {
        return lastUseImportTextEncoding;
    }

    public void setLastUseImportTextEncoding(String lastUseImportTextEncoding) {
        String oldValue = this.lastUseImportTextEncoding;
        this.lastUseImportTextEncoding = lastUseImportTextEncoding;
        propChange.firePropertyChange("lastUseImportTextEncoding", oldValue,
                lastUseImportTextEncoding);
    }

    public int getMainFrameWidth() {
        return mainFrameWidth;
    }

    public void setMainFrameWidth(int mainFrameWidth) {
        int oldValue = this.mainFrameWidth;
        this.mainFrameWidth = mainFrameWidth;
        propChange.firePropertyChange("mainFrameWidth", oldValue,
                mainFrameWidth);
    }

    public int getMainFrameHeight() {
        return mainFrameHeight;
    }

    public void setMainFrameHeight(int mainFrameHeight) {
        int oldValue = this.mainFrameHeight;
        this.mainFrameHeight = mainFrameHeight;
        propChange.firePropertyChange("mainFrameHeight", oldValue,
                mainFrameHeight);
    }

    public String getFontName() {
        return fontName;
    }

    public void setFontName(String fontName) {
        String oldValue = this.fontName;
        this.fontName = fontName;
        propChange.firePropertyChange("fontName", oldValue, fontName);
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        int oldValue = this.fontSize;
        this.fontSize = fontSize;
        propChange.firePropertyChange("fontSize", oldValue, fontSize);
    }

    /**
     * ファイルに保存する.
     * 
     * @param file
     * @throws IOException
     */
    public void save(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException();
        }

        Properties props = new Properties();

        // 現在のファイルの読み込み
        if (file.exists()) {
            props.loadFromXML(new BufferedInputStream(new FileInputStream(file)));
        }

        // 設定項目
        props.setProperty("lastUseDir",
                (lastUseDir == null) ? "" : lastUseDir.getAbsolutePath());
        props.setProperty("lastUseImportTextEncoding",
                (lastUseImportTextEncoding == null) ? ""
                        : lastUseImportTextEncoding);
        props.setProperty("mainFrameWidth", Integer.toString(mainFrameWidth));
        props.setProperty("mainFrameHeight", Integer.toString(mainFrameHeight));
        props.setProperty("fontName", (fontName == null) ? "" : fontName);
        props.setProperty("fontSize", Integer.toString(fontSize));
        props.setProperty("encoding", (encoding == null) ? "" : encoding);
        props.setProperty("keyFile", (keyFile == null) ? "" : keyFile);

        logger.log(Level.FINE, "appConfig=" + props);

        // ファイルへの書き込み
        OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
        try {
            props.storeToXML(os, "");
        } finally {
            os.close();
        }
    }

    /**
     * ファイルから復元する.
     * 
     * @param file
     * @throws IOException
     */
    public void load(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException();
        }

        Properties props = new Properties();

        if (file.exists()) {
            props.loadFromXML(new FileInputStream(file));
        }

        lastUseDir = parseFile(props.getProperty("lastUseDir"), lastUseDir);
        lastUseImportTextEncoding = props.getProperty("lastUseImportTextEncoding");
        mainFrameWidth = parseInt(props.getProperty("mainFrameWidth"),
                mainFrameWidth);
        mainFrameHeight = parseInt(props.getProperty("mainFrameHeight"),
                mainFrameHeight);
        fontName = props.getProperty("fontName");
        fontSize = parseInt(props.getProperty("fontSize"), fontSize);
        encoding = props.getProperty("encoding");
        keyFile = props.getProperty("keyFile");
    }

    /**
     * 文字列から数値(int)に変換する. nullまたは空文字、あるいは変換できない場合はデフォルトを用いる.
     * 
     * @param str
     *            文字列
     * @param defValue
     *            デフォルト
     * @return 数値
     */
    private static int parseInt(String str, int defValue) {
        if (str != null && str.trim().length() > 0) {
            try {
                return Integer.parseInt(str);

            } catch (Exception ex) {
                logger.log(Level.FINEST, "format error: " + str, ex);
                // 無視する
            }
        }
        return defValue;
    }

    /**
     * 文字列からファイルオブジェクト(File)に変換する. nullまたは空文字、あるいは変換できない場合はデフォルトを用いる.
     * 
     * @param str
     *            文字列
     * @param defValue
     *            デフォルト
     * @return ファイルオブジェクト
     */
    private static File parseFile(String str, File defValue) {
        if (str != null && str.trim().length() > 0) {
            try {
                return new File(str);

            } catch (Exception ex) {
                logger.log(Level.FINEST, "format error: " + str, ex);
                // 無視する
            }
        }
        return defValue;
    }
}
