package com.yww.image.service;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

/**
 * <p>
 *      网上传的三种清晰度计算方式
 * </P>
 *
 * @author yww
 * @since 2023/11/26
 */
public class Clarity {

    /**
     * Tenengrad梯度方法计算清晰度
     * Tenengrad梯度方法利用Sobel算子分别计算水平和垂直方向的梯度，同一场景下梯度值越高，图像越清晰。
     *
     * @param image  图片矩阵
     * @return      图片清晰度
     */
    public static double tenengrad(Mat image) {
        // 图片灰度化
        Mat grayImage = image.clone();
        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);

        // Sobel算子
        Mat sobelImage = new Mat();
        Imgproc.Sobel(grayImage, sobelImage, CvType.CV_16U, 1, 1);

        Scalar mean = Core.mean(sobelImage);
        double meanValue = mean.val[0];

        // 释放内存
        sobelImage.release();
        grayImage.release();

        return meanValue;
    }

    /**
     * Laplacian方法计算清晰度
     *
     * @param image  图片矩阵
     * @return      图片清晰度
     */
    public static double laplacian(Mat image) {
        // 图片灰度化
        Mat grayImage = image.clone();
        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);

        // Laplacian算子
        Mat laplacian = new Mat();
        Imgproc.Laplacian(grayImage, laplacian, CvType.CV_16U);

        Scalar mean = Core.mean(laplacian);
        double meanValue = mean.val[0];

        // 释放内存
        laplacian.release();
        grayImage.release();

        return meanValue;
    }

    /**
     * 通过灰度方差获取图片清晰度
     * 对焦清晰的图像相比对焦模糊的图像，它的数据之间的灰度差异应该更大，即它的方差应该较大，可以通过图像灰度数据的方差来衡量图像的清晰度，方差越大，表示清晰度越好。
     *
     * @param image  图片矩阵
     * @return      图片清晰度
     */
    public static double variance(Mat image) {
        // 图片灰度化
        Mat grayImage = image.clone();
        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);

        // 计算灰度图像的标准差
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stdDev = new MatOfDouble();
        Core.meanStdDev(grayImage, mean, stdDev);

        double meanValue = stdDev.get(0, 0)[0];

        // 释放内存
        mean.release();
        stdDev.release();
        grayImage.release();

        return meanValue;
    }

}
