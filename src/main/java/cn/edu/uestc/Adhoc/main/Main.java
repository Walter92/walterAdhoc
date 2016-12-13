package cn.edu.uestc.Adhoc.main;

import cn.edu.uestc.Adhoc.entity.adhocNode.AdhocNode;
import cn.edu.uestc.Adhoc.entity.factory.AdhocNodeFactory;
import cn.edu.uestc.Adhoc.entity.route.RouteEntry;
import cn.edu.uestc.Adhoc.entity.systemInfo.SystemInfo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by walter on 16-11-21.
 */
public class Main {
    String AdhocIp = JOptionPane.showInputDialog("输入节点IP");
    String AdhocPortName = JOptionPane.showInputDialog("输入节点端口名");
    JFrame jFrame = new JFrame("自组网节点");
    JTextArea conslo = new ConsoleText(40,70);

    JButton sendRREQ = new JButton("发送路由请求");
    JButton sendTxt = new JButton("发送文本");
    JButton queryRoute = new JButton("查询本机路由表");
    JPanel jPanel = new JPanel(new GridLayout(10,1));
    AdhocNode adhocNode ;
    private Main(){init();}

    public static void main(String[] args) {
        new Main();
    }

    private void init(){
        JScrollPane scroll = new JScrollPane(conslo);
        //把定义的JTextArea放到JScrollPane里面去

        //分别设置水平和垂直滚动条总是出现
        scroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        try {
            int ip = Integer.parseInt(AdhocIp);
            adhocNode = AdhocNodeFactory.getInstance(AdhocPortName,ip);
        }catch (NumberFormatException e){
            System.exit(-1);
        }catch (Exception e){
            e.printStackTrace();
            System.exit(-1);
        }
        JLabel ip = new JLabel("IP:"+AdhocIp);
        JLabel portName = new JLabel("    端口名:"+AdhocPortName);
        SystemInfo systemInfo = new SystemInfo();
        JLabel sysInfo = new JLabel("    平台架构:"+ systemInfo.getOsName()+"-"+systemInfo.getOsArch());
        JPanel adhocInfo = new JPanel();
        adhocInfo.add(ip);
        adhocInfo.add(portName);
        adhocInfo.add(sysInfo);

        JLabel destIPLable = new JLabel("节点IP:");
        final JTextField destIp = new JTextField(6);
        JPanel destAdhoc = new JPanel();
        destAdhoc.add(destIPLable);
        destAdhoc.add(destIp);

        sendRREQ.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String destinationIP = destIp.getText();
                try {
                    int destNodeIP = Integer.parseInt(destinationIP);
                    adhocNode.sendRREQ(destNodeIP);
                }catch (NumberFormatException nfe){
                    return;
                }
            }
        });

        sendTxt.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String content = JOptionPane.showInputDialog("输入发送内容:");
                String destinationIP = destIp.getText();
                try {
                    int destNodeIP = Integer.parseInt(destinationIP);
                    adhocNode.sendMessage(content, destNodeIP);
                }catch (NumberFormatException nfe){
                    return;
                }
            }
        });

        queryRoute.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Map<Integer,RouteEntry> routeTable = adhocNode.queryAllRoute();
                display(routeTable);
            }
        });
        jPanel.add(destAdhoc);
        jPanel.add(sendRREQ);
        jPanel.add(sendTxt);
        jPanel.add(queryRoute);

        jFrame.add(jPanel, BorderLayout.WEST);
        jFrame.add(adhocInfo,BorderLayout.NORTH);
        jFrame.add(scroll,BorderLayout.CENTER);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        jFrame.pack();
        jFrame.setVisible(true);
    }

    private void display(Map<Integer,RouteEntry> routeTable){
        JFrame routingTable = new JFrame("本机路由表");
        String[] columnTitle={"目标IP","序列号","跳数","下一跳IP","目标系统信息","状态"};
        int rows = routeTable.size();
        Object[][] cells = new Object[rows][columnTitle.length];
        Set<Integer> keys =  routeTable.keySet();
        int i=0;
        for(Integer integer:keys){
            RouteEntry entry = routeTable.get(integer);
            cells[i][0]=entry.getDestIP();
            cells[i][1]=entry.getSeqNum();
            cells[i][2]=entry.getHopCount();
            cells[i][3]=entry.getNextHopIP();
//            JButton showSysInfo = new JButton("查看");
//            showSysInfo.addMouseListener(new MouseAdapter() {
//                @Override
//                public void mouseClicked(MouseEvent e) {
//                    JOptionPane.showMessageDialog(routingTable,entry.getSystemInfo());
//                }
//            });
            cells[i][4]=entry.getSystemInfo();
            cells[i][5]=entry.getState().getShow();
            ++i;
        }
        ExtTable extTable = new ExtTable(columnTitle,cells);
        JTable table = new JTable(extTable);
        table.setRowSelectionAllowed(false);
        table.setRowHeight(40);
        table.setRowMargin(20);
        routingTable.add(new JScrollPane(table));
        routingTable.pack();
        routingTable.setVisible(true);

    }
//
//    class DynamicDisplay implements Runnable {
//
//        private JTextComponent text;
//        private final PrintStream printWriter = System.out;
//        private PipedOutputStream pipedOutputStream = new PipedOutputStream(printWriter);
//        private PipedInputStream pipedInputStream = new PipedInputStream(printWriter);
//        public void run() {
//
//        }
//    }
    class ExtTable extends DefaultTableModel{
        public ExtTable(String[] columnTable,Object[][] cells){
            super(cells,columnTable);
        }
        public Class getColumnClass(int c){
            Object object = getValueAt(0,c);
            return object.getClass();
        }
    }
    class LoopedStreams {
        private PipedOutputStream pipedOS =
                new PipedOutputStream();
        private boolean keepRunning = true;
        private ByteArrayOutputStream byteArrayOS =
                new ByteArrayOutputStream(1024) {
                    public void close() {
                        keepRunning = false;
                        try {
                            super.close();
                            pipedOS.close();
                        }
                        catch(IOException e) {
                            System.exit(1);
                        }
                    }
                };


        private PipedInputStream pipedIS = new PipedInputStream() {
            public void close() {
                keepRunning = false;
                try    {
                    super.close();
                }
                catch(IOException e) {
                    System.exit(1);
                }
            }
        };


        public LoopedStreams() throws IOException {
            pipedIS.connect(pipedOS);
            startByteArrayReaderThread();
        }
        public InputStream getInputStream() {
            return pipedIS;
        }
        public OutputStream getOutputStream() {
            return byteArrayOS;
        }
        private void startByteArrayReaderThread() {
            new Thread(new Runnable() {
                public void run() {
                    while(keepRunning) {
                        if(byteArrayOS.size() > 0) {
                            byte[] buffer = null;
                            synchronized(byteArrayOS) {
                                buffer = byteArrayOS.toByteArray();
                                byteArrayOS.reset(); // 清除缓冲区
                            }
                            try {
                                pipedOS.write(buffer, 0, buffer.length);
                            }
                            catch(IOException e) {
                                System.exit(1);
                            }
                        }
                        else
                        {

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }).start();
        }
    }

    public class ConsoleText extends JTextArea
    {
        private static final long serialVersionUID = 1L;
        public ConsoleText() {
            this(0,0);
        }
        public ConsoleText(int x,int y){
            super(x,y);
            LoopedStreams ls = null;
            try {
                ls = new LoopedStreams();
            } catch (IOException e) {
                e.printStackTrace();
            }
            PrintStream ps = new PrintStream(ls.getOutputStream(),true);
            System.setOut(ps);
            System.setErr(ps);
            startConsoleReaderThread(ls.getInputStream());
        }

        private void startConsoleReaderThread(InputStream inStream)
        {
            final BufferedReader br =new BufferedReader(new InputStreamReader(inStream));
            new Thread(new Runnable() {
                public void run() {
                    StringBuffer sb = new StringBuffer();
                    try {
                        String s;
                        while( (s=br.readLine()) != null) {
                            sb.setLength(0);
                            append(sb.append(s).append("\n").toString());
                            setCaretPosition(getText().length());
                        }
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }


                }
            }).start();
        }
    }
}
