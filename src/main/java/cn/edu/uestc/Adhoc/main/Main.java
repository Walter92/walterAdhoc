package cn.edu.uestc.Adhoc.main;

import cn.edu.uestc.Adhoc.entity.adhocNode.AdhocNode;
import cn.edu.uestc.Adhoc.entity.factory.AdhocNodeFactory;
import cn.edu.uestc.Adhoc.entity.route.RouteEntry;
import cn.edu.uestc.Adhoc.entity.systemInfo.SystemInfo;
import cn.edu.uestc.Adhoc.utils.MessageUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Created by walter on 16-11-21.
 */
public class Main {
    String AdhocIp ;
    String AdhocPortName ;
    JFrame jFrame = new JFrame("Adhoc Node");
    JTextArea conslo = new ConsoleText(40,70);

    JButton sendRREQ = new JButton("Send RREQ");
    JButton sendTxt = new JButton("Send Text");
    JButton queryRoute = new JButton("Query Route Table");
    JButton clearRouteTable = new JButton("Clean Route Table");
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
        int ip =0;
        while (true){
            try {
                AdhocIp = JOptionPane.showInputDialog("please input IP:");
                AdhocPortName = JOptionPane.showInputDialog("please input port name:");
                if(AdhocIp.trim().equals("")||AdhocPortName.trim().equals(""))
                    System.exit(-1);
                ip = Integer.valueOf(AdhocIp, 16);
                adhocNode = AdhocNodeFactory.getInstance(AdhocPortName,ip);
                break;
            }catch (NumberFormatException e){
                System.out.println("ip must be a number");
                JOptionPane.showMessageDialog(jPanel, "ip must be a number", "warn", JOptionPane.WARNING_MESSAGE);
            }catch (NoSuchElementException nee){
                System.out.println("port name is not exist");
                JOptionPane.showMessageDialog(jPanel, "port name is not exist", "warn",JOptionPane.WARNING_MESSAGE);
            }catch (Exception e){
                e.printStackTrace();
                System.exit(-1);
            }
        }
        SystemInfo systemInfo = adhocNode.getSystemInfo();
        JLabel ipLabel = new JLabel("IP:0x"+ AdhocIp.toUpperCase());
        JLabel levelLabel = new JLabel("\tlevel:"+ systemInfo.getPerformanceLevel());
        JLabel portName = new JLabel("\tPortName:"+AdhocPortName);

        JLabel sysInfo = new JLabel("\tPlatform Arch:"+ systemInfo.getOsName()+"-"+systemInfo.getOsArch());
        JPanel adhocInfo = new JPanel();
        adhocInfo.add(ipLabel);
        adhocInfo.add(portName);
        adhocInfo.add(levelLabel);
        adhocInfo.add(sysInfo);

        JLabel destIPLable = new JLabel("Destinations IP:");
        final JTextField destIp = new JTextField(6);
        JPanel destAdhoc = new JPanel();
        destAdhoc.add(destIPLable);
        destAdhoc.add(destIp);

        sendRREQ.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String destinationIP = destIp.getText();
                try {
                    int destNodeIP = Integer.valueOf(destinationIP,16);
                    adhocNode.sendRREQ(destNodeIP);
                }catch (NumberFormatException nfe){
                    return;
                }
            }
        });

        sendTxt.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String content = JOptionPane.showInputDialog("input message:");
                String destinationIP = destIp.getText();
                try {
                    int destNodeIP = Integer.valueOf(destinationIP,16);
                    adhocNode.sendMessage(content, destNodeIP);
                }catch (NumberFormatException nfe){
                    return;
                }
            }
        });

        queryRoute.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Map<Integer,RouteEntry>  routeTable = adhocNode.queryAllRoute();
                display(routeTable);
            }
        });

        clearRouteTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                adhocNode.getRouteTable().clear();
            }
        });

        jPanel.add(destAdhoc);
        jPanel.add(sendRREQ);
        jPanel.add(sendTxt);
        jPanel.add(queryRoute);
        jPanel.add(clearRouteTable);

        jFrame.add(jPanel, BorderLayout.WEST);
        jFrame.add(adhocInfo,BorderLayout.NORTH);
        jFrame.add(scroll,BorderLayout.CENTER);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        jFrame.pack();
        jFrame.setVisible(true);
    }

    private void display(Map<Integer,RouteEntry>  routeTable){
        JFrame routingTable = new JFrame("Route Table");
        String[] columnTitle={"DestIP","Seq","Hop","NextIP","SysInfo","status"};
        int rows = routeTable.size();
        Object[][] cells = new Object[rows][columnTitle.length];
        Set<Integer>  keys =  routeTable.keySet();
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
                        if(byteArrayOS.size() >  0) {
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
