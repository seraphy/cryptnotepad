package jp.seraphyware.cryptnotepad.ui;

import static java.lang.Math.min;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

/**
 * サンプルピクチャ用パネル.<br>
 * ピクチャの自動縮小と等倍表示切り替えをサポートする.<br>
 * 
 * @author seraphy
 */
public class SamplePicturePanel extends JPanel {

    private static final long serialVersionUID = 4026181978500938152L;

    /**
     * 表示する画像イメージ、なければnull
     */
    protected BufferedImage samplePicture;

    /**
     * イメージの表示・非表示フラグ
     */
    protected boolean visiblePicture = true;

    /**
     * サンプルイメージの背景色
     */
    protected Color sampleImageBgColor = Color.gray;

    /**
     * 画像がない場合に表示するテキスト.
     */
    protected String alternateText = "";

    /**
     * Fit/Fullの切り替え.<br>
     * 実寸表示の場合はtrue.<br>
     */
    protected boolean enableRealsize;

    /**
     * マウスドラッグによるスクロールのサポート
     */
    protected ScrollPaneDragScrollSupport scrollSupport;

    /**
     * インスタンスイニシャライザ.
     */
    {
        setMinimumSize(new Dimension(64, 64));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // 正確に2回
                    onDblClick();
                    e.consume();
                }
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();

        // 親がJScrollPaneである場合、
        // マウスのドラッグによるスクロールをサポートするようにマウスリスナをセットアップする.
        Component parent = getParent();
        if (parent != null && (parent instanceof JViewport)) {
            Component gparent = parent.getParent();
            if (gparent != null && (gparent instanceof JScrollPane)) {
                JScrollPane scrollPane = (JScrollPane) gparent;
                scrollSupport = new ScrollPaneDragScrollSupport(scrollPane);
                scrollSupport.installDraggingListener(this, null);
            }
        }
    }

    @Override
    public void removeNotify() {
        if (scrollSupport != null) {
            scrollSupport.uninstallDraggingListener();
            scrollSupport = null;
        }
        super.removeNotify();
    }

    /**
     * コンストラクタ.
     */
    public SamplePicturePanel() {
        this(null);
    }

    /**
     * 画像を指定して構築する.
     * 
     * @param samplePicture
     *            画像、なければnull
     */
    public SamplePicturePanel(BufferedImage samplePicture) {
        this(null, null);
    }

    /**
     * 画像と代替テキストを指定して構築する.
     * 
     * @param samplePicture
     *            画像、なければnull
     * @param alternateText
     *            代替テキスト、なければnull可
     */
    public SamplePicturePanel(BufferedImage samplePicture, String alternateText) {
        super();
        if (alternateText == null) {
            alternateText = "";
        }
        this.samplePicture = samplePicture;
        this.alternateText = alternateText;
        this.enableRealsize = false;
        adjustPreferrerdSize(false);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        super.paintComponent(g);

        if (samplePicture != null && isVisiblePicture()) {
            Rectangle rct = getBounds();
            Insets insets = getInsets();
            int x = insets.left;
            int y = insets.top;
            int w = rct.width - insets.left - insets.right;
            int h = rct.height - insets.top - insets.bottom;

            int imgW = samplePicture.getWidth();
            int imgH = samplePicture.getHeight();

            double factor1 = (double) h / (double) imgH; // 縦を納めた場合の、縦の縮小率
            double factor2 = (double) w / (double) imgW; // 横を納めた場合の、横の縮小率
            double factor = min(factor1, factor2); // 縦横を納めるのに最低必要な縮小率
            int scaledW = (int) (imgW * factor);
            int scaledH = (int) (imgH * factor);
            int offset_x = (w - scaledW) / 2;
            int offset_y = (h - scaledH) / 2;

            Object renderingHint;
            if (factor <= 1.) {
                // 等倍未満
                renderingHint = RenderingHints.VALUE_INTERPOLATION_BILINEAR;

            } else if (factor <= 2.) {
                // 2倍まで
                renderingHint = RenderingHints.VALUE_INTERPOLATION_BICUBIC;

            } else {
                // それ以上
                renderingHint = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
            }
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, renderingHint);

            g.drawImage(samplePicture, x + offset_x, y + offset_y, x + offset_x
                    + scaledW, y + offset_y + scaledH, 0, 0, imgW, imgH,
                    sampleImageBgColor, null);

        } else if (alternateText.length() > 0) {
            Rectangle rct = getBounds();
            Insets insets = getInsets();
            int x = insets.left;
            int y = insets.top;
            int w = rct.width - insets.left - insets.right;
            int h = rct.height - insets.top - insets.bottom;

            Font font = g.getFont();
            FontRenderContext frc = g.getFontRenderContext();
            int textHeight = (int) font.getMaxCharBounds(frc).getHeight();
            Shape clipOld = g.getClip();
            g.setClip(x, y, w, h);
            g.drawString(alternateText, x, y + textHeight);
            g.setClip(clipOld);
        }
    }

    /**
     * 画像をフィット・フィット解除する.
     * 
     * @param fullsize
     *            実寸表示するか?
     */
    public void adjustPreferrerdSize(boolean fullsize) {
        Dimension minSize = getMinimumSize();
        Dimension siz = minSize;

        if (samplePicture != null) {
            int div = fullsize ? 1 : 2;
            Insets insets = getInsets();
            siz = new Dimension(samplePicture.getWidth() / div + insets.left
                    + insets.right, samplePicture.getHeight() / div
                    + insets.top + insets.bottom);
        }

        siz.width = Math.max(minSize.width, siz.width);
        siz.height = Math.max(minSize.height, siz.height);

        Dimension ord = getPreferredSize();
        if (ord == null || !ord.equals(siz)) {
            setPreferredSize(siz);
            revalidate();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Container parent = getParent();
        if (!enableRealsize && parent != null && parent instanceof JViewport) {
            JViewport viewport = (JViewport) parent;
            Dimension siz = viewport.getExtentSize();
            Insets insets = viewport.getInsets();
            Dimension preferredSize = new Dimension(siz.width - insets.left
                    - insets.right, siz.height - insets.top - insets.bottom);
            return preferredSize;
        }
        return super.getPreferredSize();
    }

    protected void onDblClick() {
        Container parent = getParent();
        if (parent != null && parent instanceof JViewport) {
            enableRealsize = !enableRealsize;
            adjustPreferrerdSize(enableRealsize);
        }
    }

    public boolean isVisiblePicture() {
        return visiblePicture;
    }

    public void setVisiblePicture(boolean visiblePicture) {
        if (this.visiblePicture != visiblePicture) {
            this.visiblePicture = visiblePicture;
            repaint();
        }
    }

    public void setSamplePicture(BufferedImage samplePicture) {
        if (this.samplePicture != samplePicture) {
            this.samplePicture = samplePicture;
            enableRealsize = false;
            adjustPreferrerdSize(false);
            repaint();
        }
    }

    public BufferedImage getSamplePictrue() {
        return this.samplePicture;
    }

    public Color getSamplePictureBgColor() {
        return this.sampleImageBgColor;
    }

    public void setSamplePictureBgColor(Color color) {
        if (color == null) {
            throw new IllegalArgumentException();
        }
        if (!sampleImageBgColor.equals(color)) {
            this.sampleImageBgColor = color;
            repaint();
        }
    }

    public String getAlternateText() {
        return alternateText;
    }

    public void setAlternateText(String alternateText) {
        if (alternateText == null) {
            alternateText = "";
        }
        if (!this.alternateText.equals(alternateText)) {
            this.alternateText = alternateText;
            repaint();
        }
    }
}
