package com.yww.image.service;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *      用于白底黑字的红色印章去除
 * </P>
 *
 * @author yww
 * @since 2023/5/23
 */
public class Recognize {

    /**
     * 遍历红色像素，根据像素数量判断是否存在红色印章
     *
     * @param filePath      图片路径
     * @return              true表示可能存在红色印章
     */
    public static Boolean recognizeRed(String filePath) {
        Mat mat = Imgcodecs.imread(filePath);
        // 转为HSV空间
        Mat hsv = new Mat();
        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV);
        int nums = 0;

        for (int i = 0; i < hsv.rows(); i++) {
            for (int j = 0; j < hsv.cols(); j++) {
                double[] clone = hsv.get(i, j).clone();
                double h = clone[0];
                double s = clone[1];
                double v = clone[2];
                // 红色的hsv范围判断
                if ((h > 0 && h < 10) || (h > 156 && h < 180)) {
                    if (s > 43 && s < 255) {
                        if (v < 255 && v > 46) {
                            nums++;
                        }
                    }
                }

            }
        }
        return nums > 8000;
    }

    /**
     * 去除红色印章，只是红色通道中的去除，图片会有灰色变化
     *
     * @param filePath  图片路径
     * @param dst       去除后图片保存地址
     */
    public static void removeRed(String filePath, String dst) {
        Mat mat = Imgcodecs.imread(filePath);

        // 分离红色通道
        List<Mat> matList = new ArrayList<>();
        Core.split(mat, matList);
        Mat red = matList.get(2);

        // 红色通道二值化
        Mat redThresh = new Mat();
        Imgproc.threshold(red, redThresh, 120, 255, Imgproc.THRESH_BINARY);

        Imgcodecs.imwrite(dst, redThresh);
    }

}
