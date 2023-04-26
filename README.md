---
title: Java图片处理
date: 2023-4-18
categories:
	- Java
description: 最近经常接触Java的图片处理，所以特地来记录一下。
cover: https://img.yww52.com/2023/4/2023-4-18top_img.jpg
---

# 开发环境

本次处理图片主要使用的依赖是`opencv`，具体的方法可以去参照官网。

1. 首先去[opencv官网](https://opencv.org/releases/)去下载与系统对应的jar包和依赖文件。（这里以windows举例）

2. 选择windows版本`opencv-4.7.0-windows.exe`，然后进行安装。

3. 在安装目录的`opencv/build/java`的目录下，获得`jar`包和动态库文件。

4. 在maven项目引入`opencv`依赖。

   ```xml
           <!-- OpenCv  -->
           <dependency>
               <groupId>org.opencv</groupId>
               <artifactId>opencv</artifactId>
               <version>4.7.0</version>
               <scope>system</scope>
               <systemPath>${basedir}/src/main/resources/lib/opencv-470.jar</systemPath>
           </dependency>
   ```

5. 加载`opencv`动态库

   由于opencv的开发语言问题，所以Java在使用opencv相关类的时候，需要先加载对应的动态库才行，不然直接使用会出现异常。需要注意linux和windows的动态库是不一样的。

   ```Java
       /**
        * 加载opencv动态库（打包时建议使用绝对路径）
        * win: opencv_java470.dll
        * linux: opencv_java470.so
        */
       public static void load() {
           System.load(ResourceUtil.getResource("lib/opencv_java470.dll").getPath());
       }
   ```

> 好像最新版本的opencv（4.7.0）不支持Java8，只能有Java11，如果用Java8的话，可以使用4.6.0的版本。

# opencv读入写出图片

## 读入图片

```Java
public class Test {

    public static void main(String[] args) {
        OpencvUtil.load();
        String filePath = "C:/Users/11419/Desktop/project/image-process/src/main/resources/test/1.jpg";
        // Mat是opencv表示一张图片的基础类
        Mat mat = Imgcodecs.imread(filePath);

        // Mat其实就是一个像素矩阵
        System.out.println(mat.rows());
        System.out.println(mat.cols());
        for (int i = 0, rows = mat.rows(); i < rows; i++) {
            for (int j = 0, cols = mat.cols(); j < cols; j++) {
                double[] data = mat.get(i, j);
                System.out.println(Arrays.toString(data));
            }
        }
    }

}
```

## 写出图片

```Java
public class Test {

    public static void main(String[] args) {
        OpencvUtil.load();
        String filePath = "C:/Users/11419/Desktop/project/image-process/src/main/resources/test/1.jpg";
        String output = "C:/Users/11419/Desktop/project/image-process/src/main/resources/test/2.jpg";

        Mat mat = Imgcodecs.imread(filePath);
        // 写出图片
        Imgcodecs.imwrite(output, mat);
    }

}
```

