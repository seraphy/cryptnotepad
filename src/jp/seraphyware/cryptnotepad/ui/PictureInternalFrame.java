package jp.seraphyware.cryptnotepad.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
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
     * アプリケーションデータ
     */
    private ApplicationData data;

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
                if (!isExistFile()
                        || (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
                    // 新規ドキュメントであるか、シフトキーとともに押された場合
                    onSaveAs();

                } else {
                    // 上書き保存
                    onSave();
                }
            }
        };

        ActionMap am = this.picturePanel.getActionMap();
        InputMap im = this.picturePanel.getInputMap(JComponent.WHEN_FOCUSED);

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK),
                actSave);
        am.put(actSave, actSave);

        Box btnPanel = Box.createHorizontalBox();

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

        JScrollPane scr = new JScrollPane(picturePanel);
        scr.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scr.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        contentPane.add(scr, BorderLayout.CENTER);
        contentPane.add(btnPanel, BorderLayout.SOUTH);

        setModified(false);
    }

    public void setData(ApplicationData data) {
        ApplicationData oldValue = this.data;
        this.data = data;

        loadPicture();

        firePropertyChange("data", oldValue, data);

        setModified(false);
    }

    public ApplicationData getData() {
        return data;
    }

    /**
     * バイナリデータから画像データを構築してピクチャとして表示する. ContentTypeが画像でなければ、かわりにMIMEタイプを表示する.
     */
    protected void loadPicture() {
        String message = "";
        BufferedImage img = null;
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
