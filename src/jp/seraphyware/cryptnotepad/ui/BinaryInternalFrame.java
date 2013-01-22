package jp.seraphyware.cryptnotepad.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import jp.seraphyware.cryptnotepad.model.ApplicationData;
import jp.seraphyware.cryptnotepad.model.DocumentController;
import jp.seraphyware.cryptnotepad.util.ErrorMessageHelper;
import jp.seraphyware.cryptnotepad.util.XMLResourceBundle;

public class BinaryInternalFrame extends DocumentInternalFrame {

    private static final long serialVersionUID = 3954073407495314454L;

    public static final String PROPERTY_CURRENTPROCESS = "currentProcess";

    public static final String PROPERTY_WORKINGFILE = "workingFile";

    /**
     * ワーキングファイルの更新チェック間隔.(mSec)
     */
    public static final int TIMER_DELAY = 1500; // 1.5Sec

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
     * ワーク用ファイル
     */
    private File workingFile;

    /**
     * ワーク用ファイルの作成時点の更新日時.<br>
     * 更新判定用.<br>
     */
    private long workingFileLastModified;

    /**
     * ワーク用ファイルの作成時点のサイズ.<br>
     * 更新判定用.<br>
     */
    private long workingFileSize;

    /**
     * プロセスモードごとの処理内容の分岐用.<br>
     */
    protected interface Process {

        /**
         * 暗号化ファイルの内容をワークファイルにロードする.
         * 
         * @throws IOException
         */
        void load() throws IOException;

        /**
         * ワークファイルの内容を暗号化ファイルに保存する.
         * 
         * @throws IOException
         */
        void save() throws IOException;

        /**
         * ワークファイルを安全に削除する.
         * 
         * @throws IOException
         */
        void close() throws IOException;

        /**
         * ボタン等のUI要素を更新する.
         */
        void updateUI();
    }

    /**
     * データがない
     */
    protected final Process processNoData = new Process() {
        @Override
        public void close() throws IOException {
            throw new IllegalStateException("nodata");
        }

        @Override
        public void load() throws IOException {
            throw new IllegalStateException("nodata");
        }

        @Override
        public void save() {
            throw new IllegalStateException("nodata");
        }

        @Override
        public void updateUI() {
            txtStatus.setForeground(Color.gray);
            txtStatus.setText(resource.getString("status.nodata.title"));

            actClose.setEnabled(false);
            actSave.setEnabled(false);
            actLoad.setEnabled(false);

            setModified(false);
        }

        @Override
        public String toString() {
            return "processNoData";
        }
    };

    /**
     * データはあるがファイルに関連づけられていない
     */
    protected final Process processNoMounted = new Process() {
        @Override
        public void load() throws IOException {
            throw new IllegalStateException("nomounted");
        }

        @Override
        public void close() throws IOException {
            throw new IllegalStateException("nomounted");
        }

        @Override
        public void save() throws IOException {
            BinaryInternalFrame.this.saveAs();
        }

        @Override
        public void updateUI() {
            txtStatus.setForeground(Color.red);
            txtStatus.setText(resource.getString("status.nomounted.title"));

            actClose.setEnabled(false);
            actSave.setEnabled(true);
            actLoad.setEnabled(false);

            setModified(false);
        }

        @Override
        public String toString() {
            return "processNoMounted";
        }
    };

    /**
     * データがありファイルもあるが、ワーキングファイルは開かれていない
     */
    protected final Process processNoOpened = new Process() {
        @Override
        public void load() throws IOException {
            loadToWorking();
        }

        @Override
        public void close() throws IOException {
            throw new IllegalStateException("noopened");
        }

        @Override
        public void save() {
            throw new IllegalStateException("noopened");
        }

        @Override
        public void updateUI() {
            txtStatus.setForeground(Color.black);
            txtStatus.setText(resource.getString("status.noopend.title"));

            actClose.setEnabled(false);
            actSave.setEnabled(false);
            actLoad.setEnabled(true);

            setModified(false);
        }

        @Override
        public String toString() {
            return "processNoOpened";
        }
    };

    /**
     * ワーキングファイルはあるが、変更されていない.<br>
     * データファイルはあるかもしれないし、ないかもしれない.<br>
     */
    protected final Process processNoModified = new Process() {
        @Override
        public void close() throws IOException {
            eraseWorkingFile();
        }

        @Override
        public void load() {
            throw new IllegalStateException("nomodified");
        }

        @Override
        public void save() throws IOException {
            loadFromWorking();
            if (!isReadonly()) {
                BinaryInternalFrame.this.save();

            } else {
                BinaryInternalFrame.this.saveAs();
            }
        }

        @Override
        public void updateUI() {
            txtStatus.setForeground(Color.blue);
            txtStatus.setText(resource.getString("status.nomodified.title"));

            actClose.setEnabled(true);
            actSave.setEnabled(true); // 変更が確認できなくてもSave可
            actLoad.setEnabled(false);

            // ワーキングファイルがある時点で更新あり、とみなす.
            setModified(true);
        }

        @Override
        public String toString() {
            return "processNoModified";
        }
    };

    /**
     * ワーキングファイルは変更されている.<br>
     * データファイルはあるかもしれないし、ないかもしれない.<br>
     */
    protected final Process processModified = new Process() {
        @Override
        public void close() throws IOException {
            if (confirmDestroy()) {
                eraseWorkingFile();
            }
        }

        @Override
        public void load() throws IOException {
            if (confirmDestroy()) {
                eraseWorkingFile();
                loadToWorking();
            }
        }

        @Override
        public void save() throws IOException {
            loadFromWorking();
            if (!isReadonly()) {
                BinaryInternalFrame.this.save();

            } else {
                BinaryInternalFrame.this.saveAs();
            }
        }

        @Override
        public void updateUI() {
            txtStatus.setForeground(Color.red);
            txtStatus.setText(resource.getString("status.modified.title"));

            actClose.setEnabled(true);
            actSave.setEnabled(true);
            actLoad.setEnabled(false);

            // ワーキングファイルがある時点で更新あり、とみなす.
            setModified(true);
        }

        @Override
        public String toString() {
            return "processModified";
        }
    };

    /**
     * 現在のプロセスモード
     */
    protected Process currentProcess;

    /**
     * 定期的にワーキングファイルの更新を確認するためのタイマー
     */
    private Timer timer;

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
                try {
                    currentProcess.load();

                } catch (Exception ex) {
                    ErrorMessageHelper.showErrorDialog(
                            BinaryInternalFrame.this, ex);
                }
            }
        };
        actOpen = new AbstractAction(resource.getString("open.button.title")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (workingFile != null && workingFile.exists()) {
                        Desktop desktop = Desktop.getDesktop();
                        desktop.open(workingFile);
                    }

                } catch (Exception ex) {
                    ErrorMessageHelper.showErrorDialog(
                            BinaryInternalFrame.this, ex);
                }
            }
        };
        actSave = new AbstractAction(resource.getString("save.button.title")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    currentProcess.save();

                } catch (Exception ex) {
                    ErrorMessageHelper.showErrorDialog(
                            BinaryInternalFrame.this, ex);
                }
            }
        };
        actClose = new AbstractAction(resource.getString("close.button.title")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    currentProcess.close();

                } catch (Exception ex) {
                    ErrorMessageHelper.showErrorDialog(
                            BinaryInternalFrame.this, ex);
                }
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

        // プロパティ変更によってUI要素を更新する.
        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                BinaryInternalFrame.this.stateUIChange(evt);
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

        // 初期ステータス
        setCurrentProcess(processNoData);
        actOpen.setEnabled(false);
        setModified(false);

        // タイマーの設定
        timer = new Timer(TIMER_DELAY, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logger.log(Level.FINEST, "Timer Event. " + e);
                updateProcessState();
            }
        });
        timer.start();
        logger.log(Level.FINEST, "Start Timer. " + timer);
    }

    /**
     * ワーキングファイルに変更がある場合に破棄してもよいか確認する.<br>
     * ワーキングファイルを作成していないか、変更していなければ常にtrue.<br>
     * 
     * @return 破棄しても良い場合はtrue
     */
    protected boolean confirmDestroy() {
        if (isWorkingFileModified()) {
            String message = resource.getString("confirm.close.unsavedchanges");
            String title = resource.getString("confirm.title");
            int ret = JOptionPane.showConfirmDialog(this, message, title,
                    JOptionPane.YES_NO_OPTION);
            if (ret != JOptionPane.YES_OPTION) {
                // 破棄しない場合はfalseを返す
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onClosing() {
        if (!checkModify()) {
            // 更新ファイルがあるので破棄しない場合はクローズを中断する.
            return;
        }
        setModified(false);

        for (;;) {
            try {
                // ワークファイルの削除(あれば)
                eraseWorkingFile();

            } catch (IOException ex) {
                // ワークファイルの削除に失敗した場合はリトライを試行できる.
                int ret = JOptionPane.showConfirmDialog(this, ex.toString(),
                        "RETRY?", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.ERROR_MESSAGE);
                if (ret != JOptionPane.CANCEL_OPTION) {
                    // リトライ
                    continue;
                }
            }
            // ワークファイルの削除の成否を問わずクローズを続行する.
            break;
        }

        // タイマーの停止
        timer.stop();
        logger.log(Level.FINEST, "Stop Timer. " + timer);

        // 閉じる処理続行
        super.onClosing();
    }

    /**
     * 現在のプロセスを設定する.
     * 
     * @param currentProcess
     */
    public void setCurrentProcess(Process currentProcess) {
        Process oldValue = this.currentProcess;
        this.currentProcess = currentProcess;
        firePropertyChange(PROPERTY_CURRENTPROCESS, oldValue, currentProcess);
    }

    /**
     * 現在のプロセスを取得する.
     * 
     * @return
     */
    public Process getCurrentProcess() {
        return currentProcess;
    }

    /**
     * プロパティ変更通知をうけて画面のUI要素の更新を行う.<br>
     * 
     * @param evt
     *            プロパティ変更通知
     */
    protected void stateUIChange(PropertyChangeEvent evt) {
        String name = evt.getPropertyName();

        if (PROPERTY_MODIFIED.equals(name)) {
            // 変更フラグ、プロセスモードの変更通知は、ここでは処理しない.
            return;
        }

        if (PROPERTY_CURRENTPROCESS.equals(name)) {
            // カレントプロセスモードが変更された場合は画面の状態を更新する.
            logger.log(Level.INFO, "currentProcess=" + getCurrentProcess());
            currentProcess.updateUI();
            return;
        }

        if (PROPERTY_FILE.equals(name)) {
            // ファイルが設定された場合
            File file = getFile();
            txtSourceFile.setText((file == null) ? "" : file.getAbsolutePath());
        }

        if (PROPERTY_WORKINGFILE.equals(name)) {
            // ワーキングファイルが設定された場合
            File workingFile = getWorkingFile();
            txtWorkingFile.setText((workingFile == null) ? "" : workingFile
                    .getAbsolutePath());
            actOpen.setEnabled(workingFile != null && workingFile.exists());
        }

        // ファイルやワークファイル等の状態変更があった場合は、
        // プロセスモードの更新をチェックする.
        updateProcessState();
    }

    /**
     * データ、ファイル、ワークファイルの状態をもとに、 現在のプロセスモードを更新する.<br>
     */
    protected void updateProcessState() {
        File file = getFile();
        ApplicationData data = getData();

        Process process;
        if (data == null) {
            // データがない
            process = processNoData;

        } else if (file == null || !file.exists()) {
            // データはあるがファイルがない (新規)
            process = processNoMounted;

        } else if (workingFile == null) {
            // データがありファイルもあるがワークファイルは作成していない(既存)
            process = processNoOpened;

        } else if (!isWorkingFileModified()) {
            // ワークファイルがあるが変更されていない.
            process = processNoModified;

        } else {
            // ワークファイルがあり変更されている.
            process = processModified;
        }

        setCurrentProcess(process);
    }

    /**
     * ワーキングファイルを設定します.
     * 
     * @param workingFile
     */
    public void setWorkingFile(File workingFile) {
        File oldValue = this.workingFile;
        this.workingFile = workingFile;
        firePropertyChange(PROPERTY_WORKINGFILE, oldValue, workingFile);
    }

    /**
     * ワーキングファイルを取得します.
     */
    public File getWorkingFile() {
        return workingFile;
    }

    /**
     * ワーキングファイルが変更されているか? ワーキングファイルが設定されていない場合は変更なしとみなす.
     * 
     * @return ワーキングファイルが変更されている場合はtrue.
     */
    protected boolean isWorkingFileModified() {
        if (workingFile == null || !workingFile.exists()) {
            // ワーキングファイルが未設定か、削除された場合は変更なしとみなす.
            return false;
        }
        long lastModified = workingFile.lastModified();
        long size = workingFile.length();

        return (workingFileLastModified != lastModified)
                || (workingFileSize != size);
    }

    /**
     * ワークファイルを削除する.
     * 
     * @throws IOException
     */
    protected void eraseWorkingFile() throws IOException {
        if (workingFile == null) {
            return;
        }

        // 安全な消去
        documentController.getSymCipher().delete(workingFile);

        // プロパティの更新
        setWorkingFile(null);
    }

    /**
     * ワークファイルからデータを取得する.
     * 
     * @throws IOException
     */
    protected void loadFromWorking() throws IOException {
        if (workingFile == null) {
            throw new IllegalStateException("working file is not specified.");
        }
        ApplicationData data = getData();
        if (data == null) {
            throw new IllegalStateException("no-data");
        }

        // ワークファイルから読み込み
        byte[] contents = documentController.loadBinary(workingFile);
        if (contents == null) {
            // ワークファイルが削除されていた場合はワーキングファイルを閉じたことにする.
            eraseWorkingFile();
            return;
        }

        // ワークファイルの内容を暗号化ファイルに格納
        String contentType = data.getContentType();
        String documentTitld = data.getDocumentTitle();

        // テキストであればバイナリからテキストに変換する
        String text = null;
        if (contentType.startsWith("text/")) {
            text = new String(contents, getDocuemtnEncodingOrDefault(data));
        }

        // プロパティの更新
        this.workingFileLastModified = workingFile.lastModified();
        this.workingFileSize = workingFile.length();
        if (text != null) {
            setData(new ApplicationData(contentType, text, documentTitld));
        } else {
            setData(new ApplicationData(contentType, contents, documentTitld));
        }
    }

    @Override
    protected void save() throws IOException {
        super.save();
        // ワーキングファイルが存在すれば保存後でも変更ありにマークしておく.
        setModified(workingFile != null && workingFile.exists());
    }

    @Override
    protected void saveAs() throws IOException {
        super.saveAs();
        // ワーキングファイルが存在すれば保存後でも変更ありにマークしておく.
        setModified(workingFile != null && workingFile.exists());
    }

    /**
     * データをワークファイルにロードする.
     * 
     * @throws IOException
     */
    protected void loadToWorking() throws IOException {
        if (workingFile != null) {
            throw new IllegalStateException("working file is already exists.");
        }
        ApplicationData data = getData();
        if (data == null) {
            throw new IllegalStateException("nodata");
        }

        // 出力する一時ファイル名を選定する.
        String workingFileName = data.getDocumentTitle();
        if (workingFileName != null && workingFileName.length() > 0) {
            // ファイル名のうち最終パスだけを採用する.
            workingFileName = new File(workingFileName).getName();
        }

        // 作業用ディレクトリの実在を確認する.
        File workDir = appConfig.getWorkingDir();
        if (workDir == null || !workDir.isDirectory()) {
            throw new IOException("working directoy not found. " + workDir);
        }

        // 一時ファイル名を決定する.
        File workingFile;
        if (workingFileName == null || workingFileName.trim().length() == 0) {
            // 一時ファイル名を自動で設定
            workingFile = File.createTempFile("crynote", ".tmp", workDir);

        } else {
            // 既存ファイル名もしくは一時タイトルからファイル名を決定
            workingFile = new File(workDir, workingFileName);
        }

        // ワーキングファイルに出力
        byte[] content = data.getData();
        if (content == null) {
            // テキストの場合はバイナリに変換する.
            String text = data.getText();
            if (text != null) {
                content = text.getBytes(getDocuemtnEncodingOrDefault(data));
            }
        }
        documentController.savePlainBinary(workingFile, content);

        // ワーキングファイルへの書き出し時点のファイル更新日時とサイズを保存
        this.workingFileLastModified = workingFile.lastModified();
        this.workingFileSize = workingFile.length();

        // プロパティの更新
        setWorkingFile(workingFile);
        setModified(true);
    }

    /**
     * データのMIMEタイプから文字コードを取得する.<br>
     * バイナリであるか、データが指定されていない場合、もしくは文字コードの指定がない場合は かわりに設定ダイアログで指定された文字コードを返す.<br>
     * 
     * @param data
     *            データ、null可、テキスト・バイナリを問わず
     * @return 文字コード、もしくはデフォルトの文字コード
     */
    protected String getDocuemtnEncodingOrDefault(ApplicationData data) {
        String encoding = null;
        if (data != null) {
            // ドキュメントの指定があれば、そのタイプから文字コードを取得する.(文字コード指定があれば)
            encoding = documentController
                    .getTextEncoding(data.getContentType());
        }
        if (encoding == null) {
            // 文字コードの指定がないか、ドキュメントがnullだったか、MIMEタイプがtext/*でなかった場合は
            // 設定ダイアログで指定された文字コードを用いる.
            encoding = documentController.getSettingsModel().getEncoding();
        }
        return encoding;
    }
}