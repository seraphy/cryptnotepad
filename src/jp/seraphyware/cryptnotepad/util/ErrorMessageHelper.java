package jp.seraphyware.cryptnotepad.util;

import java.awt.Component;
import java.awt.Dimension;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * 例外を表示するダイアログ.<br>
 * ログにも記録される.<br>
 * @author seraphy
 */
public final class ErrorMessageHelper {

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(ErrorMessageHelper.class.getName());

	
	private ErrorMessageHelper() {
		super();
	}

	/**
	 * 例外が発生したことを示すダイアログを表示し、ログに記録する.<br>
	 * @param parent ダイアログを表示する親、null可
	 * @param ex 例外、nullの場合はなにもせずに戻る.
	 */
	public static void showErrorDialog(Component parent, Throwable ex) {
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
		scr.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scr.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		// ダイアログの表示
		JOptionPane.showMessageDialog(parent, scr, "ERROR", JOptionPane.ERROR_MESSAGE);
	}

}
