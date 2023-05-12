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

2. 选择windows版本`opencv-4.7.0-windows.exe`，然后进行安装。(linux的版本需要先进行编译后获取动态库和jar包)

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
>
> 如果觉得opencv配置环境过于麻烦，可以使用Javacv，Javacv其实就是调用opencv进行操作的，虽然官方缺少文档，不过和opencv的调用方法都差不多。

# opencv基础操作

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

> `Imgcodecs.imread()`这个方法，偶然碰见过一次保存失败的情况，成功运行却没有保存文件，所以还可以通过字节方式保存。

```java 
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
```



# 图片处理方法

## 图片信息

### 获取图片DPI

暂时有两种方法可以获取图片的DPI。

1. commons-imaging依赖

   ```Java
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
   
   ```

2. metadata-extractor依赖

   ```Java
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
   ```

### 获取图片分辨率

```Java
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
```



> 这些操作实质是获取图片的基本信息得到的，所以图片大部分的基本信息都可以通过这些方法修改得到。



## 图片旋转

这里的图片旋转使用了`Graphics2D`进行操作。

```Java
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
```



## 纠正图片旋转

有些时候，电脑会自动对一些图片进行旋转显示（源文件不变），比如手机拍的一些图片，竖着拍的，电脑显示是竖着的，但是源文件其实是横着的，进行图片旋转，其实是按横着的源文件图片进行旋转，所以有时候会感觉旋转角度不对，所以需要先纠正图片旋转。

```Java
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

```



## 灰度化

大多数彩色图片的每个像素都是由红绿蓝三个通道组成的，为了更方便的处理图像，将这三个通道的值按一定比例进行加权平均，得到一个单通道的灰度值，用来表示该像素的颜色亮度。大多数图片处理都会先进行图片灰度化，因为只有一个通道，处理起来方便很多。opencv的灰度化操作，只需要调用方法即可。灰度化操作的话，主要是要考虑图片通道数量的问题。

```Java
    /**
     * 图片灰度化
     *
     * @param mat   图像矩阵
     * @return      图像矩阵
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
```



## 高斯滤波

处理图像时，往往需要对图像进行平滑操作以去除噪声，同时也可以模糊图像以达到柔化的效果。高斯滤波（Gaussian blur）就是一种常用的平滑滤波器，其通过对图像像素进行加权平均的方式来实现平滑和模糊的效果。高斯滤波的原理是使用一个高斯核（Gaussian kernel）对图像像素进行加权平均。一般来说，高斯核的大小或标准差越大，平滑效果越明显，图像的细节丢失也会越多。

```Java
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
```



## 边缘检测

边缘是指图像中颜色或亮度发生急剧变化的区域，通常是物体之间的边界或物体内部的纹理。边缘检测算法旨在在图像中找到这些边缘并将其标记出来。具体的步骤如下：

1. 高斯滤波平滑，去除图像噪声，提高边缘检测的准确性
2. 图像灰度化
3. 进行边缘检测

```Java
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
        Mat cannyMat = new Mat();
        Imgproc.Canny(gray, cannyMat, threshold1, threshold2, apertureSize);

        return cannyMat;
    }
```



# 一些图片的实际应用

## 图片亮度处理

网上关于图片亮度检测的方法，大概有两种。

1. 计算图片平均亮度。

   ```Java
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
   ```

2. 计算图片亮度异常值

   ```Java
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
   ```

亮度调整主要是调整平均亮度值到一个自定义的值当中。

```Java
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
```

## 图片纠偏

图片纠偏是一个比较难的问题，主要的难点在于如何计算出图片的倾斜角度，最常见的方法就是霍夫变换检测直线了，以下是具体步骤。

1. 图片边缘检测
1. 霍夫变换，获取检测的直线
1. 计算每条直线的倾斜角度
1. 获取所有直线倾斜角度的平均值或者是众数来作为图片的倾斜角度。（关于平均值和众数，总会在某些情况下十分异常，我认为平均数比较可靠，但需要严格筛选需要内容的直线才行）

```Java
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
     * @param mat       图片
     * @return          倾斜角度
     */
    public static double getDeskewAngle(Mat mat) {
        // 图片边缘检测
        Mat canny = ImageUtil.canny(mat, 60, 200, 3);

        Mat lines = new Mat();
        // 累加器阈值参数，小于设置值不返回
        int threshold = 30;
        // 最低线段长度，低于设置值则不返回
        double minLineLength = 0;
        // 间距小于该值的线当成同一条线
        double maxLineGap = 200;
        // 霍夫变换，通过步长为1，角度为PI/180来搜索可能的直线
        Imgproc.HoughLinesP(canny, lines, 1, Math.PI / 180, threshold, minLineLength, maxLineGap);
        // 可以输出查看是否是需要的图片，进而调整参数

        // 计算每条直线的倾斜角
        List<Integer> angelList = new ArrayList<>();
        for (int i = 0; i < lines.rows(); i++) {
            double[] line = lines.get(i, 0);
            int k = calculateAngle(line[0], line[1], line[2], line[3]);
            //  表格类图片要过滤掉竖线，不然取得角度会有问题，看情况筛选
//            if (Math.abs(k) > 45) {
//                k = 90 - k;
//            }
            angelList.add(k);
        }
        if (angelList.isEmpty()) {
            return 0.0;
        }
        // 整体的角度是选择众数，如果希望是平均角度可以选择平均数
        return most(angelList);
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
        if (Math.abs(x2 - x1) < 1e-4) {
            return 90;
        } else if (Math.abs(y2 - y1) < 1e-4) {
            return 0;
        } else {
            double k = -(y2 - y1) / (x2 - x1);
            double res = 360 * Math.atan(k) / (2 * Math.PI);
            return Convert.toInt(Math.round(res));
        }
    }
```

