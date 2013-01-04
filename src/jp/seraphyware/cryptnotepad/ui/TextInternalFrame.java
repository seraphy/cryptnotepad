package jp.seraphyware.cryptnotepad.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
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

    public static final String PROPERTY_MODIFIED = "modified";

    /**
     * リソースバンドル
     */
    private ResourceBundle resource;

    /**
     * 対象ファイル、新規の場合はnull
     */
    private File file;

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

    public TextInternalFrame(File file) {
        this.resource = ResourceBundle.getBundle(getClass().getName(),
                XMLResourceBundle.CONTROL);
        this.file = file;

        if (file != null) {
            String title = file.getName();
            setTitle(title);

        } else {
            setTitle(resource.getString("notitled.title"));
        }

        setMaximizable(true);
        setResizable(true);
        setIconifiable(true);
        setClosable(true);

        StringWriter sw = new StringWriter();
        if (file != null) {
            PrintWriter pw = new PrintWriter(sw);
            try {
                loadText(file, pw);

            } catch (Exception ex) {
                ex.printStackTrace(pw);
            }
            pw.flush();
        }

        String doc = sw.toString();

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

        im.put(KeyStroke.getKeyStroke('Z', Event.CTRL_MASK), actUndo);
        am.put(actUndo, actUndo);
        im.put(KeyStroke.getKeyStroke('Y', Event.CTRL_MASK), actRedo);
        am.put(actRedo, actRedo);

        JScrollPane scr = new JScrollPane(area);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(new JButton(new AbstractAction(resource
                .getString("export.button.title")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                onExport();
            }
        }));

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

        btnPanel.add(new JButton(actSave));

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        contentPane.add(scr, BorderLayout.CENTER);
        contentPane.add(btnPanel, BorderLayout.SOUTH);

        area.setText(doc);

        this.area.getDocument().addUndoableEditListener(
                new UndoableEditListener() {
                    @Override
                    public void undoableEditHappened(UndoableEditEvent e) {
                        undoManager.addEdit(e.getEdit());
                    }
                });

        setModified(false);
        actSave.setEnabled(false);

        addPropertyChangeListener(PROPERTY_MODIFIED,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        actSave.setEnabled(isModified());
                    }
                });
    }

    public File getFile() {
        return file;
    }

    private void loadText(File file, Writer wr) throws IOException {
        Reader rd = new InputStreamReader(new FileInputStream(file), "UTF-8");
        try {
            int ch;
            while ((ch = rd.read()) != -1) {
                wr.append((char) ch);
            }

        } finally {
            rd.close();
        }
    }

    protected void onSave() throws IOException {
        String doc = area.getText();

        Writer wr = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
        try {
            wr.write(doc);

        } finally {
            wr.close();
        }

        setModified(false);
    }

    public void setModified(boolean modified) {
        boolean oldValue = this.modified;
        this.modified = modified;
        firePropertyChange(PROPERTY_MODIFIED, oldValue, modified);
    }

    public boolean isModified() {
        return modified;
    }

    protected void onSaveAs() throws IOException {
        File rootDir = ConfigurationDirUtilities.getApplicationBaseDir();
        JFileChooser fileChooser = new JFileChooser(rootDir);
        int ret = fileChooser.showSaveDialog(this);
        if (ret != JFileChooser.APPROVE_OPTION) {
            // OK以外
            return;
        }

        File file = fileChooser.getSelectedFile();

        File oldValue = this.file;
        this.file = file;

        onSave();

        firePropertyChange(PROPERTY_FILE, oldValue, file);
    }

    protected void onExport() {
        JOptionPane.showMessageDialog(this, "export");
    }
}
