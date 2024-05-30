package com.janetfilter.core.plugin;

import com.janetfilter.core.Dispatcher;
import com.janetfilter.core.Environment;
import com.janetfilter.core.commons.ConfigParser;
import com.janetfilter.core.commons.DebugInfo;
import com.janetfilter.core.utils.StringUtils;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public final class PluginManager {
    private static final String ENTRY_NAME = "JANF-Plugin-Entry";

    private final Instrumentation inst;
    private final Dispatcher dispatcher;
    private final Environment environment;

    public PluginManager(Dispatcher dispatcher, Environment environment) {
        this.inst = environment.getInstrumentation();
        this.dispatcher = dispatcher;
        this.environment = environment;
    }

    public void loadPlugins() { // 定义加载插件的方法

        long startTime = System.currentTimeMillis(); // 获取当前时间，记录开始时间

        File pluginsDirectory = environment.getPluginsDir(); // 获取插件目录
        if (!pluginsDirectory.exists() || !pluginsDirectory.isDirectory()) { // 如果插件目录不存在或不是目录
            return; // 直接返回，不继续执行
        }

        File[] pluginFiles = pluginsDirectory.listFiles((d, n) -> n.endsWith(".jar")); // 获取插件目录中所有以“.jar”结尾的文件
        if (null == pluginFiles) { // 如果没有找到任何插件文件
            return; // 直接返回，不继续执行
        }

        try { // 尝试执行以下代码，并捕获异常

            ExecutorService executorService = Executors.newCachedThreadPool(); // 创建一个缓存线程池
            for (File pluginFile : pluginFiles) { // 遍历所有插件文件
                executorService.submit(new PluginLoadTask(pluginFile)); // 提交每个插件文件的加载任务到线程池
            }

            executorService.shutdown(); // 关闭线程池，不再接收新任务
            if (!executorService.awaitTermination(30L, TimeUnit.SECONDS)) { // 等待所有任务在30秒内完成
                throw new RuntimeException("Load plugin timeout"); // 如果超时，抛出运行时异常
            }

            DebugInfo.debug(String.format("============ All plugins loaded, %.2fs elapsed ============", (System.currentTimeMillis() - startTime) / 1000D)); // 打印所有插件加载完成的信息和耗时
        } catch (Throwable e) { // 捕获所有异常
            DebugInfo.error("Load plugin failed", e); // 打印加载插件失败的信息和异常
        }
    }


    private class PluginLoadTask implements Runnable {
        private final File pluginFile;

        public PluginLoadTask(File pluginFile) {
            this.pluginFile = pluginFile;
        }

        @Override
        public void run() {
            try {
                if (pluginFile.getName().endsWith(environment.getDisabledPluginSuffix())) {
                    DebugInfo.debug("Disabled plugin: " + pluginFile + ", ignored.");
                    return;
                }

                JarFile jarFile = new JarFile(pluginFile);
                Manifest manifest = jarFile.getManifest();
                String entryClass = manifest.getMainAttributes().getValue(ENTRY_NAME);
                if (StringUtils.isEmpty(entryClass)) {
                    return;
                }

                PluginClassLoader classLoader = new PluginClassLoader(jarFile);
                Class<?> klass = Class.forName(entryClass, false, classLoader);
                if (!Arrays.asList(klass.getInterfaces()).contains(PluginEntry.class)) {
                    return;
                }

                synchronized (inst) {
                    inst.appendToBootstrapClassLoaderSearch(jarFile);
                }

                PluginEntry pluginEntry = (PluginEntry) Class.forName(entryClass).newInstance();

                File configFile = new File(environment.getConfigDir(), pluginEntry.getName().toLowerCase() + ".conf");
                PluginConfig pluginConfig = new PluginConfig(configFile, ConfigParser.parse(configFile));
                pluginEntry.init(environment, pluginConfig);

                dispatcher.addTransformers(pluginEntry.getTransformers());

                DebugInfo.debug("Plugin loaded: {name=" + pluginEntry.getName() + ", version=" + pluginEntry.getVersion() + ", author=" + pluginEntry.getAuthor() + "}");
            } catch (Throwable e) {
                DebugInfo.error("Parse plugin info failed", e);
            }
        }
    }
}
