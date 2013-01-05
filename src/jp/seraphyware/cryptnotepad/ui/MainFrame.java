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
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import jp.seraphyware.cryptnotepad.model.DocumentController;
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

        if (!documentController.getSettingsModel().isValid()) {
            String message = resource.getString("error.password.required");
            JOptionPane.showMessageDialog(this, message);
            return null;
        }

        TextInternalFrame internalFrame = new TextInternalFrame(
                documentController);

        internalFrame.addPropertyChangeListener(
                TextInternalFrame.PROPERTY_FILE, new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        fileTreePanel.refresh();
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

        // ファイルをロードする.
        // 外部URLのファイルハッシュつきの場合は復号化に時間がかかるのでウェイトカーソルをつける.
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            internalFrame.load(file);
            
        } finally {
            setCursor(Cursor.getDefaultCursor());
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
                    createChildFrame(file);
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
                onDelete();
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

    protected void onSettings() {
        SettingsDialog settingsDlg = new SettingsDialog(this);
        settingsDlg.setLocationRelativeTo(this);
        settingsDlg.setModel(documentController.getSettingsModel());
        settingsDlg.setVisible(true);
    }

    protected void onNew() {
        createChildFrame(null);
    }

    protected void onDelete() {
        JOptionPane.showMessageDialog(this, "Delete");
    }

    protected void onChangePw() {
        JOptionPane.showMessageDialog(this, "ChangePw");
    }

    /**
     * 破棄する場合
     */
    protected void onClosing() {
        documentController.dispose();
        dispose();
        logger.log(Level.INFO, "disposed mainframe");
    }
}
