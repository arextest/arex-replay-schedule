package com.arextest.schedule.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.loader.WebappClassLoaderBase;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class FileUtils {

    private static final String ADD_URL_FUN_NAME = "addURL";
    public static void copy(String oldPath, String newPath) {
        File oldFile = new File(oldPath);
        File newFile = new File(newPath);

        if (!oldFile.exists()) {
            LOGGER.error("oldFile doesn't exist. oldFile:{}", oldPath);
            return;
        }
        newFile.delete();
        try {
            Files.copy(oldFile.toPath(), newFile.toPath());
        } catch (IOException e) {
            LOGGER.error("copy file failed from {} to {}, cause:{}", oldPath, newPath, e.getMessage());
        }
    }

    public static void loadJar(String jarPath) {
        File jarFile = new File(jarPath);
        Method method = null;
        try {
            method = WebappClassLoaderBase.class.getDeclaredMethod(ADD_URL_FUN_NAME, URL.class);
        } catch (NoSuchMethodException | SecurityException e1) {
            LOGGER.error("getDeclaredMethod failed, jarPath:{}, message:{}", jarPath, e1.getMessage());
        }
        boolean accessible = method.isAccessible();
        try {
            method.setAccessible(true);
            Class<?> clazz = FileUtils.class;
            ClassLoader classLoader = clazz.getClassLoader();
//            URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
            URL url = jarFile.toURI().toURL();
            method.invoke(classLoader, url);
        } catch (Exception e2) {
            LOGGER.error("addUrl failed, jarPath:{}, message:{}", jarPath, e2.getMessage());
        } finally {
            method.setAccessible(accessible);
        }
    }


    public static List<String> readInLine(String path) {
        try {
            return Files.lines(Paths.get(path)).collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("readLine failed from {}, message:{}", path, e.getMessage());
            return new ArrayList<>();
        }
    }
}
