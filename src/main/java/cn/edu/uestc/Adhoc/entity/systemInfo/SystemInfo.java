package cn.edu.uestc.Adhoc.entity.systemInfo;

import cn.edu.uestc.Adhoc.utils.MessageUtils;

import java.util.Arrays;

/**
 * Created by walter on 15-12-3.
 */
public class SystemInfo {
    private static final String UNDER_LINE="_";
    private int processorCount;
    private int memorySize;
    private  String osName;
    private String osArch;

    public SystemInfo() {
        Runtime rt = Runtime.getRuntime();
        // 获取主机空闲内存
        long free = rt.freeMemory();
        this.memorySize = (int) (free / 1024);
        // 获取主机处理器个数
        this.processorCount = rt.availableProcessors();
        this.osName = System.getProperty("os.name");
        this.osArch = System.getProperty("os.arch");

    }

    public SystemInfo(int processorCount, int memorySize) {
        this.processorCount = processorCount;
        this.memorySize = memorySize;
    }

    public int getProcessorCount() {
        return processorCount;
    }

    public void setProcessorCount(int processorCount) {
        this.processorCount = processorCount;
    }

    public int getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(int memorySize) {
        this.memorySize = memorySize;
    }

    public byte[] getBytes() {
        byte[] sysInfo = new byte[3];
        sysInfo[0] = (byte) processorCount;
        sysInfo[1] = MessageUtils.IntToBytes(memorySize)[0];
        sysInfo[2] = MessageUtils.IntToBytes(memorySize)[1];
        byte[] osNameAndArch = (osName+UNDER_LINE+osArch).getBytes();
        byte[] sysInfos = new byte[sysInfo.length+osNameAndArch.length];
        System.arraycopy(sysInfo,0,sysInfos,0,sysInfo.length);
        System.arraycopy(osNameAndArch,0,sysInfos,sysInfo.length,osNameAndArch.length);
        return sysInfos;
    }

    public static SystemInfo recoverSysInfo(byte[] bytes) {
        SystemInfo systemInfo = new SystemInfo(bytes[0], MessageUtils.BytesToInt(new byte[]{bytes[1], bytes[2]}));
        String osNameAndArch = new String(bytes,3,bytes.length-3);
        systemInfo.setOsName(osNameAndArch.split(UNDER_LINE)[0]);
        systemInfo.setOsArch(osNameAndArch.split(UNDER_LINE)[1]);

        return systemInfo;
    }

    public String getOsArch() {
        return osArch;
    }

    public void setOsArch(String osArch) {
        this.osArch = osArch;
    }

    public String getOsName() {
        return osName;
    }

    public void setOsName(String osName) {
        this.osName = osName;
    }

    @Override
    public String toString() {
        return "the count of processor【"
                + processorCount +
                "】,memory【" + memorySize+
                "】,os name【"+osName+
                "】,os arch【"+osArch+"】";
    }


    public static void main(String[] args){
        SystemInfo systemInfo = new SystemInfo();
//        System.out.println(systemInfo);
        byte[] sysInfoBytes = systemInfo.getBytes();
        System.out.println(Arrays.toString(sysInfoBytes));
        System.out.println(sysInfoBytes.length);
        System.out.println(recoverSysInfo(sysInfoBytes));
    }
}
