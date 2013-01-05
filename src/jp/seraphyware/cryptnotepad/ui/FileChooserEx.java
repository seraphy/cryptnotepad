package jp.seraphyware.cryptnotepad.ui;

import java.io.File;
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
        super.approveSelection();
    }

    /**
     * ファイルチューザを構築して返す.<br>
     * 上書き確認を行うように拡張している.<br>
     * 
     * @param rootDir
     *            初期ディレクトリ
     * @param confirmOverwrite
     *            上書き確認するか？
     * @return ファイルチューザー
     */
    public static JFileChooser createFileChooser(File rootDir,
            boolean confirmOverwrite) {
        FileChooserEx fileChooser = new FileChooserEx(rootDir);
        fileChooser.setConfirmOverwrite(confirmOverwrite);
        return fileChooser;
    }

}
