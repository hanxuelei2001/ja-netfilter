package com.janetfilter.core;

import com.janetfilter.core.attach.VMLauncher;
import com.janetfilter.core.attach.VMSelector;
import com.janetfilter.core.commons.DebugInfo;
import com.janetfilter.core.utils.WhereIsUtils;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.util.jar.JarFile;

public class Launcher {
    public static final String ATTACH_ARG = "--attach";
    public static final String VERSION = "2022.2.0";
    public static final int VERSION_NUMBER = 202201000;

    private static boolean loaded = false;

    public static void main(String[] args) {
        URI jarURI;
        try {
            jarURI = WhereIsUtils.getJarURI();
        } catch (Throwable e) {
            DebugInfo.error("Can not locate `ja-netfilter` jar file.", e);
            return;
        }

        String jarPath = jarURI.getPath();
        if (args.length > 1 && args[0].equals(ATTACH_ARG)) {
            VMLauncher.attachVM(jarPath, args[1], args.length > 2 ? args[2] : null);
            return;
        }

        printUsage();

        try {
            new VMSelector(new File(jarPath)).select();
        } catch (Throwable e) {
            System.err.println("  ERROR: Select virtual machine failed.");
            e.printStackTrace(System.err);
        }
    }

    /**
     * premain
     * java jvm 执行 main 之前会调用 premain
     * @param args args
     * @param inst Inst
     */
    public static void premain(String args, Instrumentation inst) {
        premain(args, inst, false);
    }

    public static void agentmain(String args, Instrumentation inst) {
        // 如果没有设置 debug 环境变量，则设置为 1
        if (null == System.getProperty("janf.debug")) {
            System.setProperty("janf.debug", "1");
        }

        // 如果没有设置 output 环境变量，则设置为 3
        if (null == System.getProperty("janf.output")) {
            System.setProperty("janf.output", "3");
        }

        // 执行 premain
        premain(args, inst, true);
    }

    /**
     * premain
     *
     * @param args       args
     * @param inst       Inst
     * @param attachMode 附加模式
     */
    private static void premain(String args, Instrumentation inst, boolean attachMode) {
        // 防止多次加载
        if (loaded) {
            DebugInfo.warn("You have multiple `ja-netfilter` as javaagent.");
            return;
        }

        // 打印使用说明
        printUsage();

        // 获取 jar 文件路径
        URI jarURI;
        try {
            // 设置已加载
            loaded = true;
            // 获取 jar 文件路径
            // 目的是为了获取自己这个 jar 的所在目录
            jarURI = WhereIsUtils.getJarURI();
        } catch (Throwable e) {
            DebugInfo.error("Can not locate `ja-netfilter` jar file.", e);
            return;
        }

        // 获取 jar 文件
        File agentFile = new File(jarURI.getPath());
        try {
            // 获取Instrumentation接口实例有两种方式：
            //
            //当JVM以指定代理类的方式启动时，会将Instrumentation实例传递给代理类的premain方法。
            //当JVM提供了一种机制，可以在JVM启动后随时启动代理时，会将Instrumentation实例传递给代理代码的agentmain方法。
            //这些机制在包规范中有详细描述。
            //
            //一旦代理获得了Instrumentation实例，代理可以随时调用该实例上的方法。
            // 添加到 bootstrap classloader
            inst.appendToBootstrapClassLoaderSearch(new JarFile(agentFile));
        } catch (Throwable e) {
            DebugInfo.error("Can not access `ja-netfilter` jar file.", e);
            return;
        }

        // 初始化环境
        Initializer.init(new Environment(inst, agentFile, args, attachMode)); // for some custom UrlLoaders
    }

    private static void printUsage() {
        String content = "\n  ============================================================================  \n" +
                "\n" +
                "    ja-netfilter " + VERSION +
                "\n\n" +
                "    A javaagent framework :)\n" +
                "\n" +
                "    https://github.com/ja-netfilter/ja-netfilter\n" +
                "\n" +
                "  ============================================================================  \n\n";

        System.out.print(content);
    }
}
