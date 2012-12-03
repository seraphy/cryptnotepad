package jp.seraphyware.cryptnotepad;

import java.awt.Font;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import jp.seraphyware.cryptnotepad.ui.MainFrame;
import jp.seraphyware.cryptnotepad.util.AWTExceptionLoggingHandler;
import jp.seraphyware.cryptnotepad.util.ApplicationLoggerConfigurator;
import jp.seraphyware.cryptnotepad.util.ErrorMessageHelper;

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

            // MainFrameの表示
            mainFrame = new MainFrame();
            mainFrame.setSize(600, 400);
            mainFrame.setLocationByPlatform(true);
            mainFrame.setVisible(true);

        } catch (Throwable ex) {
            // なんらかの致命的な初期化エラーがあった場合、ログとコンソールに表示
            // ダイアログが表示されるかどうかは状況次第.
            ex.printStackTrace();
            logger.log(Level.SEVERE, "Application initiation failed.", ex);
            ErrorMessageHelper.showErrorDialog(null, ex);
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
