package com.yww.image.util;


import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;


/**
 * <p>
 *      测试类
 * </P>
 *
 * @author yww
 * @since 2023/4/21
 */
public class Test {

    public static void main(String[] args) {
        String filePath = "C:/Users/11419/Desktop/project/image-process/src/main/resources/test/1.jpg";
        String output = "C:/Users/11419/Desktop/project/image-process/src/main/resources/test/2.jpg";

        Mat mat = Imgcodecs.imread(filePath);
        // 写出图片
        Imgcodecs.imwrite(output, mat);
    }

}
