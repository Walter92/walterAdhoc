package cn.edu.uestc.Adhoc.entity.systemInfo;

import cn.edu.uestc.Adhoc.utils.MessageUtils;

import java.util.Arrays;

/**
 * Created by walter on 15-12-3.
 */
public class SystemInfo {
    private static final String UNDER_LINE = "_";
    public static final int DEFAULT_BYTE = 20;
    private int processorCount;
    private int memorySize;
    private String osName;
    private String osArch;
    private byte[] sysInfoByte;

    public SystemInfo() {
        Runtime rt = Runtime.getRuntime();
        // 获取主机空闲内存
        long free = rt.freeMemory();
        this.memorySize = (int) (free / 1024);
        // 获取主机处理器个数
        this.processorCount = rt.availableProcessors();
        this.osName = System.getProperty("os.name");
        this.osArch = System.getProperty("os.arch");
        initSysInfoBytes();
    }

    public SystemInfo(int processorCount, int memorySize) {
        this.processorCount = processorCount;
        this.memorySize = memorySize;
    }

    private void initSysInfoBytes(){
        sysInfoByte = new byte[DEFAULT_BYTE];

        sysInfoByte[0] = (byte) processorCount;
        sysInfoByte[1] = MessageUtils.IntToBytes(memorySize)[0];
        sysInfoByte[2] = MessageUtils.IntToBytes(memorySize)[1];
        byte[] osNameAndArch = (osName + UNDER_LINE + osArch).getBytes();
        for(int i=0;i<osNameAndArch.length;i++){
            sysInfoByte[3+i]=osNameAndArch[i];
        }
    }

    public byte[] getBytes() {
        return sysInfoByte;
    }

    public static SystemInfo recoverSysInfo(byte[] bytes) {
        SystemInfo systemInfo = new SystemInfo(bytes[0], MessageUtils.BytesToInt(new byte[]{bytes[1], bytes[2]}));
        String[] osNameAndArch = new String(bytes, 3, bytes.length - 3).split(UNDER_LINE);
        systemInfo.osName=osNameAndArch[0];
        systemInfo.osArch=osNameAndArch[1];
        systemInfo.initSysInfoBytes();
        return systemInfo;
    }

    public String getOsArch() {
        return osArch;
    }


    public String getOsName() {
        return osName;
    }


    @Override
    public String toString() {
        return "processor<"
                + processorCount +
                "> ,memory<" + memorySize +
                "> ,os name<" + osName +
                "> ,os arch<" + osArch.trim()+ "> ";
    }


    public static void main(String[] args) {
        SystemInfo systemInfo = new SystemInfo();
//        System.out.println(systemInfo);
        byte[] sysInfoBytes = systemInfo.getBytes();
        System.out.println(Arrays.toString(sysInfoBytes));
        System.out.println(sysInfoBytes.length);
        System.out.println(recoverSysInfo(sysInfoBytes));
    }
}
