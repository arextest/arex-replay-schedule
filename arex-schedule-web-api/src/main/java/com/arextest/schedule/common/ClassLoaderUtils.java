package com.arextest.schedule.common;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClassLoaderUtils {

  public static final String JDK_INTER_APP_CLASSLOADER = "jdk.internal.loader.ClassLoaders$AppClassLoader";

  public static void loadJar(String jarPath) {
    File jarFile = new File(jarPath);
    if (!jarFile.exists()) {
      LOGGER.error("JarFile doesn't exist! path:{}", jarPath);
      return;
    }
    try {
      loadJar(jarFile.toURI().toURL());
    } catch (Exception e) {
      LOGGER.error("Load jar failed! path:{}", jarPath, e);
    }
  }

  public static void loadJar(URL jarUrl) {
    try {
      int javaVersion = getJavaVersion();
      ClassLoader classLoader = ClassLoaderUtils.class.getClassLoader();
      Method addURL = Class.forName("java.net.URLClassLoader")
          .getDeclaredMethod("addURL", URL.class);
      addURL.setAccessible(true);

      if (javaVersion <= 8) {
        if (classLoader instanceof URLClassLoader) {
          addURL.invoke(classLoader, jarUrl);
        }
      } else if (javaVersion < 11) {
        /*
         * Due to Java 8 vs java 9+ incompatibility issues
         * See https://stackoverflow.com/questions/46694600/java-9-compatability-issue-with-classloader-getsystemclassloader/51584718
         */
        ClassLoader urlClassLoader = ClassLoader.getSystemClassLoader();
        if (!(urlClassLoader instanceof URLClassLoader)) {
          urlClassLoader = new URLClassLoader(new URL[]{jarUrl}, urlClassLoader);
        }
        addURL.invoke(urlClassLoader, jarUrl);
      } else if (JDK_INTER_APP_CLASSLOADER.equalsIgnoreCase(classLoader.getClass().getName())) {
        /**
         * append jar jdk.internal.loader.ClassLoaders.AppClassLoader
         * if java >= 11 need add jvm option:--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED
         */
        Method classPathMethod = classLoader.getClass()
            .getDeclaredMethod("appendToClassPathForInstrumentation", String.class);
        classPathMethod.setAccessible(true);
        classPathMethod.invoke(classLoader, jarUrl.getPath());
      }
    } catch (Exception e) {
      LOGGER.error("loadJar failed, message:{}", e.getMessage());
    }
  }

  private static int getJavaVersion() {
    String version = System.getProperty("java.version");
    if (version.startsWith("1.")) {
      version = version.substring(2, 3);
    } else {
      int dot = version.indexOf(".");
      if (dot != -1) {
        version = version.substring(0, dot);
      }
    }
    return Integer.parseInt(version);
  }
}
