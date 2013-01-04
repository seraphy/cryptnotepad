package jp.seraphyware.cryptnotepad.model;

import java.io.Serializable;


/**
 * textでもimageでもないデータを復号化した場合のデータのコンテナ.<br>
 * 
 * @author seraphy
 */
public final class ApplicationData implements Serializable {

    private static final long serialVersionUID = -9023165507568809612L;

    /**
     * Content-Typeの情報
     */
    private final String contentType;
    
    /**
     * データ本体
     */
    private final byte[] data;
    
    public ApplicationData(String contentType, byte[] data) {
        this.contentType = contentType;
        this.data = data;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public byte[] getData() {
        return data;
    }
}
