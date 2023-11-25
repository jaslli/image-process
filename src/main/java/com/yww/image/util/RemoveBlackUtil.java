package com.yww.image.util;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * <p>
 *      去黑边（切边矫正）
 *      原理是检测图片种最大的矩形，然后通过变换直接去除边缘
 * </p>
 *
 * @author yww
 * @since 2023/3/8 9:31
 */
public class RemoveBlackUtil {

    public static void main(String[] args) {
        String testImg = "C:\\Users\\11419\\Desktop\\test\\a.jpg";
        String resImg = "C:\\Users\\11419\\Desktop\\test\\8.jpg";
        System.load("D:\\project\\imgproc\\lib\\opencv_java460.dll");
        remove(testImg, resImg);
    }

    public static void remove(String src,String dst) {
        Mat img = Imgcodecs.imread(src);
        if(img.empty()){
            return;
        }
        Mat greyImg = img.clone();
        //1.彩色转灰色
        Imgproc.cvtColor(img, greyImg, Imgproc.COLOR_BGR2GRAY);
        OpencvUtil.saveImage(greyImg, "C:\\Users\\11419\\Desktop\\test\\1.jpg");

        Mat gaussianBlurImg = greyImg.clone();
        // 2.高斯滤波，降噪
        Imgproc.GaussianBlur(greyImg, gaussianBlurImg, new Size(3,3),0);
        OpencvUtil.saveImage(greyImg, "C:\\Users\\11419\\Desktop\\test\\2.jpg");

        // 3.Canny边缘检测
        Mat cannyImg = gaussianBlurImg.clone();
        Imgproc.Canny(gaussianBlurImg, cannyImg, 50, 200);
        OpencvUtil.saveImage(cannyImg, "C:\\Users\\11419\\Desktop\\test\\3.jpg");

        // 4.膨胀，连接边缘
        Mat dilateImg = cannyImg.clone();
        Imgproc.dilate(cannyImg, dilateImg, new Mat(), new Point(-1, -1), 3, 1, new Scalar(1));
        OpencvUtil.saveImage(dilateImg, "C:\\Users\\11419\\Desktop\\test\\4.jpg");

        //5.对边缘检测的结果图再进行轮廓提取
        List<MatOfPoint> contours = new ArrayList<>();
        List<MatOfPoint> drawContours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(dilateImg, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Mat linePic = Mat.zeros(dilateImg.rows(), dilateImg.cols(), CvType.CV_8UC3);
        //6.找出轮廓对应凸包的四边形拟合
        List<MatOfPoint> squares = new ArrayList<>();
        List<MatOfPoint> hulls = new ArrayList<>();
        MatOfInt hull = new MatOfInt();
        MatOfPoint2f approx = new MatOfPoint2f();
        approx.convertTo(approx, CvType.CV_32F);

        for (MatOfPoint contour : contours) {
            // 边框的凸包
            Imgproc.convexHull(contour, hull);
            // 用凸包计算出新的轮廓点
            Point[] contourPoints = contour.toArray();
            int[] indices = hull.toArray();
            List<Point> newPoints = new ArrayList<>();
            for (int index : indices) {
                newPoints.add(contourPoints[index]);
            }
            MatOfPoint2f contourHull = new MatOfPoint2f();
            contourHull.fromList(newPoints);
            // 多边形拟合凸包边框(此时的拟合的精度较低)
            Imgproc.approxPolyDP(contourHull, approx, Imgproc.arcLength(contourHull, true) * 0.02, true);
            MatOfPoint mat = new MatOfPoint();
            mat.fromArray(approx.toArray());
            drawContours.add(mat);
            // 筛选出面积大于某一阈值的，且四边形的各个角度都接近直角的凸四边形
            MatOfPoint approxf = new MatOfPoint();
            approx.convertTo(approxf, CvType.CV_32S);
            if (approx.rows() == 4 && Math.abs(Imgproc.contourArea(approx)) > 40000 &&
                    Imgproc.isContourConvex(approxf)) {
                double maxCosine = 0;
                for (int j = 2; j < 5; j++) {
                    double cosine = Math.abs(getAngle(approxf.toArray()[j % 4], approxf.toArray()[j - 2], approxf.toArray()[j - 1]));
                    maxCosine = Math.max(maxCosine, cosine);
                }
                // 角度大概72度
                if (maxCosine < 0.3) {
                    MatOfPoint tmp = new MatOfPoint();
                    contourHull.convertTo(tmp, CvType.CV_32S);
                    squares.add(approxf);
                    hulls.add(tmp);
                }
            }
        }
        //这里是把提取出来的轮廓通过不同颜色的线描述出来，具体效果可以自己去看
        Random r = new Random();
        for (int i = 0; i < drawContours.size(); i++) {
            Imgproc.drawContours(linePic, drawContours, i, new Scalar(r.nextInt(255),r.nextInt(255), r.nextInt(255)));
        }
        OpencvUtil.saveImage(linePic, "C:\\Users\\11419\\Desktop\\test\\5.jpg");
        //7.找出最大的矩形
        int index = findLargestSquare(squares);
        MatOfPoint largest_square;
        if(!squares.isEmpty()){
            largest_square = squares.get(index);
        }else{
            System.out.println("图片无法识别");
            return;
        }
        Mat polyPic = Mat.zeros(img.size(), CvType.CV_8UC3);
        Imgproc.drawContours(polyPic, squares, index, new Scalar(0, 0,255), 2);
        OpencvUtil.saveImage(polyPic, "C:\\Users\\11419\\Desktop\\test\\6.jpg");
        //存储矩形的四个凸点
        hull = new MatOfInt();
        Imgproc.convexHull(largest_square, hull, false);
        List<Integer> hullList =  hull.toList();
        List<Point> polyContoursList = largest_square.toList();
        List<Point> hullPointList = new ArrayList<>();
        for (Integer integer : hullList) {
            Imgproc.circle(polyPic, polyContoursList.get(integer), 10, new Scalar(r.nextInt(255), r.nextInt(255), r.nextInt(255), 3));
            hullPointList.add(polyContoursList.get(integer));
        }
        Core.addWeighted(polyPic, 1, img, 1, 0, img);
        OpencvUtil.saveImage(img, "C:\\Users\\11419\\Desktop\\test\\7.jpg");
        List<Point> lastHullPointList = new ArrayList<>(hullPointList);
        //dstPoints储存的是变换后各点的坐标，依次为左上，右上，右下， 左下
        //srcPoints储存的是上面得到的四个角的坐标
        Point[] dstPoints = {new Point(0,0), new Point(img.cols(),0), new Point(img.cols(),img.rows()), new Point(0,img.rows())};
        Point[] srcPoints = new Point[4];
        boolean sorted = false;
        int n = 4;
        //对四个点进行排序 分出左上 右上 右下 左下
        while (!sorted && n > 0){
            for (int i = 1; i < n; i++){
                sorted = true;
                if (lastHullPointList.get(i - 1).x > lastHullPointList.get(i).x){
                    Point temp1 = lastHullPointList.get(i);
                    Point temp2 = lastHullPointList.get(i-1);
                    lastHullPointList.set(i, temp2);
                    lastHullPointList.set(i - 1, temp1);
                    sorted = false;
                }
            }
            n--;
        }
        //即先对四个点的x坐标进行冒泡排序分出左右，再根据两对坐标的y值比较分出上下
        if (lastHullPointList.get(0).y < lastHullPointList.get(1).y){
            srcPoints[0] = lastHullPointList.get(0);
            srcPoints[3] = lastHullPointList.get(1);
        }else{
            srcPoints[0] = lastHullPointList.get(1);
            srcPoints[3] = lastHullPointList.get(0);
        }
        if (lastHullPointList.get(2).y < lastHullPointList.get(3).y){
            srcPoints[1] = lastHullPointList.get(2);
            srcPoints[2] = lastHullPointList.get(3);
        }else{
            srcPoints[1] = lastHullPointList.get(3);
            srcPoints[2] = lastHullPointList.get(2);
        }
        List<Point> listSrcs = java.util.Arrays.asList(srcPoints[0], srcPoints[1], srcPoints[2], srcPoints[3]);
        Mat srcPointsMat = Converters.vector_Point_to_Mat(listSrcs, CvType.CV_32F);

        List<Point> dstSrcs = java.util.Arrays.asList(dstPoints[0], dstPoints[1], dstPoints[2], dstPoints[3]);
        Mat dstPointsMat = Converters.vector_Point_to_Mat(dstSrcs, CvType.CV_32F);
        //参数分别为输入输出图像、变换矩阵、大小。
        //坐标变换后就得到了我们要的最终图像。
        Mat transMat = Imgproc.getPerspectiveTransform(srcPointsMat, dstPointsMat);    //得到变换矩阵
        Mat outPic = new Mat();
        Imgproc.warpPerspective(img, outPic, transMat, img.size());
        OpencvUtil.saveImage(outPic, dst);
    }

    // 根据三个点计算中间那个点的夹角   pt1 pt0 pt2
    private static double getAngle(Point pt1, Point pt2, Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;
        return (dx1*dx2 + dy1*dy2)/Math.sqrt((dx1*dx1 + dy1*dy1)*(dx2*dx2 + dy2*dy2) + 1e-10);
    }

    // 找到最大的正方形轮廓
    private static int findLargestSquare(List<MatOfPoint> squares) {
        if (squares.isEmpty()) {
            return -1;
        }
        int maxWidth = 0;
        int maxHeight = 0;
        int maxSquareIdx = 0;
        int currentIndex = 0;
        for (MatOfPoint square : squares) {
            Rect rectangle = Imgproc.boundingRect(square);
            if (rectangle.width >= maxWidth && rectangle.height >= maxWidth) {
                maxWidth = rectangle.width;
                maxHeight = rectangle.height;
                maxSquareIdx = currentIndex;
            }
            currentIndex++;
        }
        return maxSquareIdx;
    }

}
