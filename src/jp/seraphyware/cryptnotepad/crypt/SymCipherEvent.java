package jp.seraphyware.cryptnotepad.crypt;

import java.util.EventObject;

/**
 * SymCipherのイベント.<br>
 * イベントハンドラ側からイベントソースに処理のキャンセルを通知できます.<br>
 * 
 * @author seraphy
 */
public class SymCipherEvent extends EventObject {

    private static final long serialVersionUID = 379443266102087924L;

    protected boolean cancel;

    public SymCipherEvent(SymCipher src) {
        super(src);
        if (src == null) {
            throw new IllegalArgumentException();
        }
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
