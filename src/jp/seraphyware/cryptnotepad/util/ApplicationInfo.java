package jp.seraphyware.cryptnotepad.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * アプリケーションの情報.<br>
 * 
 * @author seraphy
 */
public final class ApplicationInfo {

    public static ApplicationInfo singleton = new ApplicationInfo();

    /**
     * タイトル
     */
    private String title;

    /**
     * バージョン
     */
    private String version;

    private ApplicationInfo() {
        try {
            load();

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Main-Classをもつ META-INF/MANIFEST.MF から、タイトルとバージョンを取得する.
     * 
     * @throws IOException
     */
    private void load() throws IOException {
        ClassLoader cl = ApplicationInfo.class.getClassLoader();

        // クラスパス上に複数のMANIFEST.MFが存在する可能性あり.
        // (たとえば QTJava.zip などのextフォルダ上のライブラリなど)
        Enumeration<URL> manifests = cl.getResources("META-INF/MANIFEST.MF");
        while (manifests.hasMoreElements()) {
            URL url = manifests.nextElement();

            // MANIFEST.MFをロードする.
            Manifest manifest;
            InputStream is = url.openStream();
            try {
                manifest = new Manifest(is);

            } finally {
                is.close();
            }

            Attributes mainAttr = manifest.getMainAttributes();

            // メイン属性を読み取る
            String mainClass = mainAttr.getValue("Main-Class");
            String title = mainAttr.getValue("Implementation-Title");
            String version = mainAttr.getValue("Implementation-Version");
            
            if (mainClass != null && mainClass.length() > 0 && title != null
                    && version != null) {
                // Main-Classが指定されているMANIFEST.MFの指定のみ採用する.
                this.title = title;
                this.version = version;
                break;
            }
        }

        if (this.title == null || this.version == null) {
            throw new RuntimeException(
                    "manifest.mfにtitle, version情報が設定されていません.");
        }
    }

    public String getTitle() {
        return title;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("(title=").append(title);
        buf.append(", version=").append(version);
        buf.append(")");
        return buf.toString();
    }

    public static ApplicationInfo getInstance() {
        return singleton;
    }
}
