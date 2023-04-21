package com.yww.image.util;

import cn.hutool.core.io.resource.ResourceUtil;

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

}
