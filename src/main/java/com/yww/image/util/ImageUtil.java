package com.yww.image.util;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;

/**
 * <p>
 *      图片处理工具类
 * </p>
 *
 * @author yww
 * @since 2023/4/26
 */
public class ImageUtil {

    static {
        OpencvUtil.load();
    }

    /**
     * 图片灰度化
     *
     * @param mat   图片矩阵
     */
    public static Mat gray(Mat mat) {
        Mat gray = mat.clone();
        // 判断图片的通道数
        if (mat.channels() == 4 || mat.channels() == 3) {
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);
        } else if (mat.channels() == 2) {
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR5652GRAY);
        } else {
            gray = mat;
        }
        return gray;
    }

    /**
     *  图片灰度化后进行边缘检测
     *  检测图像中的边缘，并生成一个二值图像，其中边缘被表示为白色，背景为黑色
     *
     * @param mat           图片矩阵
     */
    public static Mat canny(Mat mat) {
        return canny(mat, 60, 200, 3);
    }

    /**
     *  图片灰度化后进行边缘检测
     *  检测图像中的边缘，并生成一个二值图像，其中边缘被表示为白色，背景为黑色
     *
     * @param mat           图片矩阵
     * @param threshold1    边缘的阈值
     * @param threshold2    边缘的阈值
     * @param apertureSize  计算图像梯度的Sobel算子的大小
     */
    public static Mat canny(Mat mat, int threshold1, int threshold2, int apertureSize) {
        // 灰度化
        Mat gray = gray(mat);
        Mat cannyMat = gray.clone();
        // 进行边缘检测
        Imgproc.Canny(gray, cannyMat, threshold1, threshold2, apertureSize);
        return cannyMat;
    }

    /**
     * 使用Graphics2D进行旋转图片
     *
     * @param src       输入路径
     * @param dst       输出路径
     * @param degree    旋转角度
     */
    public static void rotateImage(String src, String dst, double degree) {
        BufferedImage image = ImgUtil.read(src);
        ImgUtil.write(rotateImage(image, degree), FileUtil.file(dst));
    }

    /**
     * 使用Graphics2D进行旋转图片
     *
     * @param image     需要旋转的图片
     * @param degree    旋转的角度
     * @return          旋转后的图片
     */
    public static BufferedImage rotateImage(BufferedImage image, double degree) {
        int width = image.getWidth();
        int height = image.getHeight();
        int type = image.getType();
        BufferedImage res = new BufferedImage(width, height, type);
        Graphics2D graphics  = res.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        // 设置图片底色
        graphics.setBackground(Color.WHITE);
        // 填充底图
        graphics.fillRect(0, 0, width, height);
        // 按中心点旋转图片
        graphics.rotate(Math.toRadians(degree), width >> 1, height >> 1);
        graphics.drawImage(image, 0, 0, null);
        // 关闭Graphics2D
        graphics.dispose();
        return res;
    }

    /**
     * 纠正图片旋转
     * 有些图片手机拍摄，在电脑上看是竖着的，其实是电脑自动进行的转换
     * 如果旋转这种图片，就会变成原来初始的效果进行旋转，和视图的旋转不一样
     * 所以需要先进行纠正图片的旋转
     *
     * @param srcImgPath    图片路径
     */
    public static void correctImg(String srcImgPath) {
        FileOutputStream fos = null;
        try {
            // 原始图片
            File srcFile = new File(srcImgPath);
            // 获取偏转角度
            int angle = getAngle(srcFile);
            if (angle != 90 && angle != 270) {
                return;
            }
            // 原始图片缓存
            BufferedImage srcImg = ImageIO.read(srcFile);
            // 宽高互换
            // 原始宽度
            int imgWidth = srcImg.getHeight();
            // 原始高度
            int imgHeight = srcImg.getWidth();
            // 中心点位置
            double centerWidth = ((double) imgWidth) / 2;
            double centerHeight = ((double) imgHeight) / 2;
            // 图片缓存
            BufferedImage targetImg = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
            // 旋转对应角度
            Graphics2D g = targetImg.createGraphics();
            g.rotate(Math.toRadians(angle), centerWidth, centerHeight);
            g.drawImage(srcImg, (imgWidth - srcImg.getWidth()) / 2, (imgHeight - srcImg.getHeight()) / 2, null);
            g.rotate(Math.toRadians(-angle), centerWidth, centerHeight);
            g.dispose();
            // 输出图片
            fos = new FileOutputStream(srcFile);
            ImageIO.write(targetImg, "jpg", fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.flush();
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取图片旋转角度
     *
     * @param file  上传图片
     * @return      图片旋转角度
     */
    private static int getAngle(File file) throws Exception {
        Metadata metadata = ImageMetadataReader.readMetadata(file);
        for (Directory directory : metadata.getDirectories()) {
            for (Tag tag : directory.getTags()) {
                if ("Orientation".equals(tag.getTagName())) {
                    String orientation = tag.getDescription();
                    if (orientation.contains("90")) {
                        return 90;
                    } else if (orientation.contains("180")) {
                        return 180;
                    } else if (orientation.contains("270")) {
                        return 270;
                    }
                }
            }
        }
        return 0;
    }

}
