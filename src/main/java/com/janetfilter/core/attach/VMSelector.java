package com.janetfilter.core.attach;

import com.janetfilter.core.utils.DateUtils;
import com.janetfilter.core.utils.ProcessUtils;
import com.janetfilter.core.utils.WhereIsUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class VMSelector {
    private final File thisJar;
    private List<VMDescriptor> descriptors;

    public VMSelector(File thisJar) {
        this.thisJar = thisJar;
    }

    private List<VMDescriptor> getVMList() throws Exception { // 定义私有方法getVMList，返回VMDescriptor列表，抛出异常

        File jpsCommand = WhereIsUtils.findJPS(); // 使用WhereIsUtils查找jps命令的位置
        if (null == jpsCommand) { // 如果没有找到jps命令
            throw new Exception("jps command not found"); // 抛出异常，提示未找到jps命令
        }

        List<String> list = new ArrayList<>(); // 创建一个字符串列表，用于存储jps命令的输出
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); // 创建一个字节数组输出流，用于存储jps命令的输出

        ProcessUtils.start(new ProcessBuilder(jpsCommand.getAbsolutePath(), "-lv"), bos); // 使用ProcessUtils启动jps命令，参数为-lv，并将输出写入bos

        String line; // 定义一个字符串变量，用于存储每一行输出
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bos.toByteArray()))); // 创建BufferedReader，读取bos中的内容
        while ((line = reader.readLine()) != null) { // 持续读取每一行输出，直到为空
            list.add(line); // 将每一行输出添加到list中
        }

        String processId = ProcessUtils.currentId(); // 获取当前进程的ID
        return list.stream() // 将list转换为流
                .map(s -> { // 映射每个字符串为VMDescriptor对象
                    String[] section = (s + "   ").split(" ", 3); // 分割字符串为三部分，避免数组越界
                    return new VMDescriptor(section[0].trim(), section[1].trim(), section[2].trim()); // 创建VMDescriptor对象
                })
                .filter(d -> !d.getId().equals(processId) && !"sun.tools.jps.Jps".equals(d.getClassName()) && !"jdk.jcmd/sun.tools.jps.Jps".equals(d.getClassName())) // 过滤掉当前进程和jps进程
                .sorted(Comparator.comparingInt(d -> Integer.parseInt(d.getId()))) // 按进程ID排序
                .collect(Collectors.toList()); // 收集结果为列表
    }


    private String getInput() throws IOException {
        return new BufferedReader(new InputStreamReader(System.in)).readLine().trim();
    }

    private void processSelect() throws Exception {
        System.out.print("  Select: ");
        String input = getInput();

        switch (input) {
            case "Q":
            case "q":
                System.exit(0);
            case "R":
            case "r":
                System.out.println("  =========================== " + DateUtils.formatDateTime() + " ============================");
                select();
                return;
            case "":
                processSelect();
                return;
            default:
                int index;
                try {
                    index = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    invalidInput(input);
                    return;
                }

                if (index < 1) {
                    invalidInput(input);
                    return;
                }

                if (index > descriptors.size()) {
                    invalidInput(input);
                    return;
                }

                System.out.print("  Agent args: ");
                input = getInput();
                try {
                    VMLauncher.launch(thisJar, descriptors.get(index - 1), input);
                } catch (Exception e) {
                    System.err.println("> Attach to: " + index + " failed.");
                    e.printStackTrace(System.err);
                    return;
                }
                break;
        }
    }

    private void invalidInput(String input) throws Exception {
        System.err.println("> Invalid input: " + input);
        processSelect();
    }

    public void select() throws Exception { // 定义select方法，抛出异常

        boolean first = null == descriptors; // 检查descriptors是否为空，确定是否是第一次获取虚拟机列表
        List<VMDescriptor> temp = getVMList(); // 获取当前的虚拟机列表并存储在temp中
        if (null != descriptors && !descriptors.isEmpty()) { // 如果descriptors不为空且不为空列表
            temp.forEach(d -> d.setOld(descriptors.stream().anyMatch(d1 -> d.getId().equals(d1.getId())))); // 更新temp中每个虚拟机描述符的旧状态
        }

        descriptors = temp; // 将temp赋值给descriptors
        System.out.println("  Java Virtual Machine List: (Select and attach" + (first ? "" : ", + means the new one") + ")"); // 打印虚拟机列表提示信息

        int index = 1; // 初始化索引为1
        for (VMDescriptor d : descriptors) { // 遍历descriptors中的每个虚拟机描述符
            System.out.printf("  %3d]:%s%s %s%n", index++, d.getOld() ? " " : "+", d.getId(), d.getClassName()); // 打印每个虚拟机的索引、状态、ID和类名
        }
        System.out.println("    r]: <Refresh virtual machine list>"); // 打印刷新虚拟机列表选项
        System.out.println("    q]: <Quit the ja-netfilter>"); // 打印退出ja-netfilter选项

        processSelect(); // 调用processSelect方法处理用户选择
    }

}
