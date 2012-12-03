package jp.seraphyware.cryptnotepad.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public final class ApplicationLoggerConfigurator {

    private static final String LOGGING_PROPERTIES = "logging.properties";

    private ApplicationLoggerConfigurator() {
        super();
    }

    public static void configure() {
        // ログマネージャ.
        // 初期時にJRE等にある初期設定がなされている.
        LogManager logManager = LogManager.getLogManager();

        // 設定を一旦クリアする.
        // ルートロガーがInfoになり、ハンドラはすべてリセット。ルートロガー以外の設定は空にされる.
        logManager.reset();

        Exception configurationError = null;
        try {
            // ユーザーごとのアプリケーション設定ディレクトリ上の設定ファイルを取得する.
            File appDataDir = ConfigurationDirUtilities.getUserDataDir();
            File logConfig = new File(appDataDir, LOGGING_PROPERTIES);

            if (!logConfig.exists()) {
                // ユーザ指定のロギングプロパティがない場合、リソースからコピーする
                copyDefaultLogProperty(logConfig);
            }

            InputStream is = new FileInputStream(logConfig);
            try {
                // ログを再設定する.
                logManager.readConfiguration(is);

            } finally {
                is.close();
            }

        } catch (Exception ex) {
            // 初期化に失敗した場合はログに記録するために例外を保存するが、
            // 処理は継続する.
            configurationError = ex;
        }

        // ロガーを取得
        Logger logger = Logger.getLogger(ApplicationLoggerConfigurator.class
                .getName());

        // 初期化時に失敗した場合、デフォルトのコンソールハンドラを設定し、ログに出力する.
        if (configurationError != null) {
            logger.addHandler(new ConsoleHandler());
            logger.addHandler(new ApplicationLogHandler());
            logger.log(Level.WARNING, "LogConfigurationFailed",
                    configurationError);
        }

        // 初期化時のログ
        logger.info("open logger.");
        logger.info("application configuration: baseDir="
                + ConfigurationDirUtilities.getApplicationBaseDir()
                + "  appData=" + ConfigurationDirUtilities.getUserDataDir());
    }

    /**
     * デフォルトのログプロパティをユーザディレクトリにコピーする.
     * 
     * @param logConfig
     *            ユーザディレクトリ上のログプロパティファイル位置
     */
    private static void copyDefaultLogProperty(File logConfig) {
        try {
            InputStream is = ApplicationLoggerConfigurator.class
                    .getResourceAsStream("/" + LOGGING_PROPERTIES);
            if (is != null) {
                try {
                    FileOutputStream fos = new FileOutputStream(logConfig);
                    try {
                        byte buf[] = new byte[4096];
                        for (;;) {
                            int rd = is.read(buf);
                            if (rd <= 0) {
                                break;
                            }
                            fos.write(buf, 0, rd);
                        }
                    } finally {
                        fos.close();
                    }
                } finally {
                    is.close();
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            // 失敗しても継続する
        }
    }
}
