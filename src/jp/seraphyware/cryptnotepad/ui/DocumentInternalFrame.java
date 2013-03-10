package jp.seraphyware.cryptnotepad.ui;

import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import jp.seraphyware.cryptnotepad.model.ApplicationData;
import jp.seraphyware.cryptnotepad.model.ApplicationSettings;
import jp.seraphyware.cryptnotepad.model.DocumentController;
import jp.seraphyware.cryptnotepad.util.ConfigurationDirUtilities;
import jp.seraphyware.cryptnotepad.util.XMLResourceBundle;

/**
 * MDI子ウィンドウの抽象基底クラス.<br>
 * 
 * @author seraphy
 */
public abstract class DocumentInternalFrame extends JInternalFrame {

    private static final long serialVersionUID = 2822332113293639341L;

    public static final String PROPERTY_FILE = "file";

    public static final String PROPERTY_DATA = "data";

    public static final String PROPERTY_MODIFIED = "modified";

    public static final String PROPERTY_READONLY = "readonly";

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
     * アプリケーションデータ
     */
    private ApplicationData data;

    /**
     * 変更フラグ
     */
    private boolean modified;

    /**
     * 変更不可フラグ
     */
    private boolean readonly;

    /**
     * インスタンス作成日時
     */
    private Date instanceCreationTime;

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
        
        // Mac用のL&F
        putClientProperty("JInternalFrame.frameType", "normal");

        // 閉じるイベントのハンドリング
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
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

        // インスタンス作成日時を設定する.
        instanceCreationTime = new Date();

        // ファイル名変更
        addPropertyChangeListener(PROPERTY_FILE, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateReadonlyFlag();
                updateTitle();
            }
        });
    }

    /**
     * 変更がある場合、破棄してもよいか確認する.<br>
     * 変更がなければ常にtrue
     * 
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
     * ファイル名からウィンドウのタイトルを設定する.<br>
     * ファイルが未指定で、一時タイトルが指定されてなければ"untitled"とする.<br>
     * また、更新フラグによってマーカーを付与する.<br>
     */
    protected final void updateTitle() {
        String title = getSuggestDocumentTitle();

        String marker = "";
        if (isModified()) {
            marker += "*"; // 更新済みマーク
        }
        if (isReadonly()) {
            marker += "%"; // 読み込み専用マーク
        }

        String path = "";
        if (file != null && file.exists()) {
            path = " (" + file.getPath() + ")";
        }

        setTitle(marker + title + path);
    }

    /**
     * ドキュメントのタイトルを取得する.<br>
     * ファイルが未指定で、一時タイトルが指定されてなければ"untitled"とする.<br>
     * 
     * @return タイトル
     */
    public final String getSuggestDocumentTitle() {
        String title;

        if (data != null) {
            // データが設定されていれば、ドキュメントタイトルも設定されている.
            title = data.getDocumentTitle();

        } else if (file != null) {
            // データはないが、ファイル名が明示されている場合
            title = file.getName();

        } else {
            // ファイルもデータも未指定の場合はデフォルト名
            title = resource.getString("notitled.title");

            // 現在日時を付与する.
            SimpleDateFormat dtFmt = new SimpleDateFormat("yyyyMMdd_HHmmss");
            title += dtFmt.format(instanceCreationTime);
        }
        return title;
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

    /**
     * アプリケーションデータを設定する.
     * 
     * @param data
     */
    public void setData(ApplicationData data) {
        ApplicationData oldValue = this.data;
        this.data = data;
        firePropertyChange(PROPERTY_DATA, oldValue, data);
        setModified(false);
    }

    /**
     * アプリケーションデータを取得する.
     * 
     * @return
     */
    public ApplicationData getData() {
        return data;
    }

    /**
     * 変更フラグを設定する.<br>
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
     * 変更不可フラグを設定する.
     * 
     * @param readonly
     */
    public void setReadonly(boolean readonly) {
        boolean oldValue = this.readonly;
        this.readonly = readonly;
        updateTitle();
        firePropertyChange(PROPERTY_READONLY, oldValue, readonly);
    }

    public boolean isReadonly() {
        return readonly;
    }

    /**
     * ファイルの状態から、変更不可フラグを更新する.
     */
    protected void updateReadonlyFlag() {
        setReadonly(file != null && !file.canWrite());
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
     * ファイルの保存.<br>
     * 
     * @param createOrUpdate
     *            新規ファイルのみ作成する場合はtrue、
     * @param modifiedOnly
     *            変更されているもののみ保存する場合はtrue
     * @throws IOException
     *             失敗
     */
    public void requestSave(boolean createOrUpdate, boolean modifiedOnly)
            throws IOException {
        if (modifiedOnly && !isModified()) {
            // 変更ファイルのみ保存する場合、変更されてなければ何もしない.
            return;
        }

        if (createOrUpdate) {
            // 新規保存の場合はファイルが存在しないものだけを保存する.
            if (!isExistFile()) {
                saveAs();
            }

        } else {
            // 更新保存の場合はファイルが存在するものだけを保存する.
            // (ただし、読み込み専用の場合は無視する.)
            if (isExistFile() && !isReadonly()) {
                // 上書き保存
                save();
            }
        }
    }

    /**
     * ファイルにデータを保存する.<br>
     * ファイル名とデータは設定済みでなければならない.<br>
     * 
     * @throws IOException
     *             失敗
     */
    protected void save() throws IOException {
        File file = getFile();
        ApplicationData data = getData();

        // 上書き保存する.
        save(file, data);

        // 保存済みにマークする.
        setModified(false);
    }

    /**
     * ファイルにデータを保存する.<br>
     * 
     * @param file
     *            保存先ファイル
     * @param data
     *            保存するデータ
     * @throws IOException
     *             失敗
     */
    protected void save(File file, ApplicationData data) throws IOException {
        if (file == null) {
            throw new IllegalStateException("file is not specified.");
        }
        if (data == null) {
            throw new IllegalStateException("no-data.");
        }

        // 外部ファイルのソルトの再計算が必要な場合には保存に時間がかかるため
        // ウェイトカーソルにする.
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            documentController.encrypt(file, data);

        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    /**
     * 別名保存します.<br>
     * このメソッドは新しいファイル名を選択し、そのファイル名に切り替えたのちに、
     * コールバックを呼び出し、それが成功したら、ファイル名変更のイベントを発生させます.
     * 
     * @param callback
     *            実際の保存を行うためのコールバック
     * @throws IOException
     *             失敗
     */
    protected void saveAs() throws IOException {
        // アプリケーションベース上のフォルダを既定とする.
        File rootDir = lastUseEncryptedDir;
        if (rootDir == null) {
            rootDir = ConfigurationDirUtilities.getApplicationBaseDir();
        }
        JFileChooser fileChooser = FileChooserEx.createFileChooser(rootDir,
                true);
        fileChooser.setDialogTitle(getSuggestDocumentTitle());

        // デフォルトのファイル名の選択
        if (file != null) {
            fileChooser.setSelectedFile(file);

        } else {
            // ファイル名が指定ないが、一時的なタイトルが設定されていれば
            // タイトルをファイル名に用いる.
            fileChooser.setSelectedFile(new File(getSuggestDocumentTitle()));
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

        // ドキュメントタイトルを差し替える
        ApplicationData data = getData();
        data = data.changeDocumentTitle(file.getName());
        setData(data);

        // 保存する.
        save(file, data);

        // 保存済みにする.
        setModified(false);

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
