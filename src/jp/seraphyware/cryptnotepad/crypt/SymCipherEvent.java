package jp.seraphyware.cryptnotepad.crypt;

import java.io.File;
import java.util.EventObject;

/**
 * SymCipherのイベント.<br>
 * イベントハンドラ側からイベントソースに処理のキャンセルを通知できます.<br>
 * 
 * @author seraphy
 */
public class SymCipherEvent extends EventObject {

    private static final long serialVersionUID = 379443266102087924L;

    /**
     * 対象のファイル(暗号化)
     */
    protected File file;

    /**
     * 暗号化モード
     */
    protected boolean modeEncryption;

    /**
     * 例外(あれば)
     */
    protected Throwable cause;

    /**
     * キャンセル通知用
     */
    protected boolean cancel;

    public SymCipherEvent(SymCipher src, boolean modeEncryption, File file) {
        this(src, modeEncryption, file, null);
    }

    public SymCipherEvent(SymCipher src, boolean modeEncryption, File file,
            Throwable cause) {
        super(src);
        if (src == null || file == null) {
            throw new IllegalArgumentException();
        }
        this.file = file;
        this.modeEncryption = modeEncryption;
        this.cause = cause;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public boolean isModeEncryption() {
        return modeEncryption;
    }

    public void setModeEncryption(boolean modeEncryption) {
        this.modeEncryption = modeEncryption;
    }

    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    /**
     * キャンセルフラグをセットまたはリセットします.
     * 
     * @param cancel
     *            キャンセルフラグ
     */
    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }

    /**
     * キャンセルするか?
     * 
     * @return キャンセルする場合はtrue
     */
    public boolean isCancel() {
        return cancel;
    }

    /**
     * キャンセルします.<br>
     */
    public void cancel() {
        cancel = true;
    }
}
