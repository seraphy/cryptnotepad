package jp.seraphyware.cryptnotepad.util;

import java.awt.Component;
import java.awt.Dimension;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

/**
 * 例外を表示するダイアログ.<br>
 * ログにも記録される.<br>
 * 
 * @author seraphy
 */
public abstract class ErrorMessageHelper {

    /**
     * ロガー
     */
    private static final Logger logger = Logger
            .getLogger(ErrorMessageHelper.class.getName());

    /**
     * 例外クラスとエラーハンドラ.<br>
     * 例外クラスは完全一致でなければならない.<br>
     */
    private static HashMap<Class<?>, ErrorMessageHelper> handlers = new HashMap<Class<?>, ErrorMessageHelper>();

    /**
     * デフォルトのエラーメッセージハンドラ.
     */
    public static final ErrorMessageHelper defaultErrorMessageHandler = new ErrorMessageHelper() {
        @Override
        protected void show(Component parent, Throwable ex) {
            if (ex == null) {
                return;
            }

            // ログに記録する.
            logger.log(Level.SEVERE, ex.getLocalizedMessage(), ex);

            // 例外を表示するパネルの生成
            JTextArea textArea = new JTextArea();

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw); // 例外のコールスタックをパネルに表示できるように出力
            pw.close();

            textArea.setText(sw.toString());

            textArea.setSelectionStart(0);
            textArea.setSelectionEnd(0);
            textArea.setEditable(false);

            JScrollPane scr = new JScrollPane(textArea);
            scr.setPreferredSize(new Dimension(400, 150));
            scr.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
            scr.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

            // ダイアログの表示
            JOptionPane.showMessageDialog(parent, scr, "ERROR",
                    JOptionPane.ERROR_MESSAGE);
        }
    };

    /**
     * シンプルな警告メッセージハンドラ.
     */
    public static final ErrorMessageHelper simpleErrorMessageHandler = new ErrorMessageHelper() {
        @Override
        protected void show(Component parent, Throwable ex) {
            if (ex == null) {
                return;
            }

            // ログに記録する.
            logger.log(Level.WARNING, ex.getLocalizedMessage(), ex);

            // ダイアログの表示
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                msg = ex.toString();
            }
            JOptionPane.showMessageDialog(parent, msg, "ERROR",
                    JOptionPane.WARNING_MESSAGE);
        }
    };

    /**
     * 例外が発生したことを示すダイアログを表示し、ログに記録する.<br>
     * 
     * @param parent
     *            ダイアログを表示する親、null可
     * @param ex
     *            例外、nullの場合はなにもせずに戻る.
     */
    protected abstract void show(Component parent, Throwable ex);

    /**
     * 例外が発生したことを示すダイアログを表示し、ログに記録する.<br>
     * 
     * @param parent
     *            ダイアログを表示する親、null可
     * @param ex
     *            例外、nullの場合はなにもせずに戻る.
     */
    public static void showErrorDialog(Component parent, Throwable ex) {
        if (ex == null) {
            return;
        }

        Class<?> cls = ex.getClass();
        ErrorMessageHelper handler = handlers.get(cls);
        if (handler == null) {
            handler = defaultErrorMessageHandler;
        }

        handler.show(parent, ex);
    }

    /**
     * 例外クラスに対するエラーメッセージハンドラを設定する.<br>
     * 例外クラスは完全一致でなければならない.(継承関係は考慮されない).<br>
     * 
     * @param cls
     *            クラス
     * @param helper
     *            メッセージハンドラ
     */
    public static void addHandler(Class<?> cls, ErrorMessageHelper helper) {
        if (cls == null || helper == null) {
            throw new IllegalArgumentException();
        }
        handlers.put(cls, helper);
    }

    /**
     * 例外クラスに対するエラーメッセージハンドラを解除する.<br>
     * 例外クラスは完全一致でなければならない.(継承関係は考慮されない).<br>
     * 
     * @param cls
     *            クラス
     */
    public static void removeHandler(Class<?> cls) {
        if (cls != null) {
            handlers.remove(cls);
        }
    }
}
