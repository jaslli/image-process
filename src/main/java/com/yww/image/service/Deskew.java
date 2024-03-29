package com.yww.image.service;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import com.yww.image.util.ImageDeskew;
import com.yww.image.util.ImageUtil;
import com.yww.image.util.OpencvUtil;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *      图片纠偏
 * 不同类型的倾斜角需要根据获取不同的直线去计算
 * 这里主要是计算文档类型的倾斜角
 * 网上找了很多方法，还是利用直线计算倾斜度这种方法比较有效
 * </p>
 *
 * @author yww
 * @since 2023/4/26
 */
public class Deskew {

    /**
     * 图片进行纠偏
     *
     * @param src   图片路径
     * @param dst   纠偏图片保存路径
     */
    public static void deskew(String src, String dst) {
        Mat mat = Imgcodecs.imread(src);
        // 计算图片倾斜角
        double angle = getDeskewAngle(mat);
        // 图片旋转
        ImageUtil.rotateImage(src, dst, angle);
    }

    /**
     *  通过霍夫变换后，获取直线并计算出整体的倾斜角度
     *
     * @param src       图片
     * @return          倾斜角度
     */
    public static Integer getDeskewAngle(Mat src) {
        // 图片灰度化
        Mat gray = src.clone();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

//        // 高斯模糊（？）
//        @Cleanup
//        UMat blur = gray.clone();
//        GaussianBlur(gray, blur, new Size(9, 9), 0);
//        // 二值化（？）
//        @Cleanup
//        UMat thresh = blur.clone();
//        threshold(blur, thresh, 0, 255, THRESH_BINARY_INV + THRESH_OTSU);

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));

        // 图片膨胀
        Mat erode = gray.clone();
        Imgproc.erode(gray, erode, kernel);

        // 图片腐蚀
        Mat dilate = erode.clone();
        Imgproc.dilate(erode, dilate, kernel);

        // 边缘检测
        Mat canny = dilate.clone();
        Imgproc.Canny(dilate, canny, 50, 150);

        // 霍夫变换得到线条
        Mat lines = new Mat();
        //累加器阈值参数，小于设置值不返回
        int threshold = 90;
        //最低线段长度，低于设置值则不返回
        double minLineLength = 100;
        //间距小于该值的线当成同一条线
        double maxLineGap = 10;
        // 霍夫变换，通过步长为1，角度为PI/180来搜索可能的直线
        Imgproc.HoughLinesP(canny, lines, 1, Math.PI / 180, threshold, minLineLength, maxLineGap);
        // 计算倾斜角度
        List<Integer> angelList = new ArrayList<>();
        for (int i = 0; i < lines.rows(); i++) {
            double[] line = lines.get(i, 0);
            int k = calculateAngle(line[0], line[1], line[2], line[3]);
            angelList.add(k);
        }
        if (angelList.isEmpty()) {
            return 0;
        }
        gray.release();
        kernel.release();
        erode.release();
        dilate.release();
        canny.release();
        lines.release();
        // 可能还得需要考虑方差来决定选择平均数还是众数
        return most(angelList);
    }

    /**
     * 使用tess4j的图片倾斜角计算
     * 另一种思路
     *
     * @param filePath  图片路径
     * @return          图片倾斜角
     */
    public static double getAngle2(String filePath) {
        BufferedImage src;
        try {
            src = ImageIO.read(FileUtil.file(filePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ImageDeskew imageDeskew = new ImageDeskew(src);
        return imageDeskew.getSkewAngle();
    }

    /**
     * 求数组平均数，四舍五入保留一位
     *
     * @param angelList 数组
     * @return          数组平均数
     */
    private static double avg(List<Integer> angelList) {
        int sum = 0;
        for (int i : angelList) {
            sum += i;
        }
        if (sum == 0) {
            return 0.0;
        }
        BigDecimal bigDecimal = new BigDecimal(sum).divide(new BigDecimal(angelList.size()), RoundingMode.HALF_UP);
        return bigDecimal.setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 求数组众数
     *
     * @param angelList 数组
     * @return          数组众数
     */
    private static int most(List<Integer> angelList) {
        if (angelList.isEmpty()) {
            return 0;
        }
        int res = 0;
        int max = Integer.MIN_VALUE;
        Map<Integer, Integer> map = new HashMap<>();
        for (int i : angelList) {
            map.put(i, map.getOrDefault(i, 0) + 1);
        }
        for (Integer i : map.keySet()) {
            int count = map.get(i);
            if (count > max) {
                max = count;
                res = i;
            }
        }
        return res;
    }

    /**
     * 计算直线的倾斜角
     *
     * @param x1    点1的横坐标
     * @param x2    点2的横坐标
     * @param y1    点1的纵坐标
     * @param y2    点2的纵坐标
     * @return 直角的倾斜度
     */
    private static int calculateAngle(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        if (Math.abs(dx) < 1e-4) {
            return 90;
        } else if (Math.abs(dy) < 1e-4) {
            return 0;
        } else {
            double radians = Math.atan2(dy, dx);
            double degrees = Math.toDegrees(radians);
            return Convert.toInt(Math.round(degrees));
        }
    }

}
