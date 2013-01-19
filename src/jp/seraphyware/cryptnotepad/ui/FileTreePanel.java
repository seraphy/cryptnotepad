package jp.seraphyware.cryptnotepad.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.EventListenerList;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import jp.seraphyware.cryptnotepad.Main;
import jp.seraphyware.cryptnotepad.model.ApplicationSettings;
import jp.seraphyware.cryptnotepad.util.ErrorMessageHelper;

/**
 * ファイルツリーのパネル
 * 
 * @author seraphy
 */
public class FileTreePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /**
     * 選択ファイルイベント用のコマンド名
     */
    public static final String COMMAND_SELECTFILE = "selectFile";

    /**
     * 選択ファイルプロパティ変更イベント用のキー
     */
    public static final String PROPERTY_SELECTEDFILE = "selectedFile";

    /**
     * ロガー.<br>
     */
    private static final Logger logger = Logger.getLogger(FileTreePanel.class
            .getName());

    /**
     * アプリケーション設定
     */
    private ApplicationSettings appConfig;

    /**
     * ツリーモデル
     */
    private DefaultTreeModel model;

    /**
     * ツリー
     */
    private JTree tree;

    /**
     * イベントリスナのリスト
     */
    private final EventListenerList listeners = new EventListenerList();

    /**
     * 現在選択ファイル.<br>
     * (フォーカスされているファイルではない.)<br>
     */
    private File selectedFile;

    /**
     * コンストラクタ
     */
    public FileTreePanel() {
        super(new BorderLayout());

        appConfig = ApplicationSettings.getInstance();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        model = new DefaultTreeModel(root);
        tree = new JTree(model);
        tree.setRootVisible(false);
        tree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);

        JScrollPane scr = new JScrollPane(tree);

        if (Main.isMacOSX()) {
            scr.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
            scr.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        }

        add(scr);

        DefaultTreeCellRenderer treeCellRenderer = new DefaultTreeCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTreeCellRendererComponent(JTree tree,
                    Object value, boolean sel, boolean expanded, boolean leaf,
                    int row, boolean hasFocus) {

                if (value instanceof DefaultMutableTreeNode) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                    File file = (File) node.getUserObject();
                    value = file.getName();
                }

                return super.getTreeCellRendererComponent(tree, value, sel,
                        expanded, leaf, row, hasFocus);
            }
        };
        tree.setCellRenderer(treeCellRenderer);

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // ダブルクリックの場合
                    onDblClick();
                }
            }
        });

        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    // エンターキーはダブルクリックと同じ.
                    onDblClick();
                }
            }
        });

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    // 右クリック押下で、ノードを選択する.
                    TreePath tp = tree.getPathForLocation(e.getX(), e.getY());
                    if (tp != null) {
                        tree.setSelectionPath(tp);
                    }
                }
            }
        });

        AbstractAction actRefresh = new AbstractAction("Refresh") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        };

        AbstractAction actOpen = new AbstractAction("Open") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                onDblClick();
            }
        };

        AbstractAction actRename = new AbstractAction("Rename") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                onRename();
            }
        };

        // キーボードマップ

        ActionMap am = tree.getActionMap();
        InputMap im = tree.getInputMap(JComponent.WHEN_FOCUSED);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), actRefresh);
        am.put(actRefresh, actRefresh);

        // コンテキストメニュー

        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(new JMenuItem(actRefresh));
        popupMenu.add(new JSeparator());
        popupMenu.add(new JMenuItem(actOpen));
        popupMenu.add(new JMenuItem(actRename));

        tree.setComponentPopupMenu(popupMenu);
    }

    protected void onRename() {
        File file = getFocusedFile();
        if (file == null) {
            return;
        }

        SecureRandom rng = new SecureRandom();
        byte[] data = new byte[8];
        rng.nextBytes(data);

        StringBuilder buf = new StringBuilder();
        for (byte d : data) {
            buf.append(String.format("%02x", d));
        }

        final JTextField txtNewName = new JTextField();
        txtNewName.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorRemoved(AncestorEvent event) {
                // do nothing.
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
                // do nothing.
            }

            @Override
            public void ancestorAdded(AncestorEvent event) {
                txtNewName.requestFocusInWindow();
            }
        });

        JTextField txtOldName = new JTextField();
        txtOldName.setEditable(false);

        Box pnl = Box.createVerticalBox();
        pnl.add(new JLabel("Old File Name"));
        pnl.add(txtOldName);
        pnl.add(new JLabel("New File Name"));
        pnl.add(txtNewName);

        txtOldName.setText(file.getName());
        txtNewName.setText(buf.toString());

        int ret = JOptionPane.showConfirmDialog(this, pnl, "RENAME",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (ret != JOptionPane.OK_OPTION) {
            return;
        }

        String newName = txtNewName.getText().trim();

        if (newName.equals(file.getName())) {
            // 名前が変更されていない場合
            return;
        }

        if (newName.length() > 0) {
            try {
                logger.log(Level.INFO, "rename " + file + " => " + newName);
                file.renameTo(new File(file.getParentFile(), newName));
                refresh();

            } catch (Exception ex) {
                ErrorMessageHelper.showErrorDialog(this, ex);
            }
        }
    }

    /**
     * ツリーのファイル一覧をリフレッシュする.
     */
    public void refresh() {
        File rootDir = appConfig.getContentsDir();
        if (rootDir == null) {
            throw new IllegalStateException(
                    "ApplicationSettings#contentsDirが未設定です.");
        }

        // 現在選択しているアイテムのパスを取得する.
        File currentSelection = getFocusedFile();
        DefaultMutableTreeNode restoreNode = null;

        // ルートノード
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootDir);

        // ディレクトリ探索のためのキュー
        LinkedList<DefaultMutableTreeNode> queue = new LinkedList<DefaultMutableTreeNode>();
        queue.add(root);

        // ファイルツリーをトラバーサルする.
        while (!queue.isEmpty()) {
            DefaultMutableTreeNode dirNode = queue.pop();
            File dir = (File) dirNode.getUserObject();

            logger.log(Level.FINE, "dir=" + dir);
            File[] files = null;
            try {
                files = dir.listFiles();

            } catch (Exception ex) {
                // ファイル一覧の取得に失敗した場合は空とする.
                logger.log(Level.INFO, "fileTreeTraversalError." + ex, ex);
            }

            if (files == null) {
                // ファイル一覧が取得できなかった場合
                files = new File[0];
            }

            Arrays.sort(files);

            for (File file : files) {
                logger.log(Level.FINER, "file=" + file);

                DefaultMutableTreeNode node = new DefaultMutableTreeNode(file);
                dirNode.add(node);

                if (file.isDirectory()) {
                    queue.push(node);
                }

                // リフレッシュ前に選択中のファイルがみつかった場合は、このノードを覚える
                if (file.equals(currentSelection)) {
                    restoreNode = node;
                }
            }
        }

        model.setRoot(root);

        // リフレッシュ前の選択を復元する.
        if (restoreNode != null) {
            TreePath path = new TreePath(model.getPathToRoot(restoreNode));
            tree.setSelectionPath(path);
        }
    }

    /**
     * ツリー上で現在フォーカスされているファイルまたはディレクトリを取得する.<br>
     * 該当がなければnullを返す.<br>
     * 
     * @return 選択されているファイル、ディレクトリ、もしくはnull
     */
    public File getFocusedFile() {
        TreePath select = tree.getSelectionPath();
        if (select != null) {
            Object[] path = select.getPath();
            if (path != null && path.length > 0) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path[path.length - 1];
                File file = (File) node.getUserObject();
                return file;
            }
        }
        return null;
    }

    /**
     * ダブルクリック時のハンドラ
     */
    protected void onDblClick() {
        File file = getFocusedFile();
        if (file != null && !file.isDirectory()) {
            setSelectedFile(file);
            ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                    COMMAND_SELECTFILE);
            fireActionEvent(e);
        }
    }

    public void addActionListener(ActionListener l) {
        listeners.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        listeners.remove(ActionListener.class, l);
    }

    protected void fireActionEvent(ActionEvent e) {
        if (e != null) {
            for (ActionListener l : listeners
                    .getListeners(ActionListener.class)) {
                l.actionPerformed(e);
            }
        }
    }

    public File getSelectedFile() {
        return selectedFile;
    }

    public void setSelectedFile(File selectedFile) {
        File oldValue = this.selectedFile;
        this.selectedFile = selectedFile;
        firePropertyChange(PROPERTY_SELECTEDFILE, oldValue, selectedFile);
    }
}
