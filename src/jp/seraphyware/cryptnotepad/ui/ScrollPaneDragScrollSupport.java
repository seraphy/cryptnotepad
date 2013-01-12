package jp.seraphyware.cryptnotepad.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

/**
 * JScrollPaneでマウスによるドラッグスクロールをサポートするためのヘルパクラス.<br>
 * 
 * @author seraphy
 */
public class ScrollPaneDragScrollSupport {

    /**
     * 対象となるスクロールペイン
     */
    private JScrollPane scrollPane;

    /**
     * ホイールによるスクロールで移動する分割単位.<br>
     * 表示されているエリアをn等分割したサイズごとにスクロールする.<br>
     */
    private int wheelDivider;

    /**
     * JScrollPaneを指定して構築する.
     * 
     * @param scrollPane
     */
    public ScrollPaneDragScrollSupport(JScrollPane scrollPane) {
        if (scrollPane == null) {
            throw new IllegalArgumentException();
        }
        this.scrollPane = scrollPane;

        wheelDivider = 20;
    }

    /**
     * ドラッグ開始位置を示す.<br>
     * スクロールが調整されるたびに新しい座標にセットし直す.<br>
     * ドラッグ中であれば非nullとなる.<br>
     * ドラッグが完了した場合、もしくはドラッグが開始されていなければnullとなる.<br>
     */
    private Point dragPt;

    /**
     * ドラッグによるスクロールが可能か?<br>
     * 垂直・水平のいずれのスクロールバーがない状況ではドラッグは開始されない.<br>
     * 
     * @return ドラッグによるスクロールが可能である場合はtrue
     */
    public boolean isDragScrollable() {
        JViewport vp = scrollPane.getViewport();
        Dimension viewSize = vp.getViewSize();
        Dimension visibleSize = vp.getExtentSize();
        if (viewSize.width <= visibleSize.width
                && viewSize.height <= visibleSize.height) {
            // ビューポートにビューが全部表示されていればドラッグは開始されない.
            return false;
        }
        return true;
    }

    /**
     * 現在のドラッグ位置を取得する.<br>
     * ドラッグが開始されていなければnullとなる.<br>
     * 
     * @return ドラッグ位置
     */
    public Point getDragPt() {
        return dragPt;
    }

    /**
     * 現在ドラッグ中であるか?
     * 
     * @return ドラッグ中であればtrue
     */
    public boolean isDragging() {
        return dragPt != null;
    }

    /**
     * カーソルを設定する.
     * 
     * @param cursor
     *            カーソル
     */
    protected void setCursor(Cursor cursor) {
        scrollPane.setCursor(cursor);
    }

    /**
     * ドラッグの開始または終了を行う.<br>
     * すでに開始済みで開始要求するか、開始されておらず停止要求した場合は何もしない.<br>
     * 
     * @param start
     *            開始する場合はtrue、終了する場合はfalse
     * @param mousePt
     *            開始位置
     */
    public void drag(boolean start, Point mousePt) {
        if (start) {
            if (dragPt == null) {
                JViewport vp = scrollPane.getViewport();
                Dimension viewSize = vp.getViewSize();
                Dimension visibleSize = vp.getExtentSize();
                if (viewSize.width <= visibleSize.width
                        && viewSize.height <= visibleSize.height) {
                    // ビューポートにビューが全部表示されていればドラッグは開始されない.
                    dragPt = null;
                    return;
                }

                // ドラッグ中であることを示す
                dragPt = mousePt;
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

        } else if (dragPt != null) {
            // ドラッグ中であれば解除する.
            // (ドラッグ解除済みであれば何もしない.)
            dragging(mousePt);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            dragPt = null;
        }
    }

    /**
     * マウスによるドラッグによるスクロール.<br>
     * 前回位置(初回なら開始位置)との差分からスクロール量を判定する.<br>
     * 
     * @param mousePt
     *            現在のマウス位置
     */
    public void dragging(Point mousePt) {
        if (dragPt == null || mousePt == null) {
            // 前回値がないか今回値がない場合は何もしない.
            return;
        }

        // 前回座標との差分を求める
        int diff_x = dragPt.x - mousePt.x;
        int diff_y = dragPt.y - mousePt.y;

        scroll(diff_x, diff_y);

        // 現在位置を記録
        dragPt = mousePt;
    }

    /**
     * マウス座標単位で指定したオフセット分スクロールする.
     * 
     * @param diff_x
     *            水平方向スクロール数
     * @param diff_y
     *            垂直方向スクロール数
     */
    public void scroll(int diff_x, int diff_y) {
        if (diff_x == 0 && diff_y == 0) {
            return;
        }

        JViewport vp = scrollPane.getViewport();
        Dimension viewSize = vp.getViewSize();
        Dimension visibleSize = vp.getExtentSize();

        Point vpt = vp.getViewPosition();

        vpt.x += diff_x;
        if (vpt.x < 0) {
            vpt.x = 0;

        } else if (vpt.x + visibleSize.width > viewSize.width) {
            // はみ出た分を引く
            vpt.x -= (vpt.x + visibleSize.width - viewSize.width);
        }

        vpt.y += diff_y;
        if (vpt.y < 0) {
            vpt.y = 0;

        } else if (vpt.y + visibleSize.height > viewSize.height) {
            // はみ出た分を引く
            vpt.y -= (vpt.y + visibleSize.height - viewSize.height);
        }

        vp.setViewPosition(vpt);
    }

    /**
     * ホイールによるスクロール量の分割数.<br>
     * 表示されている領域に対してn等分割したサイズを 一回あたりのスクロール量とする.<br>
     * 
     * @return スクロール量の分割数
     */
    public int getWheelDivider() {
        return wheelDivider;
    }

    public void setWheelFactor(int wheelDivider) {
        this.wheelDivider = Math.max(2, wheelDivider);
    }

    /**
     * マウスホイールによる水平・垂直スクロールを行うためのコンビニエスとメソッド.<br>
     * シフトキーで水平、それ以外は垂直とする.<br>
     * 
     * @param e
     *            ホイールイベント
     */
    public void scrollByWheel(final MouseWheelEvent e) {
        if (e == null) {
            return;
        }

        JViewport vp = scrollPane.getViewport();
        Dimension visibleSize = vp.getExtentSize();

        int rotation = e.getWheelRotation();

        int diff_x = 0;
        int diff_y = 0;

        if (e.isShiftDown()) {
            // 水平スクロール
            int unit = visibleSize.width / getWheelDivider();
            diff_x = rotation * unit;

        } else {
            // 垂直スクロール
            int unit = visibleSize.height / getWheelDivider();
            diff_y = rotation * unit;
        }

        scroll(diff_x, diff_y);
    }

    /**
     * セットアップしたリスナを保存するもの
     */
    private MouseListener mouseListener;

    /**
     * セットアップしたリスナを保存するもの
     */
    private MouseMotionListener mouseMotionListener;

    /**
     * セットアップしたリスナを保存するもの
     */
    private MouseWheelListener mouseWheelListener;

    /**
     * リスナをセットアップしたコンポーネント
     */
    private JComponent installTarget;

    /**
     * ドラッグの開始に相応しいボタンプレスであるか判定するためのインターフェイス.
     * 
     * @author seraphy
     */
    public interface DragPridicator {

        /**
         * このマウスイベントでドラッグ開始しても良いか?
         * 
         * @param e
         *            マウスイベント
         * @return ドラッグを開始しても良い場合はtrue
         */
        boolean isDraggable(MouseEvent e);

    }

    /**
     * マウスによるドラッグをサポートする、一般的なマウスリスナとマウスモーションリスナをセットアップする.<br>
     * すでに登録済みであれば何もしない.<br>
     * このメソッドはマウスリスナに特別な処理が必要ない場合に定型的な処理を代行するコンビニエスメソッドです.<br>
     * 
     * @param comp
     *            マウスリスナをセットアップするターゲットのコンポーネント
     * @param predicator
     *            マウスによるドラッグの開始を行うか判定するためのオブジェクト、nullの場合は不問
     */
    public void installDraggingListener(final JComponent comp,
            final DragPridicator predicator) {
        if (comp == null) {
            throw new IllegalArgumentException();
        }
        if (mouseListener == null) {
            mouseListener = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (predicator == null || predicator.isDraggable(e)) {
                        Point pt = SwingUtilities.convertPoint(comp,
                                e.getPoint(), scrollPane);
                        drag(true, pt);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (predicator == null || predicator.isDraggable(e)) {
                        Point pt = SwingUtilities.convertPoint(comp,
                                e.getPoint(), scrollPane);
                        drag(false, pt);
                    }
                }
            };
            // リスナを登録する.
            comp.addMouseListener(mouseListener);
        }
        if (mouseMotionListener == null) {
            mouseMotionListener = new MouseMotionListener() {
                public void mouseMoved(MouseEvent e) {
                    // 何もしない.
                }

                public void mouseDragged(MouseEvent e) {
                    Point pt = SwingUtilities.convertPoint(comp, e.getPoint(),
                            scrollPane);
                    dragging(pt);
                }
            };
            // リスナを登録する.
            comp.addMouseMotionListener(mouseMotionListener);
        }

        if (mouseWheelListener == null) {
            mouseWheelListener = new MouseWheelListener() {
                public void mouseWheelMoved(MouseWheelEvent e) {
                    scrollByWheel(e);
                    e.consume();
                }
            };
            // ホイールスクロールのデフォルト設定を解除する.
            scrollPane.setWheelScrollingEnabled(false);
            // リスナを登録する.
            comp.addMouseWheelListener(mouseWheelListener);
        }

        installTarget = comp;
    }

    /**
     * セットアップしたリスナを解除する.<br>
     * 登録されていない場合は何もしない.<br>
     */
    public void uninstallDraggingListener() {
        if (mouseListener != null && installTarget != null) {
            installTarget.removeMouseListener(mouseListener);
            mouseListener = null;
        }
        if (mouseMotionListener != null && installTarget != null) {
            installTarget.removeMouseMotionListener(mouseMotionListener);
            mouseMotionListener = null;
        }
        if (mouseWheelListener != null && installTarget != null) {
            installTarget.removeMouseWheelListener(mouseWheelListener);
            mouseWheelListener = null;
        }
    }
}
