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

package se.bes.mhfs.plugin;

import se.bes.mhfs.manager.HFSMonitor;
import se.bes.mhfs.network.NetworkInstance;
import se.datadosen.component.RiverLayout;

import javax.swing.*;
import java.awt.*;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class IPAddressPlugin extends Plugin {

    private static final long serialVersionUID = 1L;
    protected PrintStream printStream;
    private JTextArea jtext;
    private String addresses = "";

    public IPAddressPlugin(HFSMonitor m) {
        super(m, "IP Address/plugin", "ipAddress");

        Container content = this;
        content.setLayout(new RiverLayout());

        jtext = new JTextArea(10, 10);
        jtext.setEditable(false);
        jtext.setLineWrap(true);
        jtext.setWrapStyleWord(true);

        updateGfx();

        content.add("p hfill", new JLabel("IP Address"));
        content.add("p hfill vfill", new JScrollPane(jtext));
        monitor.addObserver(this);

    }

    public void runPlugin(Socket s) {
        try {
            printStream = new PrintStream(s.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
        NetworkInstance.HTMLBegin(printStream);
        printStream.println("These are your IP Addresses.<br>"
                + "Some of these might be local addresses (they might not be connected to the Internet).<br>"
                + "Make sure your router and/or firewall has the appropriate settings and forwards the correct port.<br><br>"
                + "" + addresses);
        NetworkInstance.HTMLEnd(printStream);
    }

    public void updateGfx() {
        try {
            addresses = "";
            InetAddress[] all = InetAddress.getAllByName(InetAddress
                    .getLocalHost().getHostName());
            for (int i = 0; i < all.length; i++) {
                addresses = addresses + "Address " + (i + 1) + ": http://"
                        + all[i].getHostAddress() + ":" + monitor.getPort()
                        + "\n";
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        jtext
                .setText("These are your IP Addresses.\n"
                        + "Some of these might be local addresses (they might not be connected to the Internet).\n"
                        + "Make sure your router and/or firewall has the appropriate settings and forwards the correct port.\n\n"
                        + "" + addresses);

    }
}
