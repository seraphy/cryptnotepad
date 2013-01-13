package jp.seraphyware.cryptnotepad.ui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyVetoException;
import java.io.File;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import jp.seraphyware.cryptnotepad.model.ApplicationSettings;
import jp.seraphyware.cryptnotepad.model.DocumentController;
import jp.seraphyware.cryptnotepad.util.ConfigurationDirUtilities;
import jp.seraphyware.cryptnotepad.util.XMLResourceBundle;

/**
 * MDI子ウィンドウの抽象基底クラス.<br>
 * 
 * @author seraphy
 */
public class DocumentInternalFrame extends JInternalFrame {

    private static final long serialVersionUID = 2822332113293639341L;

    public static final String PROPERTY_FILE = "file";

    public static final String PROPERTY_TEMPORARY_TITLE = "temporaryTitle";

    public static final String PROPERTY_MODIFIED = "modified";

    /**
     * アプリケーション設定
     */
    protected ApplicationSettings appConfig;

    /**
     * リソースバンドル
     */
    private ResourceBundle resource;

    /**
     * ドキュメントコントローラ
     */
    protected DocumentController documentController;

    /**
     * 対象ファイル、新規の場合はnull
     */
    private File file;

    /**
     * 一時的なタイトル.<br>
     * (ファイルがnullの場合に用いられる.)<br>
     */
    private String temporaryTitle;

    /**
     * 変更フラグ
     */
    private boolean modified;

    /**
     * コンストラクタ.<br>
     * 
     * @param documentController
     *            ドキュメント制御オブジェクト
     */
    protected DocumentInternalFrame(DocumentController documentController) {
        if (documentController == null) {
            dispose();
            throw new IllegalArgumentException();
        }

        // 閉じるイベントのハンドリング
        setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
        addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameClosing(InternalFrameEvent e) {
                onClosing();
            }
        });

        // メンバ初期化
        this.documentController = documentController;
        this.appConfig = ApplicationSettings.getInstance();
        this.resource = ResourceBundle.getBundle(
                DocumentInternalFrame.class.getName(),
                XMLResourceBundle.CONTROL);

        // 最大化・サイズ変更・最小化、閉じるボタンを許可する.
        setMaximizable(true);
        setResizable(true);
        setIconifiable(true);
        setClosable(true);

        /**
         * 閉じるショートカット.
         */
        AbstractAction actClose = new AbstractAction("close") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                onClosing();
            }
        };

        ActionMap am = this.getActionMap();
        InputMap im = this
                .getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        Toolkit tk = Toolkit.getDefaultToolkit();
        int shortcutMask = tk.getMenuShortcutKeyMask();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, shortcutMask), actClose);
        am.put(actClose, actClose);
    }

    /**
     * 変更がある場合、破棄してもよいか確認する.<br>
     * 変更がなければ常にtrue
     * @return 変更がないか、破棄してもよければtrue
     */
    protected boolean checkModify() {
        if (isModified()) {
            String message = resource.getString("confirm.close.unsavedchanges");
            String title = resource.getString("confirm.title");
            int ret = JOptionPane.showConfirmDialog(this, message, title,
                    JOptionPane.YES_NO_OPTION);
            if (ret != JOptionPane.YES_OPTION) {
                // 破棄しない場合はfalseを返す
                return false;
            }
        }
        return true;
    }
    
    /**
     * ウィンドウを閉じる.
     */
    protected void onClosing() {
        // 変更があれば破棄するか確認する.
        if (!checkModify()) {
            return;
        }

        // 閉じる.
        try {
            fireVetoableChange(IS_CLOSED_PROPERTY, Boolean.FALSE, Boolean.TRUE);
            isClosed = true;
            setVisible(false);
            firePropertyChange(IS_CLOSED_PROPERTY, Boolean.FALSE, Boolean.TRUE);
            dispose();

        } catch (PropertyVetoException pve) {
            // 無視する.
        }
    }

    /**
     * ファイル名からウィンドウのタイトルを設定する. ファイルが未指定であれば"untitled"とする.
     */
    protected final void updateTitle() {
        String title;
        if (file != null) {
            title = file.getName();

        } else if (temporaryTitle != null) {
            // ファイルが未指定だが、一時的なタイトルが指定されている場合
            title = temporaryTitle;

        } else {
            // ファイルが未指定で一時的なタイトルも設定されていない場合はデフォルト名
            title = resource.getString("notitled.title");
        }

        String marker = "";
        if (isModified()) {
            marker = "*";
        }
        setTitle(marker + title);
    }

    /**
     * 現在のファイル、未指定であればnull
     * 
     * @return ファイル
     */
    public File getFile() {
        return file;
    }

    /**
     * 現在のファイル名を設定する.<br>
     * タイトルも変更される.
     * 
     * @param file
     *            ファイル
     */
    public void setFile(File file) {
        File oldValue = this.file;
        this.file = file;

        updateTitle();

        firePropertyChange(PROPERTY_FILE, oldValue, file);
    }

    public String getTemporaryTitle() {
        return temporaryTitle;
    }

    /**
     * 一時的なタイトルを設定する.<br>
     * ファイル名が未指定の場合に用いられる.<br>
     * その場合、タイトルも変更される.<br>
     * 
     * @param temporaryTitle
     */
    public void setTemporaryTitle(String temporaryTitle) {
        if (temporaryTitle != null) {
            // タイトルはトリムされる.
            temporaryTitle = temporaryTitle.trim();
            if (temporaryTitle.length() == 0) {
                // トリム後、空文字になる場合はnull
                temporaryTitle = null;
            }
        }

        assert temporaryTitle == null || temporaryTitle.trim().length() > 0;
        String oldValue = this.temporaryTitle;
        this.temporaryTitle = temporaryTitle;

        updateTitle();

        firePropertyChange(PROPERTY_TEMPORARY_TITLE, oldValue, temporaryTitle);
    }

    /**
     * 変更フラグを更新する.<br>
     * タイトルの変更マーカーも変更される.
     * 
     * @param modified
     */
    public void setModified(boolean modified) {
        boolean oldValue = this.modified;
        this.modified = modified;
        updateTitle();
        firePropertyChange(PROPERTY_MODIFIED, oldValue, modified);
    }

    public boolean isModified() {
        return modified;
    }

    /**
     * このドキュメントが実在するファイルに関連づけられているか?
     * 
     * @return 関連づけられている場合はtrue
     */
    public boolean isExistFile() {
        return file != null && file.exists();
    }

    /**
     * 最後に使用した暗号化用ディレクトリ.<br>
     * (永続化はしない.)
     */
    private static File lastUseEncryptedDir;

    public static File getLastUseEncryptedDir() {
        return lastUseEncryptedDir;
    }

    public static void setLastUseEncryptedDir(File lastUseEncryptedDir) {
        DocumentInternalFrame.lastUseEncryptedDir = lastUseEncryptedDir;
    }

    /**
     * 別名保存します.<br>
     * このメソッドは新しいファイル名を選択し、そのファイル名に切り替えたのちに、
     * コールバックを呼び出し、それが成功したら、ファイル名変更のイベントを発生させます.
     * 
     * @param callback
     *            実際の保存を行うためのコールバック
     */
    protected void saveAs(Runnable callback) {
        if (callback == null) {
            throw new IllegalArgumentException();
        }

        // アプリケーションベース上のフォルダを既定とする.
        File rootDir = lastUseEncryptedDir;
        if (rootDir == null) {
            rootDir = ConfigurationDirUtilities.getApplicationBaseDir();
        }
        JFileChooser fileChooser = FileChooserEx.createFileChooser(rootDir,
                true);

        // デフォルトのファイル名の選択
        if (file != null) {
            fileChooser.setSelectedFile(file);

        } else if (temporaryTitle != null) {
            // ファイル名が指定ないが、一時的なタイトルが設定されていれば
            // タイトルをファイル名に用いる.
            fileChooser.setSelectedFile(new File(temporaryTitle));
        }

        int ret = fileChooser.showSaveDialog(this);
        if (ret != JFileChooser.APPROVE_OPTION) {
            // OK以外
            return;
        }

        File file = fileChooser.getSelectedFile();
        lastUseEncryptedDir = file.getParentFile();

        // 新しいファイル名を設定する.
        File oldValue = this.file;
        this.file = file;
        updateTitle();

        // 実際の保存処理はコールバックで行う.
        callback.run();

        // ファイル名変更を通知する.
        firePropertyChange(PROPERTY_FILE, oldValue, file);
    }

    /**
     * 外部ファイルへの保存ダイアログを開き、保存先ファイル名を返す.<br>
     * ダイアログがキャンセルされた場合はnullを返す.<br>
     * 
     * @return 外部ファイル、もしくはnull
     */
    protected File showExportDialog() {
        JFileChooser fileChooser = FileChooserEx.createFileChooser(
                appConfig.getLastUseDir(), true);
        if (file != null) {
            String name = file.getName();
            fileChooser.setSelectedFile(new File(name));
        }

        int ret = fileChooser.showSaveDialog(this);
        if (ret != JFileChooser.APPROVE_OPTION) {
            // OK以外
            return null;
        }

        File file = fileChooser.getSelectedFile();
        appConfig.setLastUseDir(file.getParentFile());

        return file;
    }
}
