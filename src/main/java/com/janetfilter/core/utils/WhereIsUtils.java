package com.janetfilter.core.utils;

import com.janetfilter.core.Launcher;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

public class WhereIsUtils {
    private static final String JAVA_HOME = System.getProperty("java.home");

    public static File findJPS() {
        String[] paths = new String[]{"bin/jps", "bin/jps.exe", "../bin/jps", "../bin/jps.exe"};

        for (String path : paths) {
            File file = new File(JAVA_HOME, path);
            if (file.exists() && file.isFile() && file.canExecute()) {
                return getCanonicalFile(file);
            }
        }

        return null;
    }

    public static File findJava() {
        String[] paths = new String[]{"bin/java", "bin/java.exe", "../bin/java", "../bin/java.exe"};

        for (String path : paths) {
            File file = new File(JAVA_HOME, path);
            if (file.exists() && file.isFile() && file.canExecute()) {
                return getCanonicalFile(file);
            }
        }

        return null;
    }

    public static File findToolsJar() {
        String[] paths = new String[]{"lib/tools.jar", "../lib/tools.jar", "../../lib/tools.jar"};

        for (String path : paths) {
            File file = new File(JAVA_HOME, path);
            if (file.exists() && file.isFile()) {
                return getCanonicalFile(file);
            }
        }

        return null;
    }

    /**
     * 获取jar uri
     *
     * @return {@link URI}
     * @throws Exception 例外
     */
    public static URI getJarURI() throws Exception {
        // 获取jar文件路径
        URL url = Launcher.class.getProtectionDomain().getCodeSource().getLocation();
        // 如果 url 不为空
        if (null != url) {
            // 返回 url 的 uri
            return url.toURI();
        }

        // 获取资源路径
        String resourcePath = "/6c81ec87e55d331c267262e892427a3d93d76683.txt";
        // 如果 url 为空，则获取资源路径
        url = Launcher.class.getResource(resourcePath);
        if (null == url) {
            throw new Exception("Can not locate resource file.");
        }

        // 获取路径
        String path = url.getPath();
        // 如果路径不是以资源路径结尾
        if (!path.endsWith("!" + resourcePath)) {
            throw new Exception("Invalid resource path.");
        }

        // 截取路径
        path = path.substring(0, path.length() - resourcePath.length() - 1);

        // 返回 uri
        return new URI(path);

    }

    private static File getCanonicalFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            return null;
        }
    }
}
