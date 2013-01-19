package jp.seraphyware.cryptnotepad.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.undo.UndoManager;

import jp.seraphyware.cryptnotepad.model.ApplicationData;
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
     * テキストエリアのドキュメントモデル
     */
    private PlainDocument plainDocument;

    /**
     * テキストエリア
     */
    private JTextArea area;

    private JTextArea area2;

    /**
     * テキストエリアのスクロールペイン
     */
    private JScrollPane scr;

    private JScrollPane scr2;

    /**
     * スプリットペイン
     */
    private JSplitPane splt;

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

        this.plainDocument = new PlainDocument();
        this.area = new JTextArea(this.plainDocument);
        this.area2 = new JTextArea(this.plainDocument);
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
            area2.setFont(newFont);
        }

        // Undo/Redoに対応する.
        this.undoManager = new UndoManager();
        this.plainDocument.addUndoableEditListener(new UndoableEditListener() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                undoManager.addEdit(e.getEdit());
            }
        });

        // Undoアクション
        AbstractAction actUndo = new AbstractAction("undo") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canUndo()) {
                    undoManager.undo();
                }
            }
        };

        // Redoアクション
        AbstractAction actRedo = new AbstractAction("redo") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canRedo()) {
                    undoManager.redo();
                }
            }
        };

        // Saveアクション
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

        // キーマップ

        Toolkit tk = Toolkit.getDefaultToolkit();
        int shortcutMask = tk.getMenuShortcutKeyMask();

        JComponent[] comps = { area, area2 };
        for (JComponent comp : comps) {
            ActionMap am = comp.getActionMap();
            InputMap im = comp.getInputMap(JComponent.WHEN_FOCUSED);

            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcutMask), actUndo);
            am.put(actUndo, actUndo);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, shortcutMask), actRedo);
            am.put(actRedo, actRedo);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutMask), actSave);
            am.put(actSave, actSave);
        }

        // テキストエリアのスクロール、スクロールバーは縦横ともに常に表示しておく.
        scr = new JScrollPane(area);
        scr.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scr.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        scr2 = new JScrollPane(area2);
        scr2.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scr2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        Box btnPanel = Box.createHorizontalBox();

        // フォント変更ボタン
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

        // 2分割チェックボックス
        final JCheckBox chkSplit = new JCheckBox(
                resource.getString("divider.button.title"));
        btnPanel.add(chkSplit);

        // パディング
        btnPanel.add(Box.createHorizontalGlue());

        // 平文で保存ボタン
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

        // 保存ボタン
        JButton btnSave = new JButton(actSave);
        btnSave.setToolTipText(resource.getString("save.button.tooltip"));
        btnPanel.add(btnSave);

        // スプリットペイン
        this.splt = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        splt.setOneTouchExpandable(true);
        splt.setResizeWeight(0.5); // 半分で分割
        splt.setLeftComponent(scr); // 初期状態は左ペインのみ設定

        // 2分割チェックボックスのハンドラ
        chkSplit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (chkSplit.isSelected()) {
                    // 右ペインを設定
                    splt.setRightComponent(scr2);

                } else {
                    // 右ペインを解除
                    splt.remove(scr2);
                }
            }
        });

        // レイアウト

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(splt, BorderLayout.CENTER);
        contentPane.add(btnPanel, BorderLayout.SOUTH);

        // READONLYプロパティ変更に対応する
        addPropertyChangeListener(PROPERTY_READONLY,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        area.setEditable(!isReadonly());
                        area2.setEditable(!isReadonly());
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
    private void setText(String text) {
        if (text == null) {
            text = "";
        }

        // テキストを設定しなおす.
        try {
            int len = plainDocument.getLength();
            plainDocument.remove(0, len);
            plainDocument.insertString(0, text, null);

            // 現在のUndo/Redo情報をクリアする.
            undoManager.discardAllEdits();

            setModified(false);

        } catch (BadLocationException ex) {
            UIManager.getLookAndFeel().provideErrorFeedback(this);
        }
    }

    @Override
    public void setData(ApplicationData data) {
        super.setData(data); // スーパークラスを直接呼び出す
        String text = null;
        if (data != null) {
            text = data.getText();
        }
        setText(text);
    }

    /**
     * 編集するテキストを取得する.
     * 
     * @return
     */
    private String getText() {
        try {
            int len = plainDocument.getLength();
            return plainDocument.getText(0, len);

        } catch (BadLocationException ex) {
            return "";
        }
    }

    @Override
    public ApplicationData getData() {
        // テキストの取得
        String text = getText();

        // ドキュメント名を設定する.
        // (既存データがあれば、それをもちいる.)
        String docTitle;
        ApplicationData data = super.getData();
        if (data != null) {
            docTitle = data.getDocumentTitle();
        } else {
            // データはないがファイルが指定されていれば、ファイル名が採用される.
            docTitle = getSuggestDocumentTitle();
        }

        ApplicationData dataNew = createAppData(text, docTitle);
        super.setData(dataNew); // スーパークラスを直接呼び出す.
        return super.getData();
    }

    /**
     * ファイルを暗号化してテキストを保存する.
     * 
     * @param file
     *            保存先ファイル
     * @param text
     *            暗号化するテキスト
     * @param orgFileName
     *            オリジナルファイル名、nullの場合は保存先ファイル名を用いる.
     * @throws IOException
     *             失敗
     */
    private ApplicationData createAppData(String text, String docTitle) {
        String encoding = documentController.getSettingsModel().getEncoding();
        String contentType = "text/plain; charset=" + encoding;
        return new ApplicationData(contentType, text, docTitle);
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
            area2.setFont(newFont);

            // 現在のフォント設定を記憶
            appConfig.setFontName(selFontName);
            appConfig.setFontSize(selFontSize);
        }
    }
}
