package jp.seraphyware.cryptnotepad.model;

import java.io.Serializable;
import java.util.Arrays;


public class SettingsModel implements Serializable {

	private static final long serialVersionUID = -1494835954166827007L;

	private String encoding;
	
	private String keyFile;
	
	private volatile char[] passphrase = new char[0];
	
	public String getEncoding() {
		return encoding;
	}
	
	public String getKeyFile() {
		return keyFile;
	}
	
	public char[] getPassphrase() {
		return passphrase;
	}
	
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
	
	public void setKeyFile(String keyFile) {
		this.keyFile = keyFile;
	}
	
	public void setPassphrase(char[] passphrase) {
		if (passphrase == null) {
			passphrase = new char[0];
		}
	
		// メモリ上からパスフレーズを消し去る
		Arrays.fill(this.passphrase, '@');
		
		this.passphrase = passphrase;
	}
	
	public void clear() {
		setEncoding(null);
		setKeyFile(null);
		setPassphrase(null);
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(getClass().getSimpleName());
		buf.append("@");
		buf.append(Integer.toHexString(System.identityHashCode(this)));
		buf.append("(encoding=").append(encoding);
		buf.append(", keyFile=").append(keyFile);
		buf.append(", passphrase=").append(passphrase.length > 0 ? "***" : "");
		buf.append(")");
		return buf.toString();
	}
}
