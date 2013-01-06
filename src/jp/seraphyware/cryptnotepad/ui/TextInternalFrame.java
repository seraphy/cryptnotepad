package jp.seraphyware.cryptnotepad.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Event;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoManager;

import jp.seraphyware.cryptnotepad.model.ApplicationSettings;
import jp.seraphyware.cryptnotepad.model.DocumentController;
import jp.seraphyware.cryptnotepad.util.ConfigurationDirUtilities;
import jp.seraphyware.cryptnotepad.util.ErrorMessageHelper;
import jp.seraphyware.cryptnotepad.util.XMLResourceBundle;

/**
 * テキストのドキュメントフレーム
 * 
 * @author seraphy
 */
public class TextInternalFrame extends JInternalFrame {

    private static final long serialVersionUID = -6664897509335391245L;

    public static final String PROPERTY_FILE = "file";

    public static final String PROPERTY_TEMPORARY_TITLE = "temporaryTitle";

    public static final String PROPERTY_MODIFIED = "modified";

    /**
     * アプリケーション設定
     */
    private ApplicationSettings appConfig;

    /**
     * ドキュメントコントローラ
     */
    private DocumentController documentController;

    /**
     * リソースバンドル
     */
    private ResourceBundle resource;

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
     * テキストエリア
     */
    private JTextArea area;

    /**
     * Undoマネージャ
     */
    private UndoManager undoManager;

    /**
     * 変更フラグ
     */
    private boolean modified;

    /**
     * コンストラクタ
     * 
     * @param documentController
     * @param file
     */
    public TextInternalFrame(DocumentController documentController) {
        if (documentController == null) {
            dispose();
            throw new IllegalArgumentException();
        }

        setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
        addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameClosing(InternalFrameEvent e) {
                onClosing();
            }
        });

        this.documentController = documentController;
        this.appConfig = ApplicationSettings.getInstance();
        this.resource = ResourceBundle.getBundle(getClass().getName(),
                XMLResourceBundle.CONTROL);

        updateTitle();

        setMaximizable(true);
        setResizable(true);
        setIconifiable(true);
        setClosable(true);

        this.area = new JTextArea();
        this.area.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                setModified(true);
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                setModified(true);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setModified(true);
            }
        });

        // フォントを適用する.
        String fontName = appConfig.getFontName();
        int fontSize = appConfig.getFontSize();
        if (fontName != null && fontName.trim().length() > 0 && fontSize > 0) {
            // フォントを適用する.
            Font newFont = new Font(fontName, Font.PLAIN, fontSize);
            area.setFont(newFont);
        }

        // Undo/Redoに対応する.
        this.undoManager = new UndoManager();

        ActionMap am = this.area.getActionMap();
        InputMap im = this.area.getInputMap(JComponent.WHEN_FOCUSED);

        AbstractAction actUndo = new AbstractAction("undo") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canUndo()) {
                    undoManager.undo();
                }
            }
        };
        AbstractAction actRedo = new AbstractAction("redo") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canRedo()) {
                    undoManager.redo();
                }
            }
        };

        final AbstractAction actSave = new AbstractAction(
                resource.getString("save.button.title")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (TextInternalFrame.this.file == null
                            || (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
                        // 新規ドキュメントであるか、シフトキーとともに押された場合
                        onSaveAs();
                    } else {
                        // 上書き保存
                        onSave();
                    }
                } catch (Exception ex) {
                    ErrorMessageHelper.showErrorDialog(TextInternalFrame.this,
                            ex);
                }
            }
        };

        im.put(KeyStroke.getKeyStroke('Z', Event.CTRL_MASK), actUndo);
        am.put(actUndo, actUndo);
        im.put(KeyStroke.getKeyStroke('Y', Event.CTRL_MASK), actRedo);
        am.put(actRedo, actRedo);

        im.put(KeyStroke.getKeyStroke('S', Event.CTRL_MASK), actSave);
        am.put(actSave, actSave);

        // テキストエリアのスクロール、スクロールバーは縦横ともに常に表示しておく.
        JScrollPane scr = new JScrollPane(area);
        scr.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scr.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        Box btnPanel = Box.createHorizontalBox();

        JButton btnFont = new JButton(new AbstractAction(
                resource.getString("changeFont.button.title")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                onChangeFont();
            }
        });
        btnFont.setToolTipText(resource.getString("changeFont.button.tooltip"));
        btnPanel.add(btnFont);

        btnPanel.add(Box.createHorizontalGlue());

        JButton btnExport = new JButton(new AbstractAction(
                resource.getString("export.button.title")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                onExport();
            }
        });
        btnExport.setToolTipText(resource.getString("export.button.tooltip"));
        btnPanel.add(btnExport);

        JButton btnSave = new JButton(actSave);
        btnSave.setToolTipText(resource.getString("save.button.tooltip"));
        btnPanel.add(btnSave);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        contentPane.add(scr, BorderLayout.CENTER);
        contentPane.add(btnPanel, BorderLayout.SOUTH);

        this.area.getDocument().addUndoableEditListener(
                new UndoableEditListener() {
                    @Override
                    public void undoableEditHappened(UndoableEditEvent e) {
                        undoManager.addEdit(e.getEdit());
                    }
                });

        setModified(false);
    }

    /**
     * ウィンドウを閉じる.
     */
    protected void onClosing() {
        // 変更があれば破棄するか確認する.
        if (isModified()) {
            String message = resource.getString("confirm.close.unsavedchanges");
            String title = resource.getString("confirm.title");
            int ret = JOptionPane.showConfirmDialog(this, message, title,
                    JOptionPane.YES_NO_OPTION);
            if (ret != JOptionPane.YES_OPTION) {
                return;
            }
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
    private void updateTitle() {
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
     * 編集するテキストを設定する.<br>
     * 変更フラグはリセットされる.
     * 
     * @param text
     */
    public void setText(String text) {
        if (text == null) {
            text = "";
        }

        // テキストを設定しなおす.
        area.setText(text);

        // 現在のUndo/Redo情報をクリアする.
        undoManager.discardAllEdits();

        setModified(false);
    }

    /**
     * 編集するテキストを取得する.
     * 
     * @return
     */
    public String getText() {
        return area.getText();
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
     * ファイルを上書き保存する.
     * 
     * @throws IOException
     */
    protected void onSave() throws IOException {
        assert file != null;

        String doc = area.getText();

        // 外部ファイルのソルトの再計算が必要な場合には保存に時間がかかるため
        // ウェイトカーソルにする.
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            documentController.encryptText(file, doc);

        } finally {
            setCursor(Cursor.getDefaultCursor());
        }

        setModified(false);
    }

    /**
     * ファイルを別名保存する.
     * 
     * @throws IOException
     */
    protected void onSaveAs() throws IOException {
        File rootDir = ConfigurationDirUtilities.getApplicationBaseDir();
        JFileChooser fileChooser = FileChooserEx.createFileChooser(rootDir,
                true);

        if (file != null) {
            fileChooser.setSelectedFile(file);

        } else if (temporaryTitle != null) {
            fileChooser.setSelectedFile(new File(temporaryTitle));
        }

        int ret = fileChooser.showSaveDialog(this);
        if (ret != JFileChooser.APPROVE_OPTION) {
            // OK以外
            return;
        }

        File file = fileChooser.getSelectedFile();

        File oldValue = this.file;
        this.file = file;
        updateTitle();

        onSave();

        firePropertyChange(PROPERTY_FILE, oldValue, file);
    }

    /**
     * テキストを平文で外部ファイルにエクスポートする.
     */
    protected void onExport() {
        JFileChooser fileChooser = FileChooserEx.createFileChooser(
                appConfig.getLastUseDir(), true);
        if (file != null) {
            String name = file.getName();
            fileChooser.setSelectedFile(new File(name));
        }

        int ret = fileChooser.showSaveDialog(this);
        if (ret != JFileChooser.APPROVE_OPTION) {
            // OK以外
            return;
        }

        try {
            File file = fileChooser.getSelectedFile();
            appConfig.setLastUseDir(file.getParentFile());

            String doc = area.getText();
            documentController.savePlainText(file, doc);

        } catch (Exception ex) {
            ErrorMessageHelper.showErrorDialog(TextInternalFrame.this, ex);
        }
    }

    /**
     * フォント選択
     */
    protected void onChangeFont() {
        String fontFamilyNames[] = GraphicsEnvironment
                .getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        Font currentFont = area.getFont();

        String fontName = null;
        int fontSize = 11;

        if (currentFont != null) {
            fontName = currentFont.getFontName();
            fontSize = currentFont.getSize();
        }

        JComboBox fontCombo = new JComboBox(fontFamilyNames);
        if (fontName != null) {
            fontCombo.setSelectedItem(fontName);
        }

        JFormattedTextField txtFontSize = new JFormattedTextField(
                new DecimalFormat("##"));
        txtFontSize.setValue(fontSize);

        JPanel pnl = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.EAST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.;
        gbc.weighty = 0.;

        pnl.add(new JLabel("Font Name"), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;

        pnl.add(new JLabel("Font Size"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.;

        pnl.add(fontCombo, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.;

        pnl.add(txtFontSize, gbc);

        int ret = JOptionPane.showConfirmDialog(this, pnl, "Font",
                JOptionPane.OK_CANCEL_OPTION);
        if (ret != JOptionPane.OK_OPTION) {
            return;
        }

        String selFontName = (String) fontCombo.getSelectedItem();
        int selFontSize = ((Number) txtFontSize.getValue()).intValue();
        if (selFontName != null && selFontSize > 0) {
            // フォントを適用する.
            Font newFont = new Font(selFontName, Font.PLAIN, selFontSize);
            area.setFont(newFont);

            // 現在のフォント設定を記憶
            appConfig.setFontName(selFontName);
            appConfig.setFontSize(selFontSize);
        }
    }
}
