package jp.seraphyware.cryptnotepad.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyVetoException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import jp.seraphyware.cryptnotepad.Main;
import jp.seraphyware.cryptnotepad.util.XMLResourceBundle;

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
	 * コンストラクタ
	 */
	public MainFrame() {
		try {
			resource = ResourceBundle.getBundle(getClass().getName(), XMLResourceBundle.CONTROL);
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
		
		// レイアウト
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setDividerLocation(200);
		splitPane.setResizeWeight(0.2);
		
		splitPane.add(createFileTreePanel());
		splitPane.add(createDesktopPane());
		
		contentPane.add(splitPane);
	}
	
	private JDesktopPane createDesktopPane() {
		JDesktopPane desktop = new JDesktopPane();
		desktop.setBackground(Color.gray);
		
		JInternalFrame internalFrame = new JInternalFrame("doc");
		internalFrame.setMaximizable(true);
		internalFrame.setResizable(true);
		internalFrame.setIconifiable(true);
		internalFrame.setClosable(true);
		
		internalFrame.setSize(100, 200);
		internalFrame.setLocation(30, 30);
		
		internalFrame.setVisible(true);
		desktop.add(internalFrame);
		try {
			internalFrame.setMaximum(true);
			
		} catch (PropertyVetoException ex) {
			logger.log(Level.FINE, ex.toString());
		}
		
		return desktop;
	}
	
	private JPanel createFileTreePanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Files"));
		
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		
		for (int idx = 0; idx < 10; idx++) {
			DefaultMutableTreeNode node = new DefaultMutableTreeNode();
			node.setUserObject("idx:" + idx);
			root.add(node);
			for (int idx2 = 0; idx2 < 3; idx2++) {
				DefaultMutableTreeNode node2 = new DefaultMutableTreeNode();
				node2.setUserObject("idx2:" + idx2);
				node.add(node2);
			}
		}
		
		JTree tree = new JTree(root);
		tree.setRootVisible(false);
		panel.add(tree);
		
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
