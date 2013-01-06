package jp.seraphyware.cryptnotepad.ui;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import jp.seraphyware.cryptnotepad.util.XMLResourceBundle;

/**
 * ファイルの保存時の上書き確認をサポートするファイルチューザの拡張.<br>
 * 
 * @author seraphy
 */
public class FileChooserEx extends JFileChooser {

    private static final long serialVersionUID = 184193496229675891L;

    /**
     * リソースバンドル
     */
    private ResourceBundle resource;

    /**
     * 上書き確認
     */
    private boolean confirmOverwrite;
    
    /**
     * 実在確認
     */
    private boolean checkExists;

    /**
     * コンストラクタ
     * 
     * @param rootDir
     *            初期ディレクトリ
     */
    public FileChooserEx(File rootDir) {
        super(rootDir);

        resource = ResourceBundle.getBundle(getClass().getName(),
                XMLResourceBundle.CONTROL);
    }

    public void setConfirmOverwrite(boolean confirmOverwrite) {
        this.confirmOverwrite = confirmOverwrite;
    }

    public boolean isConfirmOverwrite() {
        return confirmOverwrite;
    }
    
    public void setCheckExists(boolean checkExists) {
        this.checkExists = checkExists;
    }
    
    public boolean isCheckExists() {
        return checkExists;
    }

    /**
     * OKボタン押下時のハンドラ
     */
    @Override
    public void approveSelection() {
        if (confirmOverwrite) {
            File selectedFile = getSelectedFile();
            if (selectedFile != null && selectedFile.exists()) {
                String title = resource.getString("confirm.title");
                String message = resource.getString("confirm.overwrite");
                int ret = JOptionPane.showConfirmDialog(this, message, title,
                        JOptionPane.YES_NO_OPTION);
                if (ret != JOptionPane.YES_OPTION) {
                    return;
                }
            }
        }

        // ファイル名を正規化する.
        try {
            File selectedFile = getSelectedFile();
            if (selectedFile != null) {
                File canonicalFile = selectedFile.getCanonicalFile();
                setSelectedFile(canonicalFile);
            }

        } catch (IOException ex) {
            // 正しくないファイル名の場合は許可しない.
            return;
        }
        
        if (checkExists) {
            File selectedFile = getSelectedFile();
            if (selectedFile == null || !selectedFile.exists()) {
                // 実在しなければ許可しない.
                return;
            }
        }

        super.approveSelection();
    }

    /**
     * ファイルチューザを構築して返す.<br>
     * ファイルの実在チェック、もしくは上書き確認を行うように拡張している.<br>
     * saveモードであれば上書き確認、そうでなければ実在チェックを行う.<br>
     * 
     * @param rootDir
     *            初期ディレクトリ
     * @param save
     *            保存モードであればtrue.(上書き確認するか？)
     * @return ファイルチューザー
     */
    public static JFileChooser createFileChooser(File rootDir,
            boolean save) {
        FileChooserEx fileChooser = new FileChooserEx(rootDir);
        fileChooser.setConfirmOverwrite(save);
        fileChooser.setCheckExists(!save);
        return fileChooser;
    }

}
