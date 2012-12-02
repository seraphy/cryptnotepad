package jp.seraphyware.cryptnotepad.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;


/**
 * このアプリケーションの活動を記録するログハンドラ.<br>
 * アプリケーション用のディレクトリのlogsフォルダ下に開始日時のファイル名をもつログファイルを作成し、ログを記録する.<br>
 * ただし、終了時、警告以上のログが一度も書き込まれなかった場合はログファィルは自動的に削除される.<br>
 *  
 * @author seraphy
 */
public class ApplicationLogHandler extends Handler {

	private static final String LOGS_DIR = "logs";
	
	private final Object lock = new Object();
	
	private final File logFile;
	
	private PrintWriter pw;
	
	private boolean notRemove;
	
	public ApplicationLogHandler() {
		// 出力先の確定
		File appDir = ConfigurationDirUtilities.getUserDataDir();
		File logsDir = new File(appDir, LOGS_DIR);
		if (!logsDir.exists()) {
			if (!logsDir.mkdirs()) {
				// ログ記録場所が作成できていないのでコンソールに出すしかない.
				System.err.println("can't create the log directory. " + logsDir);
			}
		}
		
		// 出力ファイル名の確定
		String fname = getCurrentTimeForFileName() + ".log";
		logFile = new File(logsDir, fname);
		PrintWriter tmp;
		try {
			tmp = new PrintWriter(new OutputStreamWriter(new FileOutputStream(logFile)));

		} catch (Exception ex) {
			ex.printStackTrace(); // ロガーが失敗しているので、この失敗はコンソールに出すしかない。
			tmp = null;
		}
		this.pw = tmp;
	}
	
	@Override
	public void close() throws SecurityException {
		synchronized (lock) {
			// ログ設定ファイルより、ログファイルを自動消去しないか判定する.
			LogManager logManager = LogManager.getLogManager();
			String strNoRemoveLog = logManager.getProperty("noRemoveLog");
			if (strNoRemoveLog != null && Boolean.parseBoolean(strNoRemoveLog)) {
				notRemove = true;
			}

			if (pw != null) {
				pw.close();
				pw = null;
			}
			if (logFile != null && !notRemove) {
				// 警告未満のログしかないくログ消去を指定されている場合は、ログファイルを毎回削除する.
				if (!logFile.delete()) {
					System.err.println("can't delete file. " + logFile);
				}
			}
		}
	}
	
	@Override
	public void flush() {
		synchronized (lock) {
			if (pw != null) {
				pw.flush();
			}
		}
	}
	
	@Override
	public void publish(LogRecord record) {
		if (record == null) {
			return;
		}

		// メッセージの記録
		synchronized (lock) {
			if (pw == null) {
				return;
			}

			Level lv = record.getLevel();
			String name = record.getLoggerName();
			pw.println("#" + getCurrentTime() + " " + name + " "
					+ lv.getLocalizedName() + " " + record.getMessage());
			
			// 例外があれば、例外の記録
			Throwable tw = record.getThrown(); 
			if (tw != null) {
				tw.printStackTrace(pw); // 例外のコールスタックをロガーに出力
			}
			
			// フラッシュする.(随時、ファイルの中身を見ることができるように.)
			pw.flush();

			// 警告以上であれば終了時にファイルを消さない
			if (lv.intValue() >= Level.WARNING.intValue()) {
				notRemove = true;
			}
		}
	}
	
	/**
	 * 現在時刻の文字列
	 * @return 現在時刻の文字列
	 */
	public String getCurrentTime() {
		Timestamp tm = new Timestamp(System.currentTimeMillis());
		SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		return dt.format(tm);
	}

	/**
	 * 出力ファイル名
	 * @return 出力ファイル名
	 */
	public String getCurrentTimeForFileName() {
		Timestamp tm = new Timestamp(System.currentTimeMillis());
		SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd_HHmmssSSS");
		return dt.format(tm);
	}
}
