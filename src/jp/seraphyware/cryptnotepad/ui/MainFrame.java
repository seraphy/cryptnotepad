package jp.seraphyware.cryptnotepad.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.File;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import jp.seraphyware.cryptnotepad.crypt.CryptUtils;
import jp.seraphyware.cryptnotepad.model.DocumentController;
import jp.seraphyware.cryptnotepad.util.ErrorMessageHelper;
import jp.seraphyware.cryptnotepad.util.XMLResourceBundle;

/**
 * メインフレーム
 * 
 * @author seraphy
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 8358190990080417295L;

    /**
     * ロガー.<br>
     */
    private static final Logger logger = Logger
            .getLogger(TextInternalFrame.class.getName());

    /**
     * リソースバンドル
     */
    private ResourceBundle resource;

    /**
     * ドキュメントコントローラ
     */
    private DocumentController documentController;

    /**
     * MDIフレーム(デスクトップ)
     */
    private JDesktopPane desktop;

    /**
     * ファイルツリーパネル
     */
    private FileTreePanel fileTreePanel;

    /**
     * コンストラクタ
     */
    public MainFrame(DocumentController documentController) {
        try {
            if (documentController == null) {
                throw new IllegalArgumentException();
            }

            this.documentController = documentController;

            resource = ResourceBundle.getBundle(getClass().getName(),
                    XMLResourceBundle.CONTROL);
            init();

        } catch (RuntimeException ex) {
            dispose();
            throw ex;
        }
    }

    /**
     * フレームを初期化する.
     */
    private void init() {
        // ウィンドウの閉じるイベント
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClosing();
            }
        });

        // タイトル
        setTitle(resource.getString("mainframe.title"));

        // MDIフレーム
        desktop = new JDesktopPane();
        desktop.setBackground(Color.lightGray);

        // ファイル一覧パネル
        fileTreePanel = new FileTreePanel();
        JPanel leftPanel = createFileTreePanel(fileTreePanel);

        // レイアウト
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(200);

        splitPane.add(leftPanel);
        splitPane.add(desktop);

        contentPane.add(splitPane);
    }

    /**
     * MDI子ウィンドウを開く
     * 
     * @param file
     *            対象ファイル
     * @return 生成された子ウィンドウ、生成できなければnull
     */
    protected JInternalFrame createChildFrame(File file) {

        // パスフレーズの有無を確認する.
        while (!documentController.getSettingsModel().isValid()) {
            // パスフレーズが未設定であればエラー表示し、設定画面を開くか問い合わせる.
            String message = resource.getString("error.password.required");
            String title = resource.getString("confirm.title");
            int ret = JOptionPane.showConfirmDialog(this, message, title,
                    JOptionPane.YES_NO_OPTION);
            if (ret != JOptionPane.YES_OPTION) {
                // 設定画面を開かない場合は、ここで終了.
                return null;
            }
            // 設定画面を開く.(モーダル)
            onSettings();
        }

        String doc;
        try {
            // ファイルをロードする.
            // 外部URLのファイルハッシュつきの場合は復号化に時間がかかるのでウェイトカーソルをつける.
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            try {
                doc = (String) documentController.decrypt(file);

            } finally {
                setCursor(Cursor.getDefaultCursor());
            }

        } catch (Exception ex) {
            // ロードに失敗したらエラー表示し、ウィンドウは開かない.
            ErrorMessageHelper.showErrorDialog(this, ex);
            return null;
        }

        // テキスト編集用の子ウィンドウを作成する.
        final TextInternalFrame internalFrame = new TextInternalFrame(
                documentController);

        // テキストとファイル名を設定する.
        internalFrame.setText(doc);
        internalFrame.setFile(file);

        // ファイル名が変更されたら通知を受け取るようにリスナを設定する.
        internalFrame.addPropertyChangeListener(
                TextInternalFrame.PROPERTY_FILE, new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        // ドキュメントのファイル名が変更された場合
                        onChangeFileName(internalFrame,
                                (File) evt.getOldValue(),
                                (File) evt.getNewValue());
                    }
                });

        desktop.add(internalFrame);

        internalFrame.setSize(200, 200);
        internalFrame.setLocation(0, 0);
        internalFrame.setVisible(true);

        try {
            internalFrame.setMaximum(true);

        } catch (PropertyVetoException ex) {
            logger.log(Level.FINE, ex.toString());
        }

        return internalFrame;
    }

    /**
     * ファイル一覧パネルを作成する.
     * 
     * @return
     */
    private JPanel createFileTreePanel(final FileTreePanel fileTreePanel) {

        fileTreePanel.setBorder(BorderFactory.createTitledBorder(resource
                .getString("files.border.title")));

        fileTreePanel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File file = fileTreePanel.getSelectedFile();
                if (file != null) {
                    onOpenFile(file);
                }
            }
        });

        final JPanel leftPanel = new JPanel(new BorderLayout());

        JButton btnSettings = new JButton(new AbstractAction(
                resource.getString("settings.button.title")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                onSettings();
            }
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnNew = new JButton(new AbstractAction(
                resource.getString("new.button.title")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                onNew();
            }
        });
        JButton btnDelete = new JButton(new AbstractAction(
                resource.getString("delete.button.title")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if ((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
                    // シフトキーとともにある場合
                    onDeleteAny();

                } else {
                    // 通常
                    File file = fileTreePanel.getFocusedFile();
                    onDelete(file);
                }
            }
        });
        JButton btnChangePw = new JButton(new AbstractAction(
                resource.getString("password.button.title")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                onChangePw();
            }
        });
        btnPanel.add(btnNew);
        btnPanel.add(btnDelete);
        btnPanel.add(btnChangePw);

        leftPanel.add(btnSettings, BorderLayout.NORTH);
        leftPanel.add(fileTreePanel, BorderLayout.CENTER);
        leftPanel.add(btnPanel, BorderLayout.SOUTH);

        return leftPanel;
    }

    /**
     * 設定ボタンハンドラ.<br>
     * パスフレーズやキーファイル、文字コードなどの設定を行う.<br>
     */
    protected void onSettings() {
        SettingsDialog settingsDlg = new SettingsDialog(this);
        settingsDlg.setLocationRelativeTo(this);
        settingsDlg.setModel(documentController.getSettingsModel());
        settingsDlg.setVisible(true);
    }

    /**
     * ドキュメント名が変更されたことを通知される.
     * 
     * @param internalFrame
     *            ドキュメントのウィンドウ
     * @param oldFile
     *            以前のファイル名、なければnull
     * @param newFile
     *            新しいファイル名
     */
    protected void onChangeFileName(JInternalFrame internalFrame, File oldFile,
            File newFile) {
        // ドキュメントツリーをリフレッシュする.
        fileTreePanel.refresh();
    }

    /**
     * 新規用にテキストウィンドウを開く.
     */
    protected void onNew() {
        onOpenFile(null);
    }

    /**
     * 暗号化されたテキストドキュメント用のウィンドウを開く.
     * 
     * @param file
     *            ファイル (nullの場合は新規ドキュメント用)
     */
    protected void onOpenFile(File file) {
        for (JInternalFrame child : desktop.getAllFrames()) {
            if (!child.isDisplayable() || !child.isVisible()) {
                // 表示されていないか、破棄されたものは除外する.
                continue;
            }
            if (child instanceof TextInternalFrame) {
                TextInternalFrame c = (TextInternalFrame) child;
                File docFile = c.getFile();
                if (docFile != null && docFile.equals(file)) {
                    // すでに同ファイルがオープンされていれば、それをアクティブにする.
                    // (別名保存などで複数同名ファイルが開かれた状態の場合は最初の一つ)
                    desktop.getDesktopManager().activateFrame(c);
                    return;
                }
            }
        }

        // まだファイルが開かれていなければ開く.
        createChildFrame(file);
    }

    /**
     * 最後に使用したファイル削除ディレクトリ.<br>
     * なればnull.<br>
     */
    private File lastUseDeleteDir;

    /**
     * 任意のファイルを選択して削除する.
     */
    protected void onDeleteAny() {
        JFileChooser fileChooser = FileChooserEx.createFileChooser(
                lastUseDeleteDir, false);

        // 複数選択可
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setDialogTitle(resource.getString("secureerase.title"));

        int ret = fileChooser.showOpenDialog(this);
        if (ret != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File[] files = fileChooser.getSelectedFiles();
        if (files.length == 0) {
            // 選択なし
            return;
        }

        // 確認ダイアログ
        String title = resource.getString("confirm.title");
        String messageTmpl = resource.getString("confirm.erase.file");
        String message = String.format(messageTmpl,
                String.format("%d files", files.length));

        ret = JOptionPane.showConfirmDialog(this, message, title,
                JOptionPane.YES_NO_OPTION);
        if (ret == JOptionPane.YES_OPTION) {
            // 選択されたファイルをすべて削除する.
            for (File file : files) {
                try {
                    if (file.exists() && !file.isDirectory()) {
                        lastUseDeleteDir = file.getParentFile();
                        CryptUtils.erase(file);
                    }

                } catch (Exception ex) {
                    ErrorMessageHelper.showErrorDialog(this, ex);
                }
            }
            fileTreePanel.refresh();
        }
    }

    /**
     * ファイルの削除
     */
    protected void onDelete(File file) {
        if (file == null || file.isDirectory()) {
            return;
        }

        String title = resource.getString("confirm.title");
        String messageTmpl = resource.getString("confirm.erase.file");
        String message = String.format(messageTmpl, file.getName());

        int ret = JOptionPane.showConfirmDialog(this, message, title,
                JOptionPane.YES_NO_OPTION);
        if (ret == JOptionPane.YES_OPTION) {
            try {
                CryptUtils.erase(file);
                fileTreePanel.refresh();

            } catch (Exception ex) {
                ErrorMessageHelper.showErrorDialog(this, ex);
            }
        }
    }

    protected void onChangePw() {
        JOptionPane.showMessageDialog(this, "ChangePw");
    }

    /**
     * 破棄する場合
     */
    protected void onClosing() {
        // 変更されていない子ウィンドウがあるか検査する.
        boolean needConfirm = false;
        for (JInternalFrame child : desktop.getAllFrames()) {
            if (!child.isDisplayable() || !child.isVisible()) {
                // 表示できないか、表示されていないものは除外する.
                continue;
            }
            if (child instanceof TextInternalFrame) {
                TextInternalFrame c = (TextInternalFrame) child;
                if (c.isModified()) {
                    needConfirm = true;
                    break;
                }
            }
        }

        // 変更を破棄してよいか確認する.
        if (needConfirm) {
            String message = resource.getString("confirm.close.unsavedchanges");
            String title = resource.getString("confirm.title");
            int ret = JOptionPane.showConfirmDialog(this, message, title,
                    JOptionPane.YES_NO_OPTION);
            if (ret != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // ドキュメントコントローラを破棄する.
        // (パスフレーズなどをメモリから除去する.)
        documentController.dispose();

        // メインフレームを破棄する.
        dispose();
        logger.log(Level.INFO, "disposed mainframe");
    }
}
