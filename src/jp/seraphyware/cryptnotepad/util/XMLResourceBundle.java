package jp.seraphyware.cryptnotepad.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * XMLリソースバンドル
 * @author seraphy
 */
public class XMLResourceBundle extends ResourceBundle {

	protected static class XMLResourceBundleControl extends ResourceBundle.Control {
		
		/**
		 * 対応拡張子
		 */
		private static final List<String> FORMATS = Collections
				.unmodifiableList(Arrays.asList("xml"));

		@Override
		public List<String> getFormats(String baseName) {
			if (baseName == null) {
				throw new IllegalArgumentException();
			}
			return FORMATS;
		}
		
		@Override
		public ResourceBundle newBundle(
				String baseName,
				Locale locale,
				String format,
				ClassLoader loader,
				boolean reload
				) throws IllegalAccessException, InstantiationException, IOException {
			if (baseName == null || locale == null || format == null
					|| loader == null) {
				throw new IllegalArgumentException();
			}
			if (FORMATS.contains(format)) {
				String bundleName = toBundleName(baseName, locale);
	            String resourceName = toResourceName(bundleName, format);			

	            InputStream is = loader.getResourceAsStream(resourceName);
	            if (is != null) {
            		return new XMLResourceBundle(new BufferedInputStream(is));
	            }
			}
		
			return null;
		}
	}

	/**
	 * Control
	 */
	public static final XMLResourceBundleControl CONTROL = new XMLResourceBundleControl();
	
	/**
	 * XMLプロパティ
	 */
	private Properties props = new Properties();
	
	/**
	 * コンストラクタ
	 * @param is XMLプロパティの入力ストリーム
	 * @throws IOException 失敗
	 */
	public XMLResourceBundle(InputStream is) throws IOException {
		if (is == null) {
			throw new IllegalArgumentException();
		}
		// The specified stream is closed after this method returns. (on jdk1.6)
		props.loadFromXML(is);
	}
	
	@Override
	protected Object handleGetObject(String key) {
		if (key == null) {
			throw new IllegalArgumentException();
		}
		Object ret = props.getProperty(key);
		if (ret == null && parent != null) {
			ret = parent.getString(key);
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Enumeration<String> getKeys() {
		return (Enumeration<String>)props.propertyNames();
	}
}
