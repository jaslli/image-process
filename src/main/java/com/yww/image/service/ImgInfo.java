package com.yww.image.service;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.yww.image.util.OpencvUtil;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.IOException;

/**
 * <p>
 *      图片信息
 * </p>
 *
 * @author yww
 * @since 2023/4/26
 */
public class ImgInfo {

    static {
        OpencvUtil.load();
    }

    /**
     * 获取图片的分辨率
     *
     * @param filePath  图片文件位置
     * @return          [width, height] 水平分辨率 x 垂直分辨率
     */
    public static int[] getResolution(String filePath) {
        Mat mat = Imgcodecs.imread(filePath);
        int widthResolution = mat.width();
        int heightResolution = mat.height();
        return new int[]{widthResolution, heightResolution};
    }

    /**
     * 获取图片的DPI，获取不到返回-1
     * 依赖于commons-imaging
     * <dependency>
     *   <groupId>org.apache.commons</groupId>
     *   <artifactId>commons-imaging</artifactId>
     *   <version>${commons-imaging.version}</version>
     * </dependency>
     *
     * @param filePath  图片文件位置
     * @return          图片DPI
     */
    public static int getDpi1(String filePath) {
        ImageInfo imageInfo = null;
        try {
            imageInfo = Imaging.getImageInfo(FileUtil.file(filePath));
        } catch (ImageReadException | IOException e) {
            throw new RuntimeException("获取图片信息出错！");
        }
        // 水平分辨率和垂直都一样
        if (null == imageInfo) {
            return -1;
        } else {
            return imageInfo.getPhysicalWidthDpi();
        }
    }

    /**
     * 获取图片的DPI，获取不到返回-1
     * 依赖于metadata-extractor
     * <dependency>
     *   <groupId>com.drewnoakes</groupId>
     *   <artifactId>metadata-extractor</artifactId>
     *   <version>${metadata.version}</version>
     * </dependency>
     *
     * @param filePath  图片文件位置
     * @return          图片DPI
     */
    public static int getDpi2(String filePath) {
        Metadata metadata = null;
        int res = -1;
        try {
            metadata = ImageMetadataReader.readMetadata(FileUtil.file(filePath));
            for (Directory directory : metadata.getDirectories()) {
                // 遍历图片信息，寻找水平分辨率
                for (Tag tag : directory.getTags()) {
                    if ("X Resolution".equals(tag.getTagName())) {
                        res = Convert.toInt(tag.getDescription());
                    }
                }
            }
        } catch (ImageProcessingException | IOException e) {
            throw new RuntimeException("获取图片信息出错！");
        }
        return res;
    }


}
