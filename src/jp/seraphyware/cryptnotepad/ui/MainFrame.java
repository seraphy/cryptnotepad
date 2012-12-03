package jp.seraphyware.cryptnotepad.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyVetoException;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import jp.seraphyware.cryptnotepad.Main;
import jp.seraphyware.cryptnotepad.util.ConfigurationDirUtilities;
import jp.seraphyware.cryptnotepad.util.XMLResourceBundle;

/**
 * メインフレーム
 * 
 * @author seraphy
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 8358190990080417295L;

    /**
     * ロガー.<br>
     */
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    /**
     * リソースバンドル
     */
    private ResourceBundle resource;

    /**
     * MDIフレーム(デスクトップ)
     */
    private JDesktopPane desktop;

    /**
     * コンストラクタ
     */
    public MainFrame() {
        try {
            resource = ResourceBundle.getBundle(getClass().getName(),
                    XMLResourceBundle.CONTROL);
            init();

        } catch (RuntimeException ex) {
            dispose();
            throw ex;
        }
    }

    /**
     * フレームを初期化する.
     */
    private void init() {
        // ウィンドウの閉じるイベント
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClosing();
            }
        });

        // タイトル
        setTitle(resource.getString("mainframe.title"));

        // MDIフレーム
        desktop = new JDesktopPane();
        desktop.setBackground(Color.lightGray);

        // レイアウト
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(200);

        splitPane.add(createFileTreePanel());
        splitPane.add(desktop);

        contentPane.add(splitPane);
    }

    /**
     * MDI子ウィンドウを開く
     * 
     * @param file
     *            対象ファイル
     * @return 生成された子ウィンドウ
     */
    private JInternalFrame createChildFrame(File file) {

        JInternalFrame internalFrame = new JInternalFrame();

        String title = file.getName();
        internalFrame.setTitle(title);

        internalFrame.setMaximizable(true);
        internalFrame.setResizable(true);
        internalFrame.setIconifiable(true);
        internalFrame.setClosable(true);

        desktop.add(internalFrame);

        internalFrame.setSize(200, 200);
        internalFrame.setLocation(0, 0);
        internalFrame.setVisible(true);

        try {
            internalFrame.setMaximum(true);

        } catch (PropertyVetoException ex) {
            logger.log(Level.FINE, ex.toString());
        }

        return internalFrame;
    }

    /**
     * ファイル一覧パネルを作成する.
     * 
     * @return
     */
    private JPanel createFileTreePanel() {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Files"));

        File rootDir = ConfigurationDirUtilities.getApplicationBaseDir();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootDir);

        LinkedList<DefaultMutableTreeNode> queue = new LinkedList<DefaultMutableTreeNode>();
        queue.add(root);

        while (!queue.isEmpty()) {
            DefaultMutableTreeNode dirNode = queue.pop();
            File dir = (File) dirNode.getUserObject();

            logger.log(Level.FINE, "dir=" + dir);
            File[] files = dir.listFiles();
            Arrays.sort(files);
            for (File file : files) {
                logger.log(Level.FINE, "file=" + file);

                DefaultMutableTreeNode node = new DefaultMutableTreeNode(file);
                dirNode.add(node);

                if (file.isDirectory()) {
                    queue.push(node);
                }
            }
        }

        final DefaultTreeModel model = new DefaultTreeModel(root);
        final JTree tree = new JTree(model);
        tree.setRootVisible(false);

        JScrollPane scr = new JScrollPane(tree);

        if (Main.isMacOSX()) {
            scr.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            scr.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        }

        panel.add(scr);

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
                    TreePath select = tree.getSelectionPath();
                    if (select != null) {
                        Object[] path = select.getPath();
                        if (path != null && path.length > 0) {
                            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path[path.length - 1];
                            File file = (File) node.getUserObject();
                            if (!file.isDirectory()) {
                                createChildFrame(file);
                            }
                        }
                    }
                }
            }
        });

        return panel;
    }

    /**
     * 破棄する場合
     */
    protected void onClosing() {
        dispose();
        logger.log(Level.INFO, "dispose mainframe");
    }
}
