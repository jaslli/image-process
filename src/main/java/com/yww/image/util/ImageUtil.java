package com.yww.image.util;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
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

    /**
     * 使用Graphics2D进行旋转图片
     *
     * @param src       输入路径
     * @param dst       输出路径
     * @param degree    旋转角度
     */
    public static void rotateImage(String src, String dst, double degree) {
        // 读取图片
        BufferedImage image = ImgUtil.read(src);

        // 获取图片宽，高，类型
        int width = image.getWidth();
        int height = image.getHeight();
        int type = image.getType();
        // 创建Graphics2D
        BufferedImage res = new BufferedImage(width, height, type);
        Graphics2D graphics  = res.createGraphics();
        // 设置图形渲染选项，指定双线性插值算法作为图像缩放时的默认算法
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

        // 写出图片
        ImgUtil.write(res, FileUtil.file(dst));
    }

    /**
     * 图片灰度化
     *
     * @param mat       图像矩阵
     * @return          图像矩阵
     */
    public static Mat gray(Mat mat) {
        Mat gray = new Mat();
        // 获取图片的通道数，根据不同通道进行处理
        int channel = mat.channels();

        if (channel == 1) {
            // 单通道图片无需进行灰度化操作
            gray = mat.clone();
        } else if (channel == 2) {
            // 双通道图片，将两个通道的值相加再除以2，得到灰度值
            Mat temp = new Mat();
            Core.addWeighted(mat, 0.5, mat, 0.5, 0, temp);
            Imgproc.cvtColor(temp, gray, Imgproc.COLOR_BGR2GRAY);
        } else if (channel == 4 || channel == 3) {
            // 三通道和四通道的图片，使用标准的灰度化方式，考虑Alpha分量
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);
        } else {
            // 其他通道，暂不支持灰度化操作
            throw new UnsupportedOperationException("不支持灰度化的通道数： -->" + channel);
        }
        return gray;
    }

    /**
     * 高斯滤波平滑
     * 第三个参数为高斯核大小
     * 第四个和第五个参数为标准差，设置为0，则根据核大小自动计算
     *
     * @param mat       图像矩阵
     * @return          图像矩阵
     */
    public static Mat gaussianBlur(Mat mat) {
        Mat blurred = mat.clone();
        Imgproc.GaussianBlur(mat, blurred, new Size(3, 3), 0, 0);
        return blurred;
    }

    /**
     * 边缘检测
     *
     * @param mat   图像矩阵
     * @return      图像矩阵
     */
    public static Mat canny(Mat mat) {
        return canny(mat, 60, 200, 3);
    }

    /**
     *  边缘检测
     *  检测图像中的边缘，并生成一个二值图像，其中边缘被表示为白色，背景为黑色
     *  一般来说threshold2大于threshold1，保证能够检测到真正的边缘
     *  threshold1一般设置为图像灰度级的20%-30%
     *  threshold2一般设置为threshold1的三倍
     *
     * @param mat           图片矩阵
     * @param threshold1    边缘的阈值，用于检测强边缘
     * @param threshold2    边缘的阈值，用于检测弱边缘
     * @param apertureSize  Sobel算子的大小，一般为3，5或7
     * @return          图像矩阵
     */
    public static Mat canny(Mat mat, int threshold1, int threshold2, int apertureSize) {
        // 进行高斯平滑
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(mat, blurred, new Size(3, 3), 0, 0);

        // 灰度化
        Mat gray = new Mat();
        Imgproc.cvtColor(blurred, gray, Imgproc.COLOR_BGR2GRAY);

        // 进行边缘检测
        Mat canny = new Mat();
        Imgproc.Canny(gray, canny, threshold1, threshold2, apertureSize);

        return canny;
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
            Graphics2D graphics = targetImg.createGraphics();
            graphics.rotate(Math.toRadians(angle), centerWidth, centerHeight);
            graphics.drawImage(srcImg, (imgWidth - srcImg.getWidth()) / 2, (imgHeight - srcImg.getHeight()) / 2, null);
            graphics.rotate(Math.toRadians(-angle), centerWidth, centerHeight);
            graphics.dispose();
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
