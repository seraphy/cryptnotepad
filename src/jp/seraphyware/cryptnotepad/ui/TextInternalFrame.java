package jp.seraphyware.cryptnotepad.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoManager;

import jp.seraphyware.cryptnotepad.model.DocumentController;
import jp.seraphyware.cryptnotepad.util.ErrorMessageHelper;
import jp.seraphyware.cryptnotepad.util.XMLResourceBundle;

/**
 * テキストのドキュメントフレーム
 * 
 * @author seraphy
 */
public class TextInternalFrame extends DocumentInternalFrame {

    private static final long serialVersionUID = -6664897509335391245L;

    /**
     * リソースバンドル
     */
    private ResourceBundle resource;

    /**
     * テキストエリア
     */
    private JTextArea area;

    /**
     * Undoマネージャ
     */
    private UndoManager undoManager;

    /**
     * コンストラクタ
     * 
     * @param documentController
     */
    public TextInternalFrame(DocumentController documentController) {
        super(documentController);

        this.resource = ResourceBundle.getBundle(getClass().getName(),
                XMLResourceBundle.CONTROL);

        updateTitle();

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
                if (!isExistFile() || isReadonly()
                        || (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
                    // 新規ドキュメントであるか、シフトキーとともに押された場合
                    // もしくはREADONLYの場合は別名保存のみ可
                    onSaveAs();

                } else {
                    // 上書き保存
                    onSave();
                }
            }
        };

        Toolkit tk = Toolkit.getDefaultToolkit();
        int shortcutMask = tk.getMenuShortcutKeyMask();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcutMask), actUndo);
        am.put(actUndo, actUndo);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, shortcutMask), actRedo);
        am.put(actRedo, actRedo);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutMask), actSave);
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

        // READONLYプロパティ変更に対応する
        addPropertyChangeListener(PROPERTY_READONLY,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        area.setEditable(!isReadonly());
                    }
                });

        setModified(false);
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
     * ファイルを上書き保存する.
     * 
     * @throws IOException
     */
    @Override
    protected void save() throws IOException {
        File file = getFile();
        if (file == null) {
            throw new IllegalStateException("file is null");
        }
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
     * 上書き保存する.<br>
     */
    protected void onSave() {
        try {
            if (!isReadonly()) {
                save();
            }

        } catch (Exception ex) {
            ErrorMessageHelper.showErrorDialog(this, ex);
        }
    }

    /**
     * ファイルを別名保存する.
     */
    protected void onSaveAs() {
        try {
            saveAs();

        } catch (Exception ex) {
            ErrorMessageHelper.showErrorDialog(this, ex);
        }
    }

    /**
     * テキストを平文で外部ファイルにエクスポートする.
     */
    protected void onExport() {
        try {
            File file = showExportDialog();
            if (file != null) {
                String doc = area.getText();
                documentController.savePlainText(file, doc);
            }

        } catch (Exception ex) {
            ErrorMessageHelper.showErrorDialog(this, ex);
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
