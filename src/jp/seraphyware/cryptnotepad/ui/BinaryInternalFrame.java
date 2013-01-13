package jp.seraphyware.cryptnotepad.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import jp.seraphyware.cryptnotepad.model.ApplicationData;
import jp.seraphyware.cryptnotepad.model.DocumentController;
import jp.seraphyware.cryptnotepad.util.ErrorMessageHelper;
import jp.seraphyware.cryptnotepad.util.XMLResourceBundle;

public class BinaryInternalFrame extends DocumentInternalFrame {

    private static final long serialVersionUID = 3954073407495314454L;

    /**
     * ロガー.<br>
     */
    private static final Logger logger = Logger
            .getLogger(BinaryInternalFrame.class.getName());

    /**
     * リソースバンドル
     */
    private ResourceBundle resource;

    /**
     * アプリケーションデータ
     */
    private ApplicationData data;

    /**
     * 暗号化ファイルをワークに平文でロードする.
     */
    private Action actLoad;

    /**
     * ワークにロードされたファイルを開く
     */
    private Action actOpen;

    /**
     * ワークファイルを暗号化して保存する.
     */
    private Action actSave;

    /**
     * ワークファイルを安全に削除する.
     */
    private Action actClose;

    /**
     * ソースファイル表示用
     */
    private JTextField txtSourceFile;

    /**
     * ワークファイル表示用
     */
    private JTextField txtWorkingFile;

    /**
     * ステータス表示用
     */
    private JTextField txtStatus;

    /**
     * コンストラクタ
     * 
     * @param documentController
     */
    public BinaryInternalFrame(DocumentController documentController) {
        super(documentController);

        this.resource = ResourceBundle.getBundle(getClass().getName(),
                XMLResourceBundle.CONTROL);

        updateTitle();

        // アクションの定義
        actLoad = new AbstractAction(resource.getString("load.button.title")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub

            }
        };
        actOpen = new AbstractAction(resource.getString("open.button.title")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub

            }
        };
        actSave = new AbstractAction(resource.getString("save.button.title")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                // 上書き保存
                onSave();
            }
        };
        actClose = new AbstractAction(resource.getString("close.button.title")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub

            }
        };

        // コンポーネント作成
        txtSourceFile = new JTextField();
        txtSourceFile.setEditable(false);

        txtWorkingFile = new JTextField();
        txtWorkingFile.setEditable(false);

        txtStatus = new JTextField();
        txtStatus.setEditable(false);

        String txtFieldTooltip = resource.getString("textField.tooltip");
        txtSourceFile.setToolTipText(txtFieldTooltip);
        txtWorkingFile.setToolTipText(txtFieldTooltip);
        txtStatus.setToolTipText(txtFieldTooltip);

        // キーボードマップ

        ActionMap am = this.getActionMap();
        InputMap im = this.getInputMap(JComponent.WHEN_FOCUSED);

        Toolkit tk = Toolkit.getDefaultToolkit();
        int shortcutMask = tk.getMenuShortcutKeyMask();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutMask), actSave);
        am.put(actSave, actSave);

        // レイアウト
        GridBagConstraints gbc = new GridBagConstraints();
        JPanel centerPnl = new JPanel();
        centerPnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        centerPnl.setLayout(new GridBagLayout());

        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        String sourceFileMsg = resource.getString("sourceFile.label.text");
        centerPnl.add(new JLabel(sourceFileMsg), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        String workingFileMsg = resource.getString("workingFile.label.text");
        centerPnl.add(new JLabel(workingFileMsg), gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        String statusLabelMsg = resource.getString("status.label.text");
        centerPnl.add(new JLabel(statusLabelMsg), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.;
        centerPnl.add(txtSourceFile, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.;
        centerPnl.add(txtWorkingFile, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.;
        centerPnl.add(txtStatus, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 0;
        centerPnl.add(new JButton(actLoad), gbc);

        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 0;
        centerPnl.add(new JButton(actOpen), gbc);

        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.weightx = 0;
        centerPnl.add(new JButton(actSave), gbc);

        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.weightx = 0;
        centerPnl.add(new JButton(actClose), gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        gbc.weighty = 1.;
        centerPnl.add(new JPanel(), gbc);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(centerPnl, BorderLayout.CENTER);

        addPropertyChangeListener(PROPERTY_FILE, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                // ファイルが設定された場合
                File file = getFile();
                txtSourceFile.setText((file == null) ? "" : file
                        .getAbsolutePath());
            }
        });

        // 各テキストフィールドをダブルクリックした場合、そのテキストをクリップボードにコピーする.
        final Clipboard cb = tk.getSystemClipboard();
        JTextField[] txtFields = { txtSourceFile, txtWorkingFile, txtStatus };
        for (final JTextField txtField : txtFields) {
            txtField.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        // ダブルクリックの場合
                        String text = txtField.getText();
                        cb.setContents(new StringSelection(text), null);
                    }
                }
            });
        }

        setModified(false);
    }

    public void setData(ApplicationData data) {
        ApplicationData oldValue = this.data;
        this.data = data;

        // TODO:loadPicture();

        firePropertyChange("data", oldValue, data);

        setModified(false);
    }

    public ApplicationData getData() {
        return data;
    }

    /**
     * ファイルを上書き保存する.
     * 
     * @throws IOException
     */
    protected void onSave() {
        try {
            File file = getFile();
            assert file != null;

            if (data == null) {
                return;
            }

            String contentType = data.getContentType();
            byte[] buf = data.getData();

            // 外部ファイルのソルトの再計算が必要な場合には保存に時間がかかるため
            // ウェイトカーソルにする.
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            try {
                documentController.encrypt(file, buf, contentType);

            } finally {
                setCursor(Cursor.getDefaultCursor());
            }

            setModified(false);

        } catch (Exception ex) {
            ErrorMessageHelper.showErrorDialog(this, ex);
        }
    }

    /**
     * ファイルを別名保存する.
     * 
     * @throws IOException
     */
    protected void onSaveAs() {
        saveAs(new Runnable() {
            @Override
            public void run() {
                onSave();
            }
        });
    }

    /**
     * テキストを平文で外部ファイルにエクスポートする.
     */
    protected void onExport() {
        try {
            File file = showExportDialog();
            if (file != null && data != null) {
                byte[] buf = data.getData();
                documentController.savePlainBinary(file, buf);
            }

        } catch (Exception ex) {
            ErrorMessageHelper.showErrorDialog(this, ex);
        }
    }
}