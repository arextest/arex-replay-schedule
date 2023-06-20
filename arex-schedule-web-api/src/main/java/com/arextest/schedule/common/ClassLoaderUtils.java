package com.arextest.schedule.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.loader.WebappClassLoaderBase;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;

@Slf4j
public class ClassLoaderUtils {
    private static final String ADD_URL_FUN_NAME = "addURL";

    public static void loadJar(String jarPath) {
        File jarFile = new File(jarPath);
        Method method = null;
        //for tomcat
        try {
            method = WebappClassLoaderBase.class.getDeclaredMethod(ADD_URL_FUN_NAME, URL.class);
        } catch (NoSuchMethodException | SecurityException e1) {
            LOGGER.error("getDeclaredMethod failed, jarPath:{}, message:{}", jarPath, e1.getMessage());
        }
        boolean accessible = method.isAccessible();
        try {
            method.setAccessible(true);
            Class<?> clazz = ClassLoaderUtils.class;
            ClassLoader classLoader = clazz.getClassLoader();
            URL url = jarFile.toURI().toURL();
            method.invoke(classLoader, url);
        } catch (Exception e2) {
            LOGGER.error("addUrl failed, jarPath:{}, message:{}", jarPath, e2.getMessage());
        } finally {
            method.setAccessible(accessible);
        }
        //for spring

        //        try {
        //            method = URLClassLoader.class.getDeclaredMethod(ADD_URL_FUN_NAME, URL.class);
        //        } catch (NoSuchMethodException | SecurityException e1) {
        //            LOGGER.error("getDeclaredMethod failed, jarPath:{}, message:{}", jarPath, e1.getMessage());
        //        }
        //        boolean accessible = method.isAccessible();
        //        try {
        //            method.setAccessible(true);
        //            URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        //            URL url = jarFile.toURI().toURL();
        //            method.invoke(classLoader, url);
        //        } catch (Exception e2) {
        //            LOGGER.error("addUrl failed, jarPath:{}, message:{}", jarPath, e2.getMessage());
        //        } finally {
        //            method.setAccessible(accessible);
        //        }
    }
}
