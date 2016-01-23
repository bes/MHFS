/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Erik Zivkovic
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package se.bes.mhfs.gui;

import se.bes.mhfs.events.UpdateEvent;
import se.bes.mhfs.logger.GUILogger;
import se.bes.mhfs.logger.MHFSLogger;
import se.bes.mhfs.manager.HFSMonitor;
import se.bes.mhfs.manager.SettingsManager;
import se.bes.mhfs.network.Network;
import se.bes.mhfs.network.NetworkInstance;
import se.bes.mhfs.plugin.Plugin;
import se.bes.mhfs.upnp.UPnpServiceProxy;
import se.datadosen.component.RiverLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

public class MHFSGUI extends JFrame implements ActionListener, WindowListener {

    private static final long serialVersionUID = 1L;
    private JButton stopStartButton = new JButton("START");
    private JButton saveButton = new JButton("Save");
    private JButton htmlUpdateButton = new JButton("User HTML");
    private JButton hfsHTMLUpdateButton = new JButton("HFS HTML");
    private JButton chooseFileButton = new JButton("Add file(s)");
    private JButton chooseBaseDirButton = new JButton("...");
    private JButton chooseUploadDirButton = new JButton("...");

    private final JFileChooser jfc;

    private JTextField portField;
    private JTextField speedField;
    private JTextField baseDirectory;
    private JTextField uploadDirectory;
    private JTextArea log = new JTextArea("Log:\n");
    private JTextArea alternateHTMLArea;
    private Network n;
    private HFSMonitor monitor;
    private MHFSGUI hfs = this;

    public MHFSGUI() {
        super("HFS/minimal (Http File Server)");

        monitor = new HFSMonitor();

        new SettingsManager(monitor).loadSettings();

        setupPlugins();

        addWindowListener(this);

        JTabbedPane tabbed = new JTabbedPane();

        NetworkProgressPane npp = new NetworkProgressPane();
        SettingsPane sp = new SettingsPane();
        AlternateHTMLPagePane ahpp = new AlternateHTMLPagePane();
        CustomFilePane cfp = new CustomFilePane();

        tabbed.addTab("Settings", new JScrollPane(sp));
        tabbed.addTab("Progress", new JScrollPane(npp));
//        tabbed.addTab("Shared Files", new JScrollPane(new SharedFilesPane()));
        tabbed.addTab("Custom Files", new JScrollPane(cfp));
        tabbed.addTab("IP Address", new JScrollPane(new IPAddressPane()));
        tabbed.addTab("Log", new JScrollPane(log));
        tabbed.addTab("Alternate HTML", new JScrollPane(ahpp));
        tabbed.addTab("About", new JScrollPane(new AboutPane()));

        for(String s : monitor.getPluginManager().getPluginStrings()){
            System.out.println("PLUGIN " + s);
            Plugin p = monitor.getPluginManager().getPluginByIdentifier(s);
            tabbed.addTab(p.getName(), p);
        }

        jfc = new JFileChooser(".");

        add(tabbed);
        pack();
        setSize(700, 400);
        setVisible(true);
    }

    private void setupPlugins(){
        //new FirstPlugin(monitor);
        monitor.setupPlugins();
    }

    private void globalRepaint() {
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent a) {
        if (a.getSource() == stopStartButton) {
            if (monitor.getNetwork()) {
                n.stopNet();
                monitor.stopNet();
                monitor.setNetwork(false);
                stopStartButton.setText("START");
            } else {
                int port = Integer.parseInt(portField.getText());
                double speed = Double.parseDouble(speedField.getText());
                monitor.setSpeed(speed);
                monitor.setPort(port);
                MHFSLogger logger = new GUILogger(log);
                n = new Network(logger, monitor);
                n.start();
                monitor.setNetwork(true);
                stopStartButton.setText("STOP");
            }
        } else if (a.getSource() == saveButton) {
            // spara undan v�rdena f�rst (pga notify)
            int port = Integer.parseInt(portField.getText());
            double speed = Double.parseDouble(speedField.getText());
            monitor.setPort(port);
            monitor.setSpeed(speed);
            new SettingsManager(monitor).saveSettings();
        } else if (a.getSource() == htmlUpdateButton) {
            monitor.setAlternateHTML(alternateHTMLArea.getText());
        } else if (a.getSource() == hfsHTMLUpdateButton) {
            monitor.setAlternateHTML("");
        } else if (a.getSource() == chooseFileButton) {
            jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            jfc.setMultiSelectionEnabled(true);
            int returnVal = jfc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File[] files = jfc.getSelectedFiles();
                for (File f : files)
                    monitor.getDirList().addCustom(f);
            }
            jfc.setMultiSelectionEnabled(false);
        } else if (a.getSource() == chooseBaseDirButton) {
            jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            // int returnVal = jfc.showDialog(this, "Choose Directory");
            int returnVal = jfc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = jfc.getSelectedFile();
                //monitor.getDirList().setBaseDir(file.getAbsolutePath());
                monitor.getFSList().setBaseDir(file.getAbsolutePath());
            }
        } else if (a.getSource() == chooseUploadDirButton) {
            jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            // int returnVal = jfc.showDialog(this, "Choose Directory");
            int returnVal = jfc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = jfc.getSelectedFile();
                monitor.setUploadDir(file.getAbsolutePath());
            }
        }
    }

    public static void main(String[] args) {
        JFrame frame = new MHFSGUI();
        frame.setVisible(true);
    }

    public class SettingsPane extends Container implements Observer {
        private static final long serialVersionUID = 1L;

        public SettingsPane() {
            monitor.addObserver(this);
            Container content = this;
            content.setLayout(new RiverLayout());

            stopStartButton.addActionListener(hfs);
            saveButton.addActionListener(hfs);
            chooseBaseDirButton.addActionListener(hfs);
            chooseUploadDirButton.addActionListener(hfs);

            portField = new JTextField(Integer.toString(monitor.getPort()), 4);
            speedField = new JTextField(Double.toString(monitor.getSpeed()), 10);
            baseDirectory = new JTextField(monitor.getFSList().getBaseDir());
            uploadDirectory = new JTextField(monitor.getUploadDir());
            baseDirectory.setEnabled(false);
            uploadDirectory.setEnabled(false);

            content.add("p", new JLabel("Port: "));
            content.add("", portField);
            content.add("", new JLabel("Speed (\u00B110%): ")); // +/- character is u00B1
            content.add("", speedField);
            content.add("", new JLabel("kB/s (0 = unlimited) "));
            content.add("p", new JLabel("Base directory: "));
            content.add("hfill", baseDirectory);
            content.add("right", chooseBaseDirButton);
            content.add("p", new JLabel("Upload directory: "));
            content.add("hfill", uploadDirectory);
            content.add("right", chooseUploadDirButton);
            content.add("p left", saveButton);
            content.add("", stopStartButton);
            content.add("", new NetworkIndicatorComponent());

            // pack();
        }

        @Override
        public void update(Observable o, Object arg) {
            if (((UpdateEvent) arg).getEvent("settings")) {
                baseDirectory.setText(monitor.getFSList().getBaseDir());
                speedField.setText(Double.toString(monitor.getSpeed()));
                portField.setText(Integer.toString(monitor.getPort()));
                uploadDirectory.setText(monitor.getUploadDir());
            }
        }
    }

    public class NetworkIndicatorComponent extends JComponent implements
            Observer {
        private static final long serialVersionUID = 1L;

        public NetworkIndicatorComponent() {
            monitor.addObserver(this);
        }

        @Override
        public void paint(Graphics g) {
            if (monitor.getNetwork())
                g.setColor(Color.green);
            else
                g.setColor(Color.red);
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.fillOval(2, 2, 16, 16);
            g.setColor(Color.black);
            if (monitor.getNetwork())
                g.drawString("Online", 20, 15);
            else
                g.drawString("Offline", 20, 15);

        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(120, 20);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(120, 20);
        }

        @Override
        public void update(Observable arg0, Object arg1) {
            if (((UpdateEvent) arg1).getEvent("settings"))
                repaint();
        }

    }

    public class NetworkProgressPane extends Container implements Observer {
        private static final long serialVersionUID = 1L;
        private Container c;

        public NetworkProgressPane() {
            c = this;

            monitor.addObserver(this);

            c.setLayout(new RiverLayout());
            updateGfx();
        }

        private void updateGfx() {
            c.removeAll();

            LinkedList<NetworkInstance> lni = monitor.getNetworkInstances();
            for (NetworkInstance ni : lni) {
                c.add("tab left", ni.getNameLabel());
                c.add("tab hfill", ni.getProgressBar());
                c.add("br hfill", new JLabel(""));
            }

            repaint();
        }

        @Override
        public void update(Observable arg0, Object arg1) {
            if (((UpdateEvent) arg1).getEvent("progress"))
                updateGfx();
        }
    }

/*    public class SharedFilesPane extends Container implements Observer {
        private static final long serialVersionUID = 1L;
        private Container c;

        public SharedFilesPane() {
            c = this;

            monitor.addObserver(this);
            c.setLayout(new RiverLayout());
            updateGfx();
        }

        private void updateGfx() {
            c.removeAll();

            c.add("p left", new JLabel("Shared Files:"));
            c.add("br hfill", new JLabel(""));

            if(monitor.getDirList().isCustomList())
                recurseFolder(monitor.getDirList());
            else
                recurseFolder2(monitor.getFSList());

            repaint();
        }

        private void recurseFolder(DirList d){
            String[] dirs = d.getDirDirStrings();
            for(String s : dirs){
                recurseFolder(d.lookupDir(s));
            }

            String[] fils = d.getDirStrings();
            for (String s : fils) {
                File f = d.lookup(s);
                c.add("tab left", new JLabel(f.getAbsolutePath() + "\\"
                        + f.getName()));
                c.add("br hfill", new JLabel(""));
            }
        }

        private void recurseFolder2(FSList d){
            String[] dirs = d.getDirs(dirString)();
            for(String s : dirs){
                recurseFolder(d.lookupDir(s));
            }

            String[] fils = d.getDirStrings();
            for (String s : fils) {
                File f = d.lookup(s);
                c.add("tab left", new JLabel(f.getAbsolutePath() + "\\"
                        + f.getName()));
                c.add("br hfill", new JLabel(""));
            }
        }

        public void update(Observable arg0, Object arg1) {
            if (((UpdateEvent) arg1).getEvent("sharedFiles"))
                updateGfx();
        }
    }
*/

    public class CustomFilePane extends Container implements Observer,
            ActionListener {
        private static final long serialVersionUID = 1L;
        private Container c;

        public CustomFilePane() {
            c = this;

            monitor.addObserver(this);

            chooseFileButton.addActionListener(hfs);
            c.setLayout(new RiverLayout());
            updateGfx();
        }

        private void updateGfx() {
            c.removeAll();

            c.add("p left", chooseFileButton);
            c.add("br hfill", new JLabel(""));

            // LinkedList<FileDescriptor> lnf = monitor.getCustomFiles();

            if (monitor.getDirList().isCustomList()) {
                String[] dirs = monitor.getDirList().getDirStrings();
                for (String s : dirs) {
                    JButton b = new JButton(s);
                    File f = monitor.getDirList().lookup(s);
                    b.addActionListener(this);
                    c.add("tab left", b);
                    c.add("tab left", new JLabel(f.getAbsolutePath()));
                    c.add("br hfill", new JLabel(""));
                }
            }

            repaint();
        }

        @Override
        public void update(Observable arg0, Object arg1) {
            if (((UpdateEvent) arg1).getEvent("customFiles"))
                updateGfx();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            monitor.getDirList().removeCustom(
                    ((JButton) e.getSource()).getText());
        }
    }

    public class IPAddressPane extends Container implements Observer {
        private Container ipAddressContainer;
        private static final long serialVersionUID = 1L;

        public IPAddressPane() {
            Container content = this;
            content.setLayout(new RiverLayout());

            ipAddressContainer = new Container();
            ipAddressContainer.setLayout(new RiverLayout());

            JTextPane info = new JTextPane();
            info.setText("These are your IP Addresses.\n"
                    + "Some of these might be local addresses (they might not be connected to the Internet).\n"
                    + "Make sure your router and/or firewall has the appropriate settings and forwards the correct port.");
            info.setEditable(false);

            updateGfx();

            content.add("p hfill", new JLabel("IP Address"));
            content.add("p hfill", info);
            content.add("p hfill vfill", ipAddressContainer);

            monitor.addObserver(this);
        }

        private void updateGfx() {
            ipAddressContainer.removeAll();
            try {
                InetAddress[] all = InetAddress.getAllByName(InetAddress
                        .getLocalHost().getHostName());
                for (int i = 0; i < all.length; i++) {
                    final String hostAddress = all[i].getHostAddress();
                    final int port = monitor.getPort();

                    JTextPane meta = new JTextPane();
                    meta.setEditable(false);
                    boolean showMeta = false;

                    UPnpServiceProxy.UpnpInfo info = UPnpServiceProxy.getMapping(port, hostAddress);
                    final JButton action;
                    if (info == null) {
                        action = new JButton("Enable UPNP");
                        action.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                UPnpServiceProxy.updateMapping(port, hostAddress);
                                updateGfx();
                            }
                        });
                    } else {
                        action = new JButton("Disable UPNP");
                        action.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                UPnpServiceProxy.closeMapping(port, hostAddress);
                                updateGfx();
                            }
                        });
                        info.setOnChange(new UPnpServiceProxy.UpnpInfo.OnChange() {
                            @Override
                            public void onChange() {
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateGfx();
                                    }
                                });
                            }
                        });
                        if (info.devices.size() + info.messages.size() > 0) {
                            final boolean newLine = !info.devices.isEmpty() && !info.messages.isEmpty();
                            meta.setText(String.join("\n", info.devices) + (newLine ? "\n" : "") + String.join("\n", info.messages));
                            showMeta = true;
                        }
                    }

                    ipAddressContainer.add("br tab", action);
                    ipAddressContainer.add("tab", new JLabel("Address " + (i + 1) + ": http://"
                            + hostAddress + ":" + monitor.getPort()));
                    if (showMeta) {
                        ipAddressContainer.add("br", meta);
                    }
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            repaint();
            globalRepaint();
        }

        @Override
        public void update(Observable o, Object arg) {
            if (((UpdateEvent) arg).getEvent("ipAddress"))
                updateGfx();
        }

    }

    public class AlternateHTMLPagePane extends Container {

        private static final long serialVersionUID = 1L;

        public AlternateHTMLPagePane() {
            Container content = this;
            content.setLayout(new RiverLayout());

            alternateHTMLArea = new JTextArea();
            alternateHTMLArea
                    .setText("<html>\n<head>\n<title>HFS/Minimal</title>\n</head>\n<body>\nHFS/Minimal\n<br/>\n<form action=\"/\" method=\"post\" enctype=\"multipart/form-data\">\n<input name=\"zipfile\" type=\"file\" /><br />\n<input type=\"submit\" name=\"submit\" value=\"Submit File\">\n</form>\n</body>\n</html>\n");

            htmlUpdateButton.addActionListener(hfs);
            hfsHTMLUpdateButton.addActionListener(hfs);

            content.add("p hfill", new JLabel("Alternate HTML Page:"));
            content.add("p hfill vfill", alternateHTMLArea);
            content.add("p left", htmlUpdateButton);
            content.add("left", hfsHTMLUpdateButton);
        }
    }

    public class AboutPane extends Container {

        private static final long serialVersionUID = 1L;

        public AboutPane() {
            Container content = this;
            content.setLayout(new RiverLayout());

            JTextArea jtext = new JTextArea(10, 10);
            jtext.setEditable(false);

            jtext.setLineWrap(true);
            jtext.setWrapStyleWord(true);
            // jtext.setMaximumSize(new Dimension(20,30));

            jtext
                    .setText("Minimal HTTP File Server (MHFS) for file uploads/downloads.\n"
                            + "\n"
                            + "FAQ:\n"
                            + "Q: Why is my status Offline (Red ball)\n"
                            + "A: Port 80 is probably not available on your computer, choose another port and click \"Save\" then \"STOP\" and then \"START\".\n"
                            + "\n"
                            + "Q: Nothing happens when i change the port.\n"
                            + "A: Press \"STOP\" then \"START\" to change port. Beware: this will cancel any and all transfers.\n"
                            + "\n"
                            + "Q: Noting happens when i change the bandwidth limit.\n"
                            + "A: Press \"Save\".");

            content.add("p hfill", new JLabel("FAQ"));
            content.add("p vfill hfill", new JScrollPane(jtext));
        }
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
        System.out.println("Window closed.");
    }

    @Override
    public void windowClosing(WindowEvent e) {
        System.out.println("Window closing.");
        new SettingsManager(monitor).saveSettings();
        System.exit(0);
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

}
