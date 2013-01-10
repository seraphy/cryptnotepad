package jp.seraphyware.cryptnotepad;

import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import jp.seraphyware.cryptnotepad.model.ApplicationSettings;
import jp.seraphyware.cryptnotepad.model.DocumentController;
import jp.seraphyware.cryptnotepad.model.SettingsModel;
import jp.seraphyware.cryptnotepad.ui.MainFrame;
import jp.seraphyware.cryptnotepad.util.AWTExceptionLoggingHandler;
import jp.seraphyware.cryptnotepad.util.ApplicationLoggerConfigurator;
import jp.seraphyware.cryptnotepad.util.ConfigurationDirUtilities;
import jp.seraphyware.cryptnotepad.util.ErrorMessageHelper;

/**
 * メインエントリ.<br>
 * <br>
 * appbase.dirシステムプロパティでディレクトリを指定可能.<br>
 * 
 * @author seraphy
 */
public class Main implements Runnable {

    /**
     * ロガー.<br>
     */
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    /**
     * Mac OS Xであるか?
     */
    private static final boolean isMacOSX;

    /**
     * Mac OS XもしくはLinuxであるか?
     */
    private static final boolean isLinuxOrMacOSX;

    /**
     * アプリケーション設定ファイル
     */
    private File appConfigFile;

    /**
     * アプリケーション設定ファイルの変更フラグ
     */
    private boolean appConfigModified;

    /**
     * ドキュメントコントローラ
     */
    private static DocumentController documentController;

    /**
     * アプリケーション設定
     */
    private static ApplicationSettings appConfig;

    /**
     * メインフレームのインスタンス
     */
    private static MainFrame mainFrame;

    /**
     * クラスイニシャライザ.<br>
     * 実行環境に関する定数を取得・設定する.<br>
     */
    static {
        // Mac OS Xでの実行判定
        // システムプロパティos.nameは、すべてのJVM実装に存在する.
        String lcOS = System.getProperty("os.name").toLowerCase();
        isMacOSX = lcOS.startsWith("mac os x");
        isLinuxOrMacOSX = isMacOSX || lcOS.indexOf("linux") >= 0;
    }

    /**
     * ロガーの初期化.<br>
     * 失敗しても継続する.<br>
     */
    private static void initLogger() {
        try {
            // ロガーの準備

            // ローカルファイルシステム上のユーザ定義ディレクトリから
            // ログの設定を読み取る.(OSにより、設定ファイルの位置が異なることに注意)
            ApplicationLoggerConfigurator.configure();

            // SwingのEDT内の例外ハンドラの設定 (ロギングするだけ)
            System.setProperty("sun.awt.exception.handler",
                    AWTExceptionLoggingHandler.class.getName());

        } catch (Throwable ex) {
            // ロガーの準備に失敗した場合はロガーがないかもなので
            // コンソールに出力する.
            ex.printStackTrace();
            logger.log(Level.SEVERE, "logger initiation failed. " + ex, ex);
        }
    }
    
    /**
     * 診断情報をロギングする.
     */
    private static void dumpDiagnostics() {
        if (!logger.isLoggable(Level.FINE)) {
            // FINEよりも詳細レベルでなければ診断情報はプリントしない.
            return;
        }
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);

            // システムプロパティ一覧
            pw.println("*System Properties");
            Properties sysProps = System.getProperties();
            TreeSet<String> propNames = new TreeSet<String>();
            Enumeration<?> enm = sysProps.propertyNames();
            while (enm.hasMoreElements()) {
                String propName = (String) enm.nextElement();
                propNames.add(propName);
            }
            for (String propName : propNames) {
                String value = sysProps.getProperty(propName);
                pw.println(propName + "=" + value);
            }
            pw.println();
            
            // 環境変数一覧
            pw.println("*Environments");
            Map<String, String> envMap = System.getenv();
            TreeSet<String> envNames = new TreeSet<String>();
            for (String envName : envMap.keySet()) {
                envNames.add(envName);
            }
            for (String envName : envNames) {
                String value = envMap.get(envName);
                pw.println(envName + "=" + value);
            }
            
            // ログに書き込み
            logger.log(Level.FINE, sw.toString());
            
        } catch (Exception ex) {
            logger.log(Level.WARNING, "diagnostics error: " + ex, ex);
        }
    }

    /**
     * UIをセットアップする.
     * 
     * @throws Exception
     *             いろいろな失敗
     */
    private static void setupUIManager() throws Exception {
        // System.setProperty("swing.aatext", "true");
        // System.setProperty("awt.useSystemAAFontSettings", "on");

        // MacOSXであれば、スクリーンメニューを有効化
        if (isMacOSX()) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty(
                    "com.apple.mrj.application.apple.menu.about.name",
                    "CryptNotepad"); // タイトル (Tiger以前のみ有効)
        }

        // 実行プラットフォームのネイティブな外観にする.
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        // JSpliderのvalueを非表示 (GTKではデフォルトで有効のため)
        UIManager.put("Slider.paintValue", Boolean.FALSE);

        // JTextAreaの既定フォントを固定幅から、標準テキストと同じフォントに変更.
        // (Linuxなどで固定幅フォントでは日本語フォントを持っていないため。)
        Object textFieldFontUI = UIManager.get("TextField.font");
        if (textFieldFontUI == null) {
            // もし無ければダイアログUIフォントを使う.(これは日本語をサポートするであろう。)
            textFieldFontUI = new UIDefaults.ProxyLazyValue(
                    "javax.swing.plaf.FontUIResource", null, new Object[] {
                            "dialog", Integer.valueOf(Font.PLAIN),
                            Integer.valueOf(12) });
        }
        UIManager.put("TextArea.font", textFieldFontUI);
    }

    /**
     * 初期処理およびメインフレームを構築する.<br>
     * SwingのUIスレッドで実行される.<br>
     */
    public void run() {
        try {
            // UIManagerのセットアップ.
            try {
                setupUIManager();

            } catch (Exception ex) {
                // UIManagerの設定に失敗した場合はログに書いて継続する.
                ex.printStackTrace();
                logger.log(Level.WARNING, "UIManager setup failed.", ex);
            }

            // アプリケーション設定の初期化
            appConfig = ApplicationSettings.getInstance();

            // ドキュメントコンストローラの設定
            documentController = new DocumentController();

            // アプリケーション設定のロード
            initAppConfig();
            ApplicationSettings appConfig = ApplicationSettings.getInstance();

            // MainFrameの表示
            mainFrame = new MainFrame(documentController);
            mainFrame.setSize(appConfig.getMainFrameWidth(),
                    appConfig.getMainFrameHeight());
            mainFrame.setLocationByPlatform(true);
            mainFrame.setVisible(true);

            // ウィンドウ破棄イベントのハンドリング
            mainFrame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    onCloseMainFrame(mainFrame);
                }
            });

        } catch (Throwable ex) {
            // なんらかの致命的な初期化エラーがあった場合、ログとコンソールに表示
            // ダイアログが表示されるかどうかは状況次第.
            ex.printStackTrace();
            logger.log(Level.SEVERE, "Application initiation failed.", ex);
            ErrorMessageHelper.showErrorDialog(null, ex);
        }
    }

    /**
     * アプリケーション終了のハンドリング.
     * 
     * @param mainFrame
     */
    protected void onCloseMainFrame(MainFrame mainFrame) {
        // ウィンドウサイズの保存
        if (mainFrame.getExtendedState() == JFrame.NORMAL) {
            int width = mainFrame.getWidth();
            int height = mainFrame.getHeight();
            if (width > 100 && height > 100) {
                appConfig.setMainFrameWidth(width);
                appConfig.setMainFrameHeight(height);
            }
        }

        // 設定を退避する.
        SettingsModel settingModel = documentController.getSettingsModel();
        appConfig.setEncoding(settingModel.getEncoding());
        appConfig.setKeyFile(settingModel.getKeyFile());

        // アプリケーション設定の保存
        saveAppConfig();

        // ドキュメントコントローラを破棄する.
        // (パスフレーズなどをメモリから除去する.)
        documentController.dispose();

        logger.log(Level.INFO, "normal shutdown.");
    }

    /**
     * アプリケーション設定ファイルの初期化とロード
     */
    public void initAppConfig() {
        try {
            // アプリケーション設定ファイルの位置を求める
            File userDir = ConfigurationDirUtilities.getUserDataDir();
            appConfigFile = new File(userDir, "appconfig.xml");

            // 環境による初期値を設定する.
            appConfig.setWorkingDir(new File(System
                    .getProperty("java.io.tmpdir")));
            appConfig.setContentsDir(ConfigurationDirUtilities
                    .getApplicationBaseDir());

            // 設定ファイルをロードする.
            appConfig.load(appConfigFile);

            // プロパティ変更イベントを受け取れるようにリスナーを設定する.
            appConfig.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    // 終了時に設定ファイルを保存するようにフラグを立てておく.
                    appConfigModified = true;
                }
            });

            // 設定に反映する.
            SettingsModel settingModel = documentController.getSettingsModel();
            settingModel.setEncoding(appConfig.getEncoding());
            settingModel.setKeyFile(appConfig.getKeyFile());

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.log(Level.WARNING, "config file load failed.", ex);
        }
    }

    /**
     * アプリケーション設定ファイルを保存する.
     */
    public void saveAppConfig() {
        try {
            // アプリケーション設定が変更されている場合、
            if (appConfigModified) {
                // 作業フォルダはデフォルトと変更なければ空にして保存する.
                File workingDir = appConfig.getWorkingDir();
                if (workingDir.equals(new File(System.getProperty("java.io.tmpdir")))) {
                    appConfig.setWorkingDir(null);
                }
                
                // 設定ファイルを保存する.
                if (appConfig.save(appConfigFile)) {
                    logger.log(Level.INFO, "config file was saved.");
                }
            }

        } catch (Exception ex) {
            // 書き込みに失敗してもログに記録するだけ.
            ex.printStackTrace();
            logger.log(Level.WARNING, "config file save failed.", ex);
        }
    }

    /**
     * エントリポイント.<br>
     * 最初のメインフレームを開いたときにMac OS Xであればスクリーンメニューの登録も行う.<br>
     * 
     * @param args
     *            引数(未使用)
     */
    public static void main(String[] args) {
        // ロガー等の初期化
        initLogger();
        
        // 診断情報のプリント
        dumpDiagnostics();

        // プロキシのシステム設定の利用を許可
        System.setProperty("java.net.useSystemProxies", "true");

        // フレームの生成等は、SwingのEDTで実行する.
        SwingUtilities.invokeLater(new Main());
    }

    /**
     * Mac OS Xで動作しているか?
     * 
     * @return Max OS X上であればtrue
     */
    public static boolean isMacOSX() {
        return isMacOSX;
    }

    /**
     * Mac OS X、もしくはlinuxで動作しているか?
     * 
     * @return Mac OS X、もしくはlinuxで動作していればtrue
     */
    public static boolean isLinuxOrMacOSX() {
        return isLinuxOrMacOSX;
    }

    /**
     * メインフレームのインスタンスを取得する. まだ作成されていない場合はnull.
     * 
     * @return メインフレーム
     */
    public static MainFrame getMainFrame() {
        return mainFrame;
    }
}
