package com.janetfilter.core;

import com.janetfilter.core.commons.DebugInfo;
import com.janetfilter.core.plugin.PluginManager;

import java.lang.instrument.Instrumentation;
import java.util.Set;

public class Initializer {
    /**
     * 初始化
     *
     * @param environment 环境
     */
    public static void init(Environment environment) {
        // 定义静态方法init，接收Environment对象作为参数

        DebugInfo.useFile(environment.getLogsDir());
        // 使用环境中的日志目录初始化DebugInfo
        DebugInfo.info(environment.toString());
        // 打印环境信息到DebugInfo

        Dispatcher dispatcher = new Dispatcher(environment);
        // 创建Dispatcher对象，传入环境参数
        new PluginManager(dispatcher, environment).loadPlugins();
        // 创建PluginManager对象并加载插件

        Instrumentation inst = environment.getInstrumentation();
        // 获取环境中的Instrumentation实例
        inst.addTransformer(dispatcher, true);
        // 添加字节码转换器，使用dispatcher对象
        inst.setNativeMethodPrefix(dispatcher, environment.getNativePrefix()); // 设置本地方法前缀

        Set<String> classSet = dispatcher.getHookClassNames(); // 获取需要钩子的类名集合
        for (Class<?> c : inst.getAllLoadedClasses()) { // 遍历所有已加载的类
            String name = c.getName(); // 获取类的名称
            if (!classSet.contains(name)) { // 如果类名不在需要钩子的类名集合中
                continue; // 跳过此类
            }

            try {
                c.getGenericSuperclass(); // 获取类的泛型超类（触发类加载）
                inst.retransformClasses(c); // 重新转换类的字节码
            } catch (Throwable e) { // 捕获所有异常
                DebugInfo.error("Retransform class failed: " + name, e); // 打印重新转换类失败的信息和异常
            }
        }
    }

}
