package com.yww.image.service;

import com.yww.image.util.OpencvUtil;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * <p>
 *      图片亮度处理
 * </p>
 *
 * @author yww
 * @since 2023/4/26
 */
public class Brightness {

    static {
        OpencvUtil.load();
    }

    /**
     * 计算图片平均亮度
     * mean 获取Mat中各个通道的均值
     *
     * @param filePath  图片路径
     * @return          图片平均亮度值
     */
    public static double brightness(String filePath) {
        // 读取为灰度图片
        Mat grayImage = Imgcodecs.imread(filePath, Imgcodecs.IMREAD_GRAYSCALE);
        // 计算图像的平均亮度
        Scalar mean = Core.mean(grayImage);
        return mean.val[0];
    }

    /**
     * 图片亮度检测
     * cast为计算出的偏差值，小于1表示比较正常，大于1表示存在亮度异常；当cast异常时，da大于0表示过亮，da小于0表示过暗
     * 标准可以自己定义
     *
     * @param   filePath    图片路径
     * @return              [cast, da] [亮度值， 亮度异常值]
     */
    private double[] brightness2(String filePath) {
        Mat src = Imgcodecs.imread(filePath);
        // 灰度化，转为灰度图
        Mat gray = src.clone();
        // 获取原图的列数，RGB图像为3列
        if (3 == src.channels()) {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        } else {
            throw new RuntimeException("图片不是RGB图像！");
        }
        double a = 0;
        int[] hist = new int[256];
        for (int i = 0; i < gray.rows(); i++) {
            for (int j = 0; j < gray.cols(); j++) {
                a += gray.get(i, j)[0] - 128;
                int index = (int) gray.get(i, j)[0];
                hist[index]++;
            }
        }
        double da = a / (gray.rows() * gray.cols());
        double ma = 0;
        for (int i = 0; i < hist.length; i++) {
            ma += Math.abs(i - 128 - da) * hist[i];
        }
        ma = ma / (gray.rows() * gray.cols());
        double cast = Math.abs(da) / Math.abs(ma);
        return new double[] {cast, da};
    }

}
