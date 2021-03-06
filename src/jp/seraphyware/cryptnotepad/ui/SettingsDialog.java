package jp.seraphyware.cryptnotepad.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import jp.seraphyware.cryptnotepad.model.ApplicationSettings;
import jp.seraphyware.cryptnotepad.model.SettingsModel;
import jp.seraphyware.cryptnotepad.util.ConfigurationDirUtilities;
import jp.seraphyware.cryptnotepad.util.ErrorMessageHelper;
import jp.seraphyware.cryptnotepad.util.XMLResourceBundle;

/**
 * 設定ダイアログ.
 * 
 * @author seraphy
 */
public class SettingsDialog extends JDialog {

    private static final long serialVersionUID = -8017941806888773490L;

    /**
     * ロガー.<br>
     */
    private static final Logger logger = Logger.getLogger(SettingsDialog.class
            .getName());

    /**
     * リソースバンドル
     */
    private ResourceBundle resource;

    /**
     * モデル
     */
    private SettingsModel model = null;

    /**
     * コンストラクタ
     * 
     * @param parent
     *            親フレーム
     */
    public SettingsDialog(JFrame parent) {
        super(parent);
        setModalityType(ModalityType.APPLICATION_MODAL);
        try {
            resource = ResourceBundle.getBundle(getClass().getName(),
                    XMLResourceBundle.CONTROL);
            init();

            // パスフレーズにフォーカスを設定する.
            txtPassphrase.requestFocus();

        } catch (RuntimeException ex) {
            dispose();
            throw ex;
        }
    }

    /**
     * 文字コード
     */
    private JTextField txtEncoding;

    /**
     * パスフレーズ
     */
    private JPasswordField txtPassphrase;

    /**
     * キーファイル
     */
    private JTextField txtKeyFile;

    /**
     * 文書ディレクトリ
     */
    private JTextField txtContentsDir;

    /**
     * 作業ディレクトリ
     */
    private JTextField txtWorkingDir;

    /**
     * レイアウトなど初期化
     */
    private void init() {
        // ウィンドウの閉じるボタン処理
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // メンバの構築

        txtEncoding = new JTextField();
        txtPassphrase = new JPasswordField();
        txtKeyFile = new JTextField();

        txtContentsDir = new JTextField();
        txtWorkingDir = new JTextField();

        Dimension passphraseSize = txtPassphrase.getPreferredSize();
        passphraseSize.width = 300;
        txtPassphrase.setPreferredSize(passphraseSize);

        setTitle(resource.getString("settingDialog.title"));

        // コンポーネントの登録

        GridBagConstraints gbc = new GridBagConstraints();

        // ファイル設定群

        JPanel pnlFileSettings = new JPanel();
        GridBagLayout gbl = new GridBagLayout();
        pnlFileSettings.setLayout(gbl);
        pnlFileSettings.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(3, 3, 3, 3), BorderFactory
                        .createTitledBorder(resource
                                .getString("titledborder.filesettings.text"))));

        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.ipadx = 2;
        gbc.ipady = 2;
        gbc.fill = GridBagConstraints.BOTH;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        pnlFileSettings.add(
                new JLabel(resource.getString("label.encoding.text")), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.;
        gbc.anchor = GridBagConstraints.WEST;
        pnlFileSettings.add(txtEncoding, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        pnlFileSettings.add(
                new JLabel(resource.getString("label.passphrase.text")), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.;
        gbc.anchor = GridBagConstraints.WEST;
        pnlFileSettings.add(txtPassphrase, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        pnlFileSettings.add(
                new JLabel(resource.getString("label.keyfile.text")), gbc);

        AbstractAction actBrowseFile = new AbstractAction(
                resource.getString("button.browse.text")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                onBrowseFile();
            }
        };

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.;
        gbc.anchor = GridBagConstraints.WEST;
        pnlFileSettings.add(
                createPanel(txtKeyFile, new JButton(actBrowseFile)), gbc);

        // ディレクトリ設定群

        JPanel pnlDirSettings = new JPanel();
        pnlDirSettings.setLayout(new GridBagLayout());
        pnlDirSettings.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(3, 3, 3, 3), BorderFactory
                        .createTitledBorder(resource
                                .getString("titledborder.dirsettings.text"))));

        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.ipadx = 2;
        gbc.ipady = 2;
        gbc.fill = GridBagConstraints.BOTH;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        pnlDirSettings.add(
                new JLabel(resource.getString("label.contentsDir.text")), gbc);

        AbstractAction actBrowseContentsDir = new AbstractAction(
                resource.getString("button.browse.text")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                onBrowseContentsDir();
            }
        };
        AbstractAction actBrowseWorkingDir = new AbstractAction(
                resource.getString("button.browse.text")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                onBrowseWorkingDir();
            }
        };

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.;
        gbc.anchor = GridBagConstraints.WEST;
        pnlDirSettings.add(
                createPanel(txtContentsDir, new JButton(actBrowseContentsDir)),
                gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        pnlDirSettings.add(
                new JLabel(resource.getString("label.workingDir.text")), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.;
        gbc.anchor = GridBagConstraints.WEST;
        pnlDirSettings.add(
                createPanel(txtWorkingDir, new JButton(actBrowseWorkingDir)),
                gbc);

        // ボタンパネル

        AbstractAction actOK = new AbstractAction(
                resource.getString("button.ok.text")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        };
        AbstractAction actCancel = new AbstractAction(
                resource.getString("button.cancel.text")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        };

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnOK = new JButton(actOK);
        JButton btnCancel = new JButton(actCancel);

        btnPanel.add(btnOK);
        btnPanel.add(btnCancel);

        // OKボタンをデフォルトボタンに設定
        getRootPane().setDefaultButton(btnOK);

        // ESCキーでキャンセルするようにキーマップを設定する.
        ActionMap am = getRootPane().getActionMap();
        InputMap im = getRootPane().getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), actCancel);
        am.put(actCancel, actCancel);

        // パネルの配置

        Box centerPanel = Box.createVerticalBox();
        centerPanel.add(pnlFileSettings);
        centerPanel.add(pnlDirSettings);
        centerPanel.add(Box.createVerticalGlue());

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(centerPanel, BorderLayout.CENTER);
        contentPane.add(btnPanel, BorderLayout.SOUTH);

        // データの設定
        update(false);

        // レイアウト完了
        pack();
    }

    /**
     * パネルを作成する.
     * 
     * @param center
     *            センターに配置するコンポーネント
     * @param east
     *            東に配置するコンポーネント
     * @return パネル
     */
    private JPanel createPanel(JComponent center, JComponent east) {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.add(center, BorderLayout.CENTER);
        pnl.add(east, BorderLayout.EAST);
        return pnl;
    }

    /**
     * 画面を更新する. モデルが設定されていない場合は何もしない.
     * 
     * @param save
     *            trueであればモデルにUIの値を設定する.<br>
     *            falseであればUIにモデルの値を設定する.<br>
     */
    protected void update(boolean save) {
        if (model == null) {
            return;
        }

        ApplicationSettings appConfig = ApplicationSettings.getInstance();

        if (save) {
            model.setEncoding(txtEncoding.getText());
            model.setKeyFile(txtKeyFile.getText());

            // パスフレーズが入力されていない場合は以前のまま使用する.
            char[] passphrase = txtPassphrase.getPassword();
            if (passphrase.length > 0) {
                model.setPassphrase(passphrase);
            }

            String strContentsDir = txtContentsDir.getText();
            if (strContentsDir != null && strContentsDir.trim().length() > 0) {
                appConfig.setContentsDir(new File(strContentsDir.trim()));
            } else {
                appConfig.setContentsDir(ConfigurationDirUtilities
                        .getApplicationBaseDir());
            }
            String strWorkingDir = txtWorkingDir.getText();
            if (strWorkingDir != null && strWorkingDir.trim().length() > 0) {
                appConfig.setWorkingDir(new File(strWorkingDir.trim()));
            } else {
                appConfig.setWorkingDir(new File(System
                        .getProperty("java.io.tmpdir")));
            }

        } else {
            txtEncoding.setText(model.getEncoding());
            txtKeyFile.setText(model.getKeyFile());

            txtContentsDir
                    .setText(appConfig.getContentsDir().getAbsolutePath());
            txtWorkingDir.setText(appConfig.getWorkingDir().getAbsolutePath());
        }
    }

    /**
     * モデルを設定する.(参照渡し)
     * 
     * @param model
     */
    public void setModel(SettingsModel model) {
        this.model = model;
        update(false);
    }

    /**
     * モデルデータを取得する.(参照渡し)
     * 
     * @return モデルデータ
     */
    public SettingsModel getModel() {
        return model;
    }

    /**
     * ファイル選択ハンドラ
     */
    protected void onBrowseFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(true);
        int ret = fileChooser.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            txtKeyFile.setText(selectedFile.getAbsolutePath());
        }
    }

    /**
     * 文書ディレクトリを選択する.
     */
    protected void onBrowseContentsDir() {
        JFileChooser dirChooser = new JFileChooser();
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        String strContentsDir = txtContentsDir.getText();
        if (strContentsDir != null && strContentsDir.trim().length() > 0) {
            dirChooser.setSelectedFile(new File(strContentsDir.trim()));
        }
        int ret = dirChooser.showOpenDialog(this);
        if (ret != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File contentsDir = dirChooser.getSelectedFile();
        if (contentsDir != null && contentsDir.isDirectory()) {
            txtContentsDir.setText(contentsDir.getAbsolutePath());
        }
    }

    /**
     * 作業ディレクトリを選択する.
     */
    protected void onBrowseWorkingDir() {
        JFileChooser dirChooser = new JFileChooser();
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        String strWorkingDir = txtWorkingDir.getText();
        if (strWorkingDir != null && strWorkingDir.trim().length() > 0) {
            dirChooser.setSelectedFile(new File(strWorkingDir.trim()));
        }
        int ret = dirChooser.showOpenDialog(this);
        if (ret != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File workingDir = dirChooser.getSelectedFile();
        if (workingDir != null && workingDir.isDirectory()) {
            txtWorkingDir.setText(workingDir.getAbsolutePath());
        }
    }

    /**
     * OKハンドラ
     */
    protected void onOK() {
        if (model == null) {
            logger.log(Level.WARNING, "model missing");
            return;
        }
        try {
            update(true);
            logger.log(Level.FINE, "ok model=" + model);
            dispose();

        } catch (Exception ex) {
            ErrorMessageHelper.showErrorDialog(SettingsDialog.this, ex);
        }
    }

    /**
     * キャンセルハンドラ
     */
    protected void onCancel() {
        logger.log(Level.FINE, "cancel model=" + model);
        dispose();
    }
}
