package com.yww.image.util;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * <p>
 *      去黑边
 *      扫描图片四周像素，判断每行或每列的平均灰度值，若是接近黑色直接去除
 * </p>
 *
 * @author yww
 * @since 2023/3/10 15:01
 */
public class RemoveBlackUtil2 {

    /**
     * 去黑边"全黑"阈值
     */
    private static final Integer BLACK_VALUE = 100;

    public static void remove(String src, String dst) {
        Mat mat = Imgcodecs.imread(src);
        Mat res = removeBlackEdge(mat);
        OpencvUtil.saveImage(res, dst);
        mat.release();
        res.release();
    }

    /**
     * 去除图片黑边，若无黑边，则原图返回。默认“全黑”阈值为 {@code BLACK_VALUE}
     *
     * @param srcMat 预去除黑边的Mat
     * @return 去除黑边之后的Mat
     */
    private static Mat removeBlackEdge(Mat srcMat) {
        return removeBlackEdge(srcMat, BLACK_VALUE);
    }

    /**
     * 去除图片黑边，若无黑边，则原图返回。
     *
     * @param blackValue 一般低于5的已经是很黑的颜色了
     * @param srcMat     源Mat对象
     * @return Mat对象
     */
    private static Mat removeBlackEdge(Mat srcMat, int blackValue) {
        // 灰度化
        Mat grayMat = gray(srcMat);
        // 定义边界
        int topRow = 0;
        int leftCol = 0;
        int rightCol = grayMat.width() - 1;
        int bottomRow = grayMat.height() - 1;

        // 上方黑边判断
        for (int row = 0; row < grayMat.height(); row++) {
            if (sum(grayMat.row(row)) / grayMat.width() < blackValue) {
                topRow = row;
            } else {
                break;
            }
        }
        // 左边黑边判断
        for (int col = 0; col < grayMat.width(); col++) {
            if (sum(grayMat.col(col)) / grayMat.height() < blackValue) {
                leftCol = col;
            } else {
                break;
            }
        }
        // 右边黑边判断
        for (int col = grayMat.width() - 1; col > 0; col--) {
            if (sum(grayMat.col(col)) / grayMat.height() < blackValue) {
                rightCol = col;
            } else {
                break;
            }
        }
        // 下方黑边判断
        for (int row = grayMat.height() - 1; row > 0; row--) {
            if (sum(grayMat.row(row)) / grayMat.width() < blackValue) {
                bottomRow = row;
            } else {
                break;
            }
        }

        int x = leftCol;
        int y = topRow;
        int width = rightCol - leftCol;
        int height = bottomRow - topRow;

        grayMat.release();
        if (leftCol == 0 && rightCol == grayMat.width() - 1 && topRow == 0 && bottomRow == grayMat.height() - 1) {
            return srcMat;
        }
        return cut(srcMat, x, y, width, height);
    }

    /**
     * 像素求和
     *
     * @param mat mat
     * @return sum
     */
    public static int sum(Mat mat) {
        int sum = 0;
        for (int row = 0; row < mat.height(); row++) {
            for (int col = 0; col < mat.width(); col++) {
                sum += (int) mat.get(row, col)[0];
            }
        }
        return sum;
    }

    /**
     * 灰度处理 BGR灰度处理
     *
     * @param src 原图Mat
     * @return Mat 灰度后的Mat
     */
    private static Mat gray(Mat src) {
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        return gray;
    }

    /**
     * 按照指定的尺寸截取Mat，坐标原点为左上角
     *
     * @param src    源Mat
     * @param x      x
     * @param y      y
     * @param width  width
     * @param height height
     * @return 截取后的Mat
     */
    private static Mat cut(Mat src, int x, int y, int width, int height) {
        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }
        if (width > src.width()) {
            width = src.width();
        }
        if (height > src.height()) {
            height = src.height();
        }
        // 截取尺寸
        Rect rect = new Rect(x, y, width, height);
        return new Mat(src, rect);
    }

}
