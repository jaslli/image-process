# 开发环境

本次处理图片主要使用的依赖是`opencv`，具体的方法可以去参照官网。

1. 首先去[opencv官网](https://opencv.org/releases/)去下载与系统对应的jar包和依赖文件。

2. 在maven项目引入`opencv`依赖。

   ```xml
          <!-- OpenCv  -->
          <dependency>
              <groupId>org.opencv</groupId>
              <artifactId>opencv</artifactId>
              <version>4.6.0</version>
              <scope>system</scope>
              <systemPath>${basedir}/src/main/resources/lib/opencv-460.jar</systemPath>
          </dependency>
   ```

3. 加载`opencv`动态库

   由于opencv的开发语言问题，所以Java在使用opencv相关类的时候，需要先加载对应的动态库才行，不然直接使用会出现异常。动态库在官网下载的资源目录中可以找到。需要注意linux和windows的动态库是不一样的。

   ```java
       // linux
       System.load("./libopencv_java460.so");
       // windows
       System.load(ResourceUtil.getResource("lib/opencv_java460.dll").getPath());
   ```

   
