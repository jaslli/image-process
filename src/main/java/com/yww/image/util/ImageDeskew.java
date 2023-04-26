package com.yww.image.util;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

/**
 * <p>
 *      从tess4j上扒下来的源码，可以直接使用maven导入依赖
 * <dependency>
 *   <groupId>net.sourceforge.tess4j</groupId>
 *    <artifactId>tess4j</artifactId>
 *    <version>5.6.0</version>
 *  </dependency>
 * </p>
 *
 * @author yww
 * @since 2023/4/26
 */
@SuppressWarnings("all")
public class ImageDeskew {

    /**
     * Representation of a line in the image.
     */
    public class HoughLine {
        // count of points in the line
        public int count = 0;
        // index in matrix.
        public int index = 0;
        // the line is represented as all x, y that solve y * cos(alpha) - x *
        // sin(alpha) = d
        public double alpha;
        public double d;
    }

    // the source image
    private BufferedImage cImage;
    // the range of angles to search for lines
    private double cAlphaStart = -20;
    private double cAlphaStep = 0.2;
    private int cSteps = 40 * 5;
    // pre-calculation of sin and cos
    private double[] cSinA;
    private double[] cCosA;
    // range of d
    private double cDMin;
    private double cDStep = 1.0;
    private int cDCount;
    // count of points that fit in a line
    private int[] cHMatrix;

    /**
     * Constructor.
     */
    public ImageDeskew(BufferedImage image) {
        this.cImage = image;
    }

    /**
     * Calculates the skew angle of the image cImage.
     */
    public double getSkewAngle() {
        HoughLine[] hl;
        double sum = 0.0;
        int count = 0;

        // perform Hough Transformation
        calc();
        // top 20 of the detected lines in the image
        hl = getTop(20);

        if (hl.length >= 20) {
            // average angle of the lines
            for (int i = 0; i < 19; i++) {
                sum += hl[i].alpha;
                count++;
            }
            return (sum / count);
        } else {
            return 0.0d;
        }
    }

    // calculate the count lines in the image with most points
    private HoughLine[] getTop(int count) {

        HoughLine[] hl = new HoughLine[count];
        for (int i = 0; i < count; i++) {
            hl[i] = new HoughLine();
        }

        HoughLine tmp;

        for (int i = 0; i < (this.cHMatrix.length - 1); i++) {
            if (this.cHMatrix[i] > hl[count - 1].count) {
                hl[count - 1].count = this.cHMatrix[i];
                hl[count - 1].index = i;
                int j = count - 1;
                while ((j > 0) && (hl[j].count > hl[j - 1].count)) {
                    tmp = hl[j];
                    hl[j] = hl[j - 1];
                    hl[j - 1] = tmp;
                    j--;
                }
            }
        }

        int alphaIndex;
        int dIndex;

        for (int i = 0; i < count; i++) {
            dIndex = hl[i].index / cSteps; // integer division, no
            // remainder
            alphaIndex = hl[i].index - dIndex * cSteps;
            hl[i].alpha = getAlpha(alphaIndex);
            hl[i].d = dIndex + cDMin;
        }

        return hl;
    }

    // Hough Transformation
    private void calc() {
        int hMin = (int) ((this.cImage.getHeight()) / 4.0);
        int hMax = (int) ((this.cImage.getHeight()) * 3.0 / 4.0);
        init();

        for (int y = hMin; y < hMax; y++) {
            for (int x = 1; x < (this.cImage.getWidth() - 2); x++) {
                // only lower edges are considered
                if (isBlack(this.cImage, x, y)) {
                    if (!isBlack(this.cImage, x, y + 1)) {
                        calc(x, y);
                    }
                }
            }
        }

    }

    // calculate all lines through the point (x,y)
    private void calc(int x, int y) {
        double d;
        int dIndex;
        int index;

        for (int alpha = 0; alpha < (this.cSteps - 1); alpha++) {
            d = y * this.cCosA[alpha] - x * this.cSinA[alpha];
            dIndex = (int) (d - this.cDMin);
            index = dIndex * this.cSteps + alpha;
            try {
                this.cHMatrix[index] += 1;
            } catch (Exception e) {
            }
        }
    }

    private void init() {

        double angle;

        // pre-calculation of sin and cos
        this.cSinA = new double[this.cSteps - 1];
        this.cCosA = new double[this.cSteps - 1];

        for (int i = 0; i < (this.cSteps - 1); i++) {
            angle = getAlpha(i) * Math.PI / 180.0;
            this.cSinA[i] = Math.sin(angle);
            this.cCosA[i] = Math.cos(angle);
        }

        // range of d
        this.cDMin = -this.cImage.getWidth();
        this.cDCount = (int) (2.0 * ((this.cImage.getWidth() + this.cImage.getHeight())) / this.cDStep);
        this.cHMatrix = new int[this.cDCount * this.cSteps];
    }

    public double getAlpha(int index) {
        return this.cAlphaStart + (index * this.cAlphaStep);
    }

    /**
     * Whether the pixel is black.
     *
     * @param image source image
     * @param x
     * @param y
     * @return
     */
    public static boolean isBlack(BufferedImage image, int x, int y) {
        if (image.getType() == BufferedImage.TYPE_BYTE_BINARY) {
            WritableRaster raster = image.getRaster();
            int pixelRGBValue = raster.getSample(x, y, 0);
            return pixelRGBValue == 0;
        }

        int luminanceValue = 140;
        return isBlack(image, x, y, luminanceValue);
    }

    /**
     * Whether the pixel is black.
     *
     */
    public static boolean isBlack(BufferedImage image, int x, int y, int luminanceCutOff) {
        int pixelRGBValue;
        int r;
        int g;
        int b;
        double luminance = 0.0;

        // return white on areas outside of image boundaries
        if (x < 0 || y < 0 || x > image.getWidth() || y > image.getHeight()) {
            return false;
        }

        try {
            pixelRGBValue = image.getRGB(x, y);
            r = (pixelRGBValue >> 16) & 0xff;
            g = (pixelRGBValue >> 8) & 0xff;
            b = (pixelRGBValue) & 0xff;
            luminance = (r * 0.299) + (g * 0.587) + (b * 0.114);
        } catch (Exception e) {
        }

        return luminance < luminanceCutOff;
    }

}
