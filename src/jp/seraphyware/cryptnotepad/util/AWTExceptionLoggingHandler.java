package jp.seraphyware.cryptnotepad.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import jp.seraphyware.cryptnotepad.Main;

/**
 * SwingのEDT内での例外をロギングするためのハンドラ.
 * 
 * @author seraphy
 */
public class AWTExceptionLoggingHandler {

    /**
     * ロガー
     */
    private static final Logger logger = Logger
            .getLogger(AWTExceptionLoggingHandler.class.getName());

    /**
     * 例外のハンドル.<br>
     * 
     * @param ex
     *            例外
     */
    public void handle(final Throwable ex) {
        // まずはロギング
        logger.log(Level.SEVERE,
                "exception occurred on the event dispatch thread.  " + ex, ex);

        // エラーダイアログを表示する.(非同期)
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame currentFrame = Main.getMainFrame();
                if (currentFrame == null || !currentFrame.isDisplayable()
                        || !currentFrame.isVisible()) {
                    // メインフレームがまだ無いか、表示されていないか破棄済みであれば無いとみなす.
                    currentFrame = null;
                }
                ErrorMessageHelper.showErrorDialog(currentFrame, ex);
            }
        });
    }
}
