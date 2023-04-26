package com.yww.image.util;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 *      Opencv工具类
 * </P>
 *
 * @author yww
 * @since 2023/4/21
 */
public class OpencvUtil {

    /**
     * 加载opencv动态库
     * win: opencv_java470.dll
     * linux: opencv_java470.so
     */
    public static void load() {
        System.load(ResourceUtil.getResource("lib/opencv_java470.dll").getPath());
    }

    /**
     * 保存图片到指定位置
     *
     * @param mat       图片矩阵
     * @param filePath  文件路径
     * @param ext       保存文件后缀
     */
    public static void saveImage(Mat mat, String filePath, String ext) {
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode("." + ext, mat, matOfByte);
        byte[] byteArray = matOfByte.toArray();
        FileUtil.writeBytes(byteArray, filePath);
    }

    /**
     * 保存图片到指定位置
     *
     * @param mat       图片矩阵
     * @param filePath  文件路径
     */
    public static void saveImage(Mat mat, String filePath) {
        saveImage(mat, filePath, "png");
    }

    /**
     *  根据文件转换为mat对象
     *  不建议使用该方法，很容易出现空指针，不支持图片类型等一些奇怪报错
     *
     * @param file  文件
     * @return      图片矩阵
     */
    public static Mat getMat(MultipartFile file) throws IOException, ImageReadException {
        ImageInfo imageInfo = Imaging.getImageInfo(file.getInputStream(), file.getOriginalFilename());
        InputStream inputStream = file.getInputStream();
        /*
            hutool的ImgUtil.read()等效与ImageIO.read()
            ImageIO.read()容易出现空指针读不出图片
            hutool的ImgUtil.read()只支持hutool提供的图片类型
         */
        BufferedImage image = ImgUtil.read(inputStream);
        image = convert(image);

        DataBuffer data = image.getRaster().getDataBuffer();
        byte[] bytes = ((DataBufferByte) data).getData();
        Mat mat = new Mat(imageInfo.getHeight(), imageInfo.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, bytes);
        return mat;
    }

    /**
     *  将图片转换为BufferedImage.TYPE_3BYTE_BGR
     *
     * @param image 图片流
     * @return      图片流
     */
    private static BufferedImage convert(BufferedImage image) {
        BufferedImage convertedImage = new BufferedImage(image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_3BYTE_BGR);
        convertedImage.getGraphics().drawImage(image, 0, 0, null);
        return convertedImage;
    }

}
