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

import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.types.DeviceType;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.BorderUIResource;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;

public class MHFSGUI extends JFrame implements WindowListener {

    private static final long serialVersionUID = 1L;

    private final JFileChooser jfc;

    private Network network;
    private final HFSMonitor monitor;

    private final JTextArea log = new JTextArea("Log:\n");
    private final MHFSLogger logger = new GUILogger(log);

    private final UPnpServiceProxy upnpProxy = new UPnpServiceProxy(logger);

    private MHFSGUI() {
        super("MHFS/Minimal Http File Server)");

        monitor = new HFSMonitor(logger);
        log.setEditable(false);

        new SettingsManager(monitor).loadSettings();

        setupPlugins();

        addWindowListener(this);

        JTabbedPane tabbed = new JTabbedPane();

        tabbed.addTab("Settings", new JScrollPane(new SettingsPane()));
        tabbed.addTab("Progress", new JScrollPane(new NetworkProgressPane()));
//        tabbed.addTab("Shared Files", new JScrollPane(new SharedFilesPane()));
        tabbed.addTab("Custom Files", new JScrollPane(new CustomFilePane()));
        tabbed.addTab("IP Address", new JScrollPane(new IPAddressPane()));
        tabbed.addTab("UPnP", new JScrollPane(new UPnPPane()));
        tabbed.addTab("Log", new JScrollPane(log));
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

    public static void main(String[] args) {
        JFrame frame = new MHFSGUI();
        frame.setVisible(true);
    }


    private class SettingsPane extends Container implements Observer, UPnpServiceProxy.OnUpnpChange {
        private static final long serialVersionUID = 1L;

        private boolean defaultUpnpSelected = false;
        private boolean defaultIpSelected = false;

        private final JButton startStopButton = new JButton("START");
        private final JButton saveButton = new JButton("Save settings");
        private JTextField portField;
        private JTextField speedField;
        private JTextField baseDirectory;
        private JTextField uploadDirectory;
        private final JButton chooseBaseDirButton = new JButton("...");
        private final JButton chooseUploadDirButton = new JButton("...");
        private JComboBox<String> upnpIpSelect;
        private JComboBox<UPnPDeviceLabel> upnpDeviceSelect;

        SettingsPane() {
            monitor.addObserver(this);
            Container content = this;
            content.setLayout(new RiverLayout());

            startStopButton.addActionListener(e -> {
                if (monitor.getNetwork()) {
                    network.stopNet();
                    monitor.stopNet();
                    monitor.setNetwork(false);

                    if (!HFSMonitor.NO_UPNP_IP.equals(monitor.getUpnpHost())) {
                        upnpProxy.removePortMapping(monitor.getInMemory().port, monitor.getUpnpHost());
                    }
                } else {
                    network = new Network(logger, monitor);
                    network.start();
                    monitor.setNetwork(true);

                    String ip = (String) upnpIpSelect.getSelectedItem();
                    UPnPDeviceLabel deviceLabel = (UPnPDeviceLabel) upnpDeviceSelect.getSelectedItem();
                    if (!HFSMonitor.NO_UPNP_IP.equals(ip) &&
                            deviceLabel != null &&
                            !HFSMonitor.NO_UPNP_DEVICE.equals(deviceLabel.toString())) {
                        monitor.setUpnpHost(ip);
                        upnpProxy.addPortMapping((RemoteDevice) deviceLabel.device, monitor.getInMemory().port, monitor.getUpnpHost());
                    }
                }

                evaluateStartStopButton();
            });

            saveButton.addActionListener(e -> {
                new SettingsManager(monitor).saveSettings();
            });
            saveButton.setEnabled(monitor.needsSave());

            chooseBaseDirButton.addActionListener(e -> {
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnVal = jfc.showOpenDialog(this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = jfc.getSelectedFile();
                    monitor.setBaseDir(file.getAbsolutePath());
                }
            });
            chooseUploadDirButton.addActionListener(e -> {
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnVal = jfc.showOpenDialog(this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = jfc.getSelectedFile();
                    monitor.setUploadDir(file.getAbsolutePath());
                }
            });

            portField = new JTextField(Integer.toString(monitor.getInMemory().port), 4);
            portField.getDocument().addDocumentListener(new SimpleDocumentListener() {
                @Override
                public void onEvent(DocumentEvent e) {
                    try {
                        monitor.setPort(Integer.parseInt(portField.getText()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            });

            speedField = new JTextField(Integer.toString(monitor.getInMemory().speed), 10);
            speedField.getDocument().addDocumentListener(new SimpleDocumentListener() {
                @Override
                public void onEvent(DocumentEvent e) {
                    try {
                        monitor.setSpeed(Integer.parseInt(speedField.getText()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            });

            baseDirectory = new JTextField(monitor.getInMemory().baseDir);
            uploadDirectory = new JTextField(monitor.getInMemory().uploadDir);
            baseDirectory.setEnabled(false);
            uploadDirectory.setEnabled(false);

            upnpIpSelect = new JComboBox<>();
            upnpDeviceSelect = new JComboBox<>();

            upnpIpSelect.addItem(HFSMonitor.NO_UPNP_IP);
            upnpDeviceSelect.addItem(UPnPDeviceLabelNone.NONE);

            updateNetwork();
            updateUpnpDevices();

            // Add action listener HAS to be done after loading items and settings,
            // otherwise the item from the saved settings will be overwritten at once
            upnpIpSelect.addActionListener(e -> {
                String selectedItem = (String) upnpIpSelect.getSelectedItem();
                monitor.setUpnpIp(selectedItem);
            });

            upnpDeviceSelect.addActionListener(e -> {
                Object o = upnpDeviceSelect.getSelectedItem();
                if (o != null) {
                    String selectedItem = o.toString();
                    monitor.setUpnpDevice(selectedItem);
                }
            });

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
            content.add("p left", new JLabel("UPnP"));
            content.add("hfill", upnpDeviceSelect);
            content.add("", new JLabel("to"));
            content.add("hfill", upnpIpSelect);
            content.add("p left", saveButton);
            content.add("", startStopButton);
            content.add("", new NetworkIndicatorComponent());

            upnpProxy.addListener(this);
        }

        private void evaluateStartStopButton() {
            if (!monitor.getNetwork()) {
                portField.setEnabled(true);
                upnpIpSelect.setEnabled(true);
                upnpDeviceSelect.setEnabled(true);
                startStopButton.setText("START");
            } else {
                portField.setEnabled(false);
                upnpIpSelect.setEnabled(false);
                upnpDeviceSelect.setEnabled(false);
                startStopButton.setText("STOP");
            }
        }

        private void updateNetwork() {
            HashSet<String> removeSet = new HashSet<>();
            for (int i = 1; i < upnpIpSelect.getItemCount(); i++) {
                removeSet.add(upnpIpSelect.getItemAt(i));
            }

            try {
                InetAddress[] all = InetAddress.getAllByName(InetAddress
                        .getLocalHost().getHostName());
                for (InetAddress anAll : all) {
                    final String hostAddress = anAll.getHostAddress();
                    if (removeSet.contains(hostAddress)) {
                        removeSet.remove(hostAddress);
                    } else {
                        upnpIpSelect.addItem(hostAddress);
                    }
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            for (int i = upnpIpSelect.getItemCount() - 1; i >= 0; i--) {
                if (removeSet.contains(upnpIpSelect.getItemAt(i))) {
                    upnpIpSelect.removeItemAt(i);
                }
            }

            if (!defaultIpSelected) {
                String defaultIp = monitor.getInSettings().upnpIp;
                if (defaultIp != null) {
                    for (int i = 0; i < upnpIpSelect.getItemCount(); i++) {
                        if (defaultIp.equals(upnpIpSelect.getItemAt(i))) {
                            defaultIpSelected = true;
                            upnpIpSelect.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            }

            globalRepaint();
        }

        private void updateUpnpDevices() {
            HashSet<Device> removeSet = new HashSet<>();
            for (int i = 1; i < upnpDeviceSelect.getItemCount(); i++) {
                removeSet.add(upnpDeviceSelect.getItemAt(i).device);
            }

            for (Device device : upnpProxy.getDevices()) {
                if (removeSet.contains(device)) {
                    removeSet.remove(device);
                } else {
                    DeviceType type = device.getType();
                    if (type.getType().equalsIgnoreCase(UPnpServiceProxy.SUPPORTED_DEVICE)) {
                        upnpDeviceSelect.addItem(new UPnPDeviceLabel(device));
                    }
                }
            }

            for (int i = upnpDeviceSelect.getItemCount() - 1; i >= 0; i--) {
                if (removeSet.contains(upnpDeviceSelect.getItemAt(i).device)) {
                    upnpDeviceSelect.removeItemAt(i);
                }
            }

            if (!defaultUpnpSelected) {
                String defaultUpnp = monitor.getInSettings().upnpDevice;
                if (defaultUpnp != null) {
                    for (int i = 0; i < upnpDeviceSelect.getItemCount(); i++) {
                        if (defaultUpnp.equals(upnpDeviceSelect.getItemAt(i).toString())) {
                            System.out.println("FOUND DEFAULT " + defaultUpnp);
                            defaultUpnpSelected = true;
                            upnpDeviceSelect.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            }

            globalRepaint();
        }

        @Override
        public void update(Observable o, Object arg) {
            SwingUtilities.invokeLater(() -> {
                if (((UpdateEvent) arg).contains(UpdateEvent.Type.SETTINGS)) {
                    final String baseDir = monitor.getInMemory().baseDir;
                    if (!baseDirectory.getText().equals(baseDir)) {
                        baseDirectory.setText(baseDir);
                    }
                    String monitorSpeed = Integer.toString(monitor.getInMemory().speed);
                    if (!speedField.getText().equals(monitorSpeed)) {
                        speedField.setText(monitorSpeed);
                    }
                    String monitorPort = Integer.toString(monitor.getInMemory().port);
                    if (!portField.getText().equals(monitorPort)) {
                        portField.setText(monitorPort);
                    }
                    String monitorUploadDir = monitor.getInMemory().uploadDir;
                    if (!uploadDirectory.getText().equals(monitorUploadDir)) {
                        uploadDirectory.setText(monitorUploadDir);
                    }

                    evaluateStartStopButton();

                    saveButton.setEnabled(monitor.needsSave());
                }
                if (((UpdateEvent) arg).contains(UpdateEvent.Type.IP_ADDRESS)) {
                    updateNetwork();
                }
            });
        }

        @Override
        public Void onUpnpChange() {
            SwingUtilities.invokeLater(this::updateUpnpDevices);
            return null;
        }
    }

    private class NetworkIndicatorComponent extends JComponent implements
            Observer {
        private static final long serialVersionUID = 1L;

        NetworkIndicatorComponent() {
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
            SwingUtilities.invokeLater(() -> {
                if (((UpdateEvent) arg1).contains(UpdateEvent.Type.SETTINGS)) {
                    repaint();
                }
            });
        }
    }

    private class NetworkProgressPane extends Container implements Observer {
        private static final long serialVersionUID = 1L;
        private Container c;

        NetworkProgressPane() {
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
            SwingUtilities.invokeLater(() -> {
                if (((UpdateEvent) arg1).contains(UpdateEvent.Type.PROGRESS)) {
                    updateGfx();
                }
            });
        }
    }

    private class CustomFilePane extends Container implements Observer {
        private static final long serialVersionUID = 1L;
        private Container c;
        private JButton chooseFileButton = new JButton("Add file(s)");
        private JButton saveButton = new JButton("Save settings");

        CustomFilePane() {
            c = this;

            monitor.addObserver(this);

            chooseFileButton.addActionListener(e -> {
                jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                jfc.setMultiSelectionEnabled(true);
                int returnVal = jfc.showOpenDialog(this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File[] files = jfc.getSelectedFiles();
                    for (File f : files) {
                        monitor.addCustomFile(f);
                    }
                }
                jfc.setMultiSelectionEnabled(false);
            });

            saveButton.addActionListener(e -> {
                new SettingsManager(monitor).saveSettings();
            });
            saveButton.setEnabled(monitor.needsSave());

            c.setLayout(new RiverLayout());
            updateGfx();
        }

        private void updateGfx() {
            c.removeAll();

            c.add("p left", chooseFileButton);
            c.add("", saveButton);

            if (monitor.isCustomList()) {
                String[] dirs = monitor.getCustomDirList().getDirStrings();
                for (final String s : dirs) {
                    File f = monitor.getCustomDirList().lookup(s);

                    JButton b = new JButton("Remove");
                    b.addActionListener(e -> monitor.removeCustomFile(s));
                    c.add("br tab", b);

                    JLabel name = new JLabel(s);
                    name.setBorder(BorderUIResource.getLoweredBevelBorderUIResource());
                    c.add("tab", name);

                    JTextField path = new JTextField(f.getAbsolutePath());
                    path.setEditable(false);
                    c.add("tab hfill", path);
                }
            }

            repaint();
        }

        @Override
        public void update(Observable o, Object arg) {
            SwingUtilities.invokeLater(() -> {
                if (((UpdateEvent) arg).contains(UpdateEvent.Type.CUSTOM_FILES)) {
                    updateGfx();
                }
                if (((UpdateEvent) arg).contains(UpdateEvent.Type.SETTINGS)) {
                    saveButton.setEnabled(monitor.needsSave());
                }
            });
        }
    }

    private class IPAddressPane extends Container implements Observer {
        private Container ipAddressContainer;
        private static final long serialVersionUID = 1L;

        IPAddressPane() {
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
                for (InetAddress anAll : all) {
                    final String hostAddress = anAll.getHostAddress();
                    final int port = monitor.getInMemory().port;

                    NetworkInterface nif = NetworkInterface.getByInetAddress(anAll);

                    JTextField ipField = new JTextField(String.format("http://%s:%d", hostAddress, port));
                    ipField.setEditable(false);
                    ipAddressContainer.add("br", new JLabel(nif.getDisplayName()));
                    ipAddressContainer.add("tab hfill", ipField);
                }
            } catch (UnknownHostException | SocketException e) {
                e.printStackTrace();
            }
            repaint();
            globalRepaint();
        }

        @Override
        public void update(Observable o, Object arg) {
            SwingUtilities.invokeLater(() -> {
                if (((UpdateEvent) arg).contains(UpdateEvent.Type.IP_ADDRESS)) {
                    updateGfx();
                }
            });
        }

    }

    private class UPnPPane extends Container implements Observer, UPnpServiceProxy.OnUpnpChange {
        private Container upnpDeviceContainer;
        private static final long serialVersionUID = 1L;

        UPnPPane() {
            Container content = this;
            content.setLayout(new RiverLayout());

            upnpProxy.addListener(this);

            upnpDeviceContainer = new Container();
            upnpDeviceContainer.setLayout(new RiverLayout());

            updateGfx();

            content.add("p hfill", new JLabel("UPnP devices on your network"));
            content.add("p hfill vfill", upnpDeviceContainer);

            monitor.addObserver(this);
        }

        private void updateGfx() {
            upnpDeviceContainer.removeAll();

            Collection<Device> devices = upnpProxy.getDevices();
            for (Device device : devices) {
                upnpDeviceContainer.add("br hfill", new JLabel(device.getDisplayString()));
            }

            repaint();
            globalRepaint();
        }

        @Override
        public void update(Observable o, Object arg) {
            SwingUtilities.invokeLater(() -> {
                if (((UpdateEvent) arg).contains(UpdateEvent.Type.IP_ADDRESS)) {
                    updateGfx();
                }
            });
        }

        @Override
        public Void onUpnpChange() {
            SwingUtilities.invokeLater(() -> {
                updateGfx();
            });
            return null;
        }
    }

    private class AboutPane extends Container {

        private static final long serialVersionUID = 1L;

        AboutPane() {
            Container content = this;
            content.setLayout(new RiverLayout());

            JTextArea jtext = new JTextArea(10, 10);
            jtext.setEditable(false);

            jtext.setLineWrap(true);
            jtext.setWrapStyleWord(true);
            // jtext.setMaximumSize(new Dimension(20,30));

            jtext.append("Minimal HTTP File Server (MHFS) for file uploads/downloads.\n");
            jtext.append("\n");
            jtext.append("FAQ:\n");
            jtext.append("Q: Why is my status Offline (Red ball)\n");
            jtext.append("A: The port you want to use is probably not available on your computer, choose another port and click \"Save\" then \"STOP\" and then \"START\". You can also check the log for error messages.\n");
            jtext.append("\n");
            jtext.append("https://github.com/bes/MHFS");

            content.add("p hfill", new JLabel("About"));
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

    private static class UPnPDeviceLabel {
        final Device device;

        private UPnPDeviceLabel(Device device) {
            this.device = device;
        }

        @Override
        public String toString() {
            String s = device.getDisplayString();
            if (s != null) {
                return s.substring(0, Math.min(s.length()-1, 30));
            }
            return "No name";
        }
    }

    private static class UPnPDeviceLabelNone extends UPnPDeviceLabel {

        private static final UPnPDeviceLabelNone NONE = new UPnPDeviceLabelNone();

        private UPnPDeviceLabelNone() {
            super(null);
        }

        @Override
        public String toString() {
            return HFSMonitor.NO_UPNP_DEVICE;
        }
    }

    private static abstract class SimpleDocumentListener implements DocumentListener {
        @Override
        public final void insertUpdate(DocumentEvent e) {
            onEvent(e);
        }

        @Override
        public final void removeUpdate(DocumentEvent e) {
            onEvent(e);
        }

        @Override
        public final void changedUpdate(DocumentEvent e) {
            onEvent(e);
        }

        abstract public void onEvent(DocumentEvent e);
    }
}
