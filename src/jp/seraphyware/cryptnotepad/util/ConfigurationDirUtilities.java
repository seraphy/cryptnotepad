package jp.seraphyware.cryptnotepad.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

import jp.seraphyware.cryptnotepad.Main;

/**
 * アプリケーションの設定ファイル等の位置を取得するユーテリティクラス.<br>
 * Mainクラスのロード時に最も早くロードされるであろうクラスの一つである.<br>
 * 
 * @author seraphy
 */
public final class ConfigurationDirUtilities {
    
    /**
     * アプリケーション配備位置を明示的に指定するためのシステムプロパティ名.<br>
     */
    public static final String APPBASE_DIR = "appbase.dir";
    
    /**
     * ユーザごとのプロファイルデータを格納するディレクトリを明示的に指定するためのシステムプロパティ名.<br>
     */
    public static final String APPDATA_DIR = "appdata.dir";

    /**
     * ユーザごとの設定ファイル等を格納するディレクトリ.<br>
     */
    private static File userDataDir;

    /**
     * アプリケーションが配備されているディレクトリ.<br>
     */
    private static File applicationBaseDir;

    private ConfigurationDirUtilities() {
        throw new RuntimeException("utilities class.");
    }

    /**
     * ユーザーごとのアプリケーションデータ保存先を取得する.<br>
     * 環境変数「APPDATA」もしくはシステムプロパティ「appdata.dir」からベース位置を取得する.<br>
     * いずれも設定されておらず、Mac OS Xであれば「~/Library」をベース位置とする。 Mac OS
     * Xでなければ「~/」をベース位置とする.<br>
     * これに対してMANIFEST.MFのImplementation-Titleの文字列をフォルダ名としたものを返す.
     */
    public synchronized static File getUserDataDir() {
        if (userDataDir == null) {

            String appData = null;
            // システムプロパティ「appdata.dir」を探す
            appData = System.getProperty(APPDATA_DIR);
            if (appData == null) {
                // なければ環境変数APPDATAを探す
                // Windows2000/XP/Vista/Windows7/8には存在する.
                appData = System.getenv("APPDATA");
            }
            if (appData == null && Main.isMacOSX()) {
                // システムプロパティも環境変数にも設定がなく、実行環境がMac OS Xであれば
                // ~/Libraryをベースにする.(Mac OS Xならば必ずある。)
                appData = new File(System.getProperty("user.home"), "Library")
                        .getPath();
            }
            if (appData == null) {
                // なければシステムプロパティ「user.home」を使う
                // このプロパティは必ず存在する.
                appData = System.getProperty("user.home");
            }

            // ディレクトリ名はMANIFEST.MFのタイトルより設定する.
            ApplicationInfo appInfo = ApplicationInfo.getInstance();
            String title = appInfo.getTitle();
            try {
                userDataDir = new File(appData, title).getAbsoluteFile()
                        .getCanonicalFile();

            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            // ディレクトリを準備する.
            if (!userDataDir.exists()) {
                if (!userDataDir.mkdirs()) {
                    // ログ保管場所も設定されていないのでコンソールに出すしかない.
                    System.err.println("can't create the user data directory. "
                            + userDataDir);
                }
            }
        }
        return userDataDir;
    }

    /**
     * アプリケーションディレクトリを取得する.<br>
     * このクラスをコードソースから、ベースとなる位置を割り出す.<br>
     * クラスが格納されているクラスパスのフォルダか、JARに固められている場合は、そのJARファイルの、その親ディレクトリを指し示す.<br>
     * このクラスのプロテクションドメインのコードソースがnullでコードの位置が取得できないか、
     * コードの位置を示すURLがファイルプロトコルでない場合は実行時例外が返される.<br>
     * ただし、システムプロパティ「appbase.dir」が明示的に設定されていれば、それが優先される.<br>
     */
    public synchronized static File getApplicationBaseDir() {
        if (applicationBaseDir == null) {

            String appbaseDir = System.getProperty(APPBASE_DIR);
            if (appbaseDir != null && appbaseDir.length() > 0) {
                // 明示的にアプリケーションベースディレクトリが指定されている場合.
                try {
                    applicationBaseDir = new File(appbaseDir)
                            .getCanonicalFile();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    // 継続する.まだログの準備ができていない可能性が高いので標準エラー出力へ.
                }
            }
            if (applicationBaseDir == null) {
                // 明示的に指定されていない場合はコードの実行位置から割り出す.
                URL codeBaseUrl = getCodeBaseUrl();
                if (codeBaseUrl == null) {
                    throw new RuntimeException("実行コードが配置されている場所を判定できません");
                }
                // クラスパスフォルダ、またはJARファイルの、その親
                applicationBaseDir = new File(codeBaseUrl.getPath())
                        .getParentFile();
            }
        }
        return applicationBaseDir;
    }

    /**
     * 実行中のクラスが定義されている親フォルダを取得します. binフォルダ下にクラスファイルがある場合は、そのbinフォルダの親を返します.
     * jarまたはzipに格納されている場合は、そのjarまたはzipが置かれているフォルダを返します.
     * 
     * @see http://www.javaworld.com/javaqa/2003-07/01-qa-0711-classsrc.html
     * @return URL that points to the class definition [null if not found].
     */
    private static URL getCodeBaseUrl() {
        URL result = null;
        ProtectionDomain pd = ConfigurationDirUtilities.class
                .getProtectionDomain();
        if (pd != null) {
            CodeSource cs = pd.getCodeSource();
            if (cs != null) {
                result = cs.getLocation();
            }
        }
        return result;
    }
}
