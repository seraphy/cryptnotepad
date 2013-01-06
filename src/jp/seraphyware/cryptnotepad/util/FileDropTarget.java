package jp.seraphyware.cryptnotepad.util;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ファイルドロップターゲット.<br>
 * Windows/Macと、Linuxの両方のデスクトップのドロップをサポートする.
 * 
 * @author seraphy
 */
public abstract class FileDropTarget extends DropTargetAdapter {

    /**
     * ロガー
     */
    private final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * ドロップハンドラ
     */
    public void drop(DropTargetDropEvent dtde) {
        try {
            // urlListFlavor (RFC 2483 for the text/uri-list format)
            DataFlavor uriListFlavor;
            try {
                uriListFlavor = new DataFlavor(
                        "text/uri-list;class=java.lang.String");
            } catch (ClassNotFoundException ex) {
                logger.log(Level.WARNING, "urlListFlavor is not supported.", ex);
                uriListFlavor = null;
            }

            final List<File> dropFiles = new ArrayList<File>();

            for (DataFlavor flavor : dtde.getCurrentDataFlavors()) {
                logger.log(Level.FINE, "flavor: " + flavor);

                if (DataFlavor.javaFileListFlavor.equals(flavor)) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings({ "unchecked", "rawtypes" })
                    List<File> files = (List) dtde.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    logger.log(Level.FINER,
                            "DragAndDrop files(javaFileListFlavor)=" + files);
                    dropFiles.addAll(files);
                    break;
                }

                if (uriListFlavor != null && uriListFlavor.equals(flavor)) {
                    // LinuxではjavaFileListFlavorではなく、text/uri-listタイプで送信される.
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    String uriList = (String) dtde.getTransferable()
                            .getTransferData(uriListFlavor);
                    logger.log(Level.FINER, "DragAndDrop files(text/uri-list)="
                            + uriList);
                    for (String fileStr : uriList.split("\r\n")) { // RFC2483によると改行コードはCRLF
                        fileStr = fileStr.trim();
                        if (fileStr.startsWith("#")) {
                            continue;
                        }
                        try {
                            URI uri = new URI(fileStr);
                            File dropFile = new File(uri);
                            dropFiles.add(dropFile);
                            break;

                        } catch (URISyntaxException ex) {
                            logger.log(Level.WARNING, "invalid drop file: "
                                    + fileStr, ex);
                        }
                    }
                    break;
                }
            }

            // 存在しないファイルを除去する.
            for (Iterator<File> ite = dropFiles.iterator(); ite.hasNext();) {
                File dropFile = ite.next();
                if (dropFile == null || !dropFile.exists()) {
                    ite.remove();
                }
            }

            // ドロップされたファイルを通知する.
            onDropFiles(dropFiles);

        } catch (UnsupportedFlavorException ex) {
            logger.log(Level.WARNING, "unsipported flovaor.", ex);
            onException(ex);

        } catch (IOException ex) {
            logger.log(Level.WARNING, "drop target failed.", ex);
            onException(ex);
        }
    }

    /**
     * ドロップされたファイルが通知される.<br>
     * 派生クラスで実装する必要がある.<br>
     * 
     * @param dropFiles
     *            ファイルのリスト
     */
    protected abstract void onDropFiles(List<File> dropFiles);

    /**
     * 例外が発生した場合に呼び出される.<br>
     * デフォルトでは何もしない.<br>
     * 
     * @param ex
     */
    protected void onException(Exception ex) {
        // do nothing.
    }
}