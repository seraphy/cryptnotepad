package jp.seraphyware.cryptnotepad.model;

import java.io.Serializable;
import java.util.Arrays;

/**
 * textでもimageでもないデータを復号化した場合のデータのコンテナ.<br>
 * 
 * @author seraphy
 */
public final class ApplicationData implements Serializable {

    private static final long serialVersionUID = -9023165507568809612L;

    /**
     * Content-Typeの情報.<br>
     * 必須.<br>
     */
    private final String contentType;

    /**
     * オリジナルのファイル名もしくはドキュメントタイトル.<br>
     * 必須.<br>
     */
    private final String documentTitle;

    /**
     * データ本体(バイナリ).<br>
     * バイナリが設定されている場合は常に非nullとなる.<br>
     */
    private final byte[] data;

    /**
     * データ本体(テキスト).<br>
     * テキストが設定されている場合は常に非nullとなる.<br>
     */
    private final String text;

    public ApplicationData(String contentType, byte[] data, String documentTitle) {
        if (contentType == null || contentType.length() == 0
                || documentTitle == null || documentTitle.trim().length() == 0) {
            throw new IllegalArgumentException();
        }
        this.contentType = contentType.trim();
        this.data = (data == null) ? new byte[0] : data;
        this.text = null;
        this.documentTitle = documentTitle.trim();
    }

    public ApplicationData(String contentType, String text, String documentTitle) {
        if (contentType == null || contentType.length() == 0
                || documentTitle == null || documentTitle.trim().length() == 0) {
            throw new IllegalArgumentException();
        }
        this.contentType = contentType.trim();
        this.data = null;
        this.text = (text == null) ? "" : text;
        this.documentTitle = documentTitle.trim();
    }

    /**
     * コンテントタイプを取得する.<br>
     * 
     * @return コンテントタイプ、null不可
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * ドキュメントタイトルを取得する.<br>
     * 
     * @return タイトル、null不可
     */
    public String getDocumentTitle() {
        return documentTitle;
    }

    public byte[] getData() {
        return data;
    }

    public String getText() {
        return text;
    }

    @Override
    public int hashCode() {
        if (text != null) {
            return text.hashCode();
        }
        if (data != null) {
            return Arrays.hashCode(data);
        }
        return contentType.hashCode() ^ documentTitle.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj != null && obj instanceof ApplicationData) {
            ApplicationData o = (ApplicationData) obj;
            if (contentType.equals(o.contentType)
                    && documentTitle.equals(o.documentTitle)) {
                if (text != null) {
                    return text.equals(o.text);
                }
                if (data != null) {
                    return Arrays.equals(data, o.data);
                }
            }
        }
        return false;
    }

    /**
     * アプリケーションデータのタイトルを変更した新しいオブジェクトを返す.<br>
     * 
     * @param newTitle
     *            新しいタイトル
     * @return 新しいオブジェクト
     */
    public ApplicationData changeDocumentTitle(String newTitle) {
        String contentType = getContentType();
        String text = getText();
        byte[] data = getData();
        if (text != null) {
            return new ApplicationData(contentType, text, newTitle);
        }
        return new ApplicationData(contentType, data, newTitle);
    }
}
