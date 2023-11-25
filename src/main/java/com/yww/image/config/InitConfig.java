package com.yww.image.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @author yww
 * @since 2023/11/26
 */
@Configuration
public class InitConfig {

    @Value("${opencv.url}")
    String opencvUrl;

    @PostConstruct
    public void asposeRegister() {
        System.load(opencvUrl);
    }

}
