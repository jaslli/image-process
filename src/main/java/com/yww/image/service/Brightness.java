package com.yww.image.service;

import com.yww.image.util.ImageUtil;
import com.yww.image.util.OpencvUtil;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;

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
        Mat src = Imgcodecs.imread(filePath);
        // 灰度化，转为灰度图
        Mat gray = ImageUtil.gray(src.clone());

        // 计算图像的平均亮度
        Scalar mean = Core.mean(gray);
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
    public double[] brightness2(String filePath) {
        Mat src = Imgcodecs.imread(filePath);
        // 灰度化，转为灰度图
        Mat gray = ImageUtil.gray(src.clone());

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

    /**
     * 图片亮度调整
     *
     * @param src   输入路径
     * @param dst   输出路径
     */
    public void adjustBrightness(String src, String dst) {
        // 读取图片
        Mat mat = Imgcodecs.imread(src);
        // 灰度化，转为灰度图
        Mat gray = ImageUtil.gray(mat.clone());
        // 获取图片平均亮度值
        Scalar mean = Core.mean(gray);
        double brightness = mean.val[0];

        // 图片亮度判断，需要根据具体情况进行判断
        Mat adjustedImage = mat.clone();
        if (brightness < 100 || brightness > 250) {
            // 计算亮度调整值，可以根据具体情况选择参数调整
            double alpha = 175 / brightness;
            // 执行亮度调整
            gray.convertTo(adjustedImage, -1, alpha, 0);
        }
        // 保存调整后的图像
        Imgcodecs.imwrite(dst, adjustedImage);
    }

}
