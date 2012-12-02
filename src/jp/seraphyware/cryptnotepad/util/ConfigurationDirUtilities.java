package jp.seraphyware.cryptnotepad.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

import jp.seraphyware.cryptnotepad.Main;


/**
 * アプリケーションの設定ファイル等の位置を取得するユーテリティクラス.<br>
 * Mainクラスのロード時に最も早くロードされるであろうクラスの一つである.<br>
 * @author seraphy
 */
public final class ConfigurationDirUtilities {

	private static File userDataDir;
	
	private static File applicationBaseDir;
	
	private ConfigurationDirUtilities() {
		throw new RuntimeException("utilities class.");
	}
	
	/**
	 * ユーザーごとのアプリケーションデータ保存先を取得する.<br>
	 * 環境変数「APPDATA」もしくはシステムプロパティ「appdata.dir」からベース位置を取得する.<br>
	 * いずれも設定されておらず、Mac OS Xであれば「~/Library」をベース位置とする。
	 * Mac OS Xでなければ「~/」をベース位置とする.<br>
	 * これに対してMANIFEST.MFのImplementation-Titleの文字列をフォルダ名としたものを返す.
	 */
	public synchronized static File getUserDataDir() {
		if (userDataDir == null) {
			
			String appData = null;
			// システムプロパティ「appdata.dir」を探す
			appData = System.getProperty("appdata.dir");
			if (appData == null) {
				// なければ環境変数APPDATAを探す
				// Windows2000/XP/Vista/Windows7/8には存在する.
				appData = System.getenv("APPDATA");
			}
			if (appData == null && Main.isMacOSX()) {
				// システムプロパティも環境変数にも設定がなく、実行環境がMac OS Xであれば
				// ~/Libraryをベースにする.(Mac OS Xならば必ずある。)
				appData = new File(System.getProperty("user.home"), "Library").getPath();
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
				userDataDir = new File(appData, title).getAbsoluteFile().getCanonicalFile();

			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}

			// ディレクトリを準備する.
			if (!userDataDir.exists()) {
				if (!userDataDir.mkdirs()) {
					// ログ保管場所も設定されていないのでコンソールに出すしかない.
					System.err.println("can't create the user data directory. " + userDataDir);
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
			
			String appbaseDir = System.getProperty("appbase.dir");
			if (appbaseDir != null && appbaseDir.length() > 0) {
				// 明示的にアプリケーションベースディレクトリが指定されている場合.
				try {
					applicationBaseDir = new File(appbaseDir).getCanonicalFile();
				} catch (IOException ex) {
					ex.printStackTrace();
					// 継続する.まだログの準備ができていない可能性が高いので標準エラー出力へ.
				}
			}
			if (applicationBaseDir == null) {
				// 明示的に指定されていない場合はコードの実行位置から割り出す.
				URL codeBaseUrl = getClassLocation(ConfigurationDirUtilities.class);
				if (codeBaseUrl == null) {
					throw new RuntimeException("コード位置が取得できません");
				}
				// クラスパスフォルダ、またはJARファイルの、その親
				applicationBaseDir = new File(codeBaseUrl.getPath()).getParentFile();
			}
		}
		return applicationBaseDir;
	}
	
	/**
	 * Given a Class object, attempts to find its .class location [returns null
	 * if no such definition can be found]. Use for testing/debugging only.
	 * @see http://www.javaworld.com/javaqa/2003-07/01-qa-0711-classsrc.html
	 * @return URL that points to the class definition [null if not found].
	 */
	public static URL getClassLocation(final Class<?> cls) {
		if (cls == null)
			throw new IllegalArgumentException("null input: cls");

		URL result = null;
		final String clsAsResource = cls.getName().replace('.', '/')
				.concat(".class");

		final ProtectionDomain pd = cls.getProtectionDomain();
		// java.lang.Class contract does not specify if 'pd' can ever be null;
		// it is not the case for Sun's implementations, but guard against null
		// just in case:
		if (pd != null) {
			final CodeSource cs = pd.getCodeSource();
			// 'cs' can be null depending on the classloader behavior:
			if (cs != null) {
				result = cs.getLocation();
			}

			if (result != null) {
				// Convert a code source location into a full class file
				// location
				// for some common cases:
				if ("file".equals(result.getProtocol())) {
					try {
						if (result.toExternalForm().endsWith(".jar")
								|| result.toExternalForm().endsWith(".zip")) {
							result = new URL("jar:" + result.toExternalForm()
									+ "!/" + clsAsResource);
						} else if (new File(result.getFile()).isDirectory()) {
							result = new URL(result, clsAsResource);
						}
					} catch (MalformedURLException ignore) {
						// 無視する.
					}
				}
			}
		}

		if (result == null) {
			// Try to find 'cls' definition as a resource; this is not
			// documented to be legal, but Sun's implementations seem to allow
			// this:
			final ClassLoader clsLoader = cls.getClassLoader();

			result = (clsLoader != null) ? clsLoader.getResource(clsAsResource)
					: ClassLoader.getSystemResource(clsAsResource);
		}

		return result;
	}
}
