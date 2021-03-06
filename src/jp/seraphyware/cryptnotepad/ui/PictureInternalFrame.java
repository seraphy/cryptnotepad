package jp.seraphyware.cryptnotepad.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
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
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;

import jp.seraphyware.cryptnotepad.model.ApplicationData;
import jp.seraphyware.cryptnotepad.model.DocumentController;
import jp.seraphyware.cryptnotepad.util.ErrorMessageHelper;
import jp.seraphyware.cryptnotepad.util.XMLResourceBundle;

public class PictureInternalFrame extends DocumentInternalFrame {

    private static final long serialVersionUID = 5878052788182350970L;

    /**
     * ロガー.<br>
     */
    private static final Logger logger = Logger
            .getLogger(PictureInternalFrame.class.getName());

    /**
     * リソースバンドル
     */
    private ResourceBundle resource;

    /**
     * ピクチャパネル
     */
    private SamplePicturePanel picturePanel;

    /**
     * コンストラクタ
     * 
     * @param documentController
     */
    public PictureInternalFrame(DocumentController documentController) {
        super(documentController);

        this.resource = ResourceBundle.getBundle(getClass().getName(),
                XMLResourceBundle.CONTROL);

        updateTitle();

        picturePanel = new SamplePicturePanel();

        final AbstractAction actSave = new AbstractAction(
                resource.getString("save.button.title")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isExistFile() || isReadonly()
                        || (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
                    // 新規ドキュメントであるか、シフトキーとともに押された場合
                    // もしくは読み込み専用である場合
                    onSaveAs();

                } else {
                    // 上書き保存
                    onSave();
                }
            }
        };

        ActionMap am = this.picturePanel.getActionMap();
        InputMap im = this.picturePanel.getInputMap(JComponent.WHEN_FOCUSED);

        Toolkit tk = Toolkit.getDefaultToolkit();
        int shortcutMask = tk.getMenuShortcutKeyMask();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutMask), actSave);
        am.put(actSave, actSave);

        Box btnPanel = Box.createHorizontalBox();

        btnPanel.add(Box.createHorizontalGlue());

        // Exportボタン
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

        // ドラッグハンドル用の余白
        btnPanel.add(Box.createHorizontalStrut(24));

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        JScrollPane scr = new JScrollPane(picturePanel);
        scr.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scr.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        contentPane.add(scr, BorderLayout.CENTER);
        contentPane.add(btnPanel, BorderLayout.SOUTH);

        setModified(false);

        addPropertyChangeListener(PROPERTY_DATA, new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                loadPicture();
            }
        });
    }

    /**
     * バイナリデータから画像データを構築してピクチャとして表示する. ContentTypeが画像でなければ、かわりにMIMEタイプを表示する.
     */
    protected void loadPicture() {
        String message = "";
        BufferedImage img = null;
        ApplicationData data = getData();
        if (data != null) {
            String contentType = data.getContentType();
            if (contentType.startsWith("image/")) {
                // イメージの場合
                try {
                    byte[] buf = data.getData();
                    if (buf != null && buf.length > 0) {
                        ImageIO.setUseCache(false); // 一時ディレクトリに書き込まないように
                        ByteArrayInputStream is = new ByteArrayInputStream(buf);
                        try {
                            img = ImageIO.read(is);
                        } finally {
                            is.close();
                        }
                    }

                } catch (Exception ex) {
                    logger.log(Level.INFO, "picture load failed.", ex);
                    message = "load failed.";
                    img = null;
                }

            } else {
                // イメージではない場合
                message = contentType;
            }
        }

        picturePanel.setSamplePicture(img);
        picturePanel.setAlternateText(message);
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

        ApplicationData data = getData();
        if (data == null) {
            return;
        }

        // 外部ファイルのソルトの再計算が必要な場合には保存に時間がかかるため
        // ウェイトカーソルにする.
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            documentController.encrypt(file, data);

        } finally {
            setCursor(Cursor.getDefaultCursor());
        }

        setModified(false);
    }

    /**
     * ファイルを上書き保存する.
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
     * 
     * @throws IOException
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
            ApplicationData data = getData();
            if (file != null && data != null) {
                byte[] buf = data.getData();
                documentController.savePlainBinary(file, buf);
            }

        } catch (Exception ex) {
            ErrorMessageHelper.showErrorDialog(this, ex);
        }
    }
}
