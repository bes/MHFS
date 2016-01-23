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

package se.bes.mhfs.network;

import se.bes.mhfs.filesystem.DirList;
import se.bes.mhfs.filesystem.FSList;
import se.bes.mhfs.logger.MHFSLogger;
import se.bes.mhfs.manager.HFSMonitor;
import se.bes.mhfs.plugin.Plugin;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Hashtable;

public class NetworkInstance extends Thread implements MouseListener, ActionListener {
    private Socket mSocket;
    private DataInputStream mIn;
    //private BufferedReader in;

    private OutputStream mOut;
    private PrintStream mOutP;
    private MHFSLogger mLogArea;
    private JProgressBar mProgress = new JProgressBar(0, 100);
    private JLabel mLabel = new JLabel("Nothing");
    private JPopupMenu mPopupMenu = new JPopupMenu();
    private JMenuItem mItem;
    private HFSMonitor mMonitor;
    private Hashtable<String, String> mRequests;

    private boolean running = true;

    public NetworkInstance(Socket s, MHFSLogger logArea) {
        this.mSocket = s;
        this.mLogArea = logArea;
        try {
            mIn = new DataInputStream(s.getInputStream());
            // in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            mOut = s.getOutputStream();
            mOutP = new PrintStream(s.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mItem = new JMenuItem("Close transfer");
        mItem.addActionListener(this);
        mPopupMenu.add(mItem);
        mLabel.addMouseListener(this);

        updateLabel("Listing " + " ( " + s.getInetAddress().getHostAddress()
                + " )");
        mProgress.setStringPainted(true);
    }

    private void updateLabel(String text){
        mLabel.setText(text);
        mItem.setText("Close transfer " + mLabel.getText().substring(0, Math.min(mLabel.getText().length(), 20)) + "...");
    }

    public void setMonitor(HFSMonitor monitor) {
        this.mMonitor = monitor;
    }

    private String readLine() throws IOException {
        return mIn.readLine();
    }

    @Override
    public void run() {
        try {

            String req = readLine();// in.readLine();
            String line = null;

            // loop through and save request lines
            mRequests = new Hashtable<String, String>();
            line = req;
            System.out.println(line);

            line = readLine();// in.readLine();
            while (line.length() > 0) {
                String[] splits = line.split(": ");
                System.out.println(line);
                if (splits.length == 2) {
                    mRequests.put(splits[0], splits[1]);
                } else {
                    System.err
                            .println("Error, Request had non \": \" separator");
                }
                line = readLine();// in.readLine();
            }

            String[] strings = req.split(" ");
            if (strings[0].toLowerCase().equals("post")) {
                NetworkInstance.HTMLBegin(mOutP);
                if (receiveFile()) {
                    mOutP
                            .println("File upload success!<br/><a href=\"/\">Back to file listing.</a>");
                } else {
                    mOutP
                            .println("File upload failure :/ File probably already exists. (Cannot overwrite)<br/><a href=\"/\">Back to file listing.</a>");
                }
                NetworkInstance.HTMLEnd(mOutP);
            }
            // GET stuff
            // GET /
            else if (strings[1].length() == 0 || strings[1].equals("/")) {
                if (mMonitor.isAlternateHTML()) {
                    mLogArea.append("Alternate HTML List Sent. ( "
                            + mSocket.getInetAddress().getHostAddress() + " )\n");
                    mOutP.print("HTTP/1.0 200 OK\r\n" + "Content-Type: "
                            + guessContentType(".html") + "\r\n\r\n");
                    mOutP.println(mMonitor.getAlternateHTML());
                } else {
                    DirList d = mMonitor.getDirList();
                    if (d.isCustomList())
                        printDirList(d, "");
                    else
                        printDirList2(/* monitor.getDirList(), */"");
                }
                mProgress.setValue(100);
            }
            // GET File
            else {
                mProgress.setValue(0);
                String[] fname = strings[1].split("/");
                String append = "";

                File f = null;
                DirList d = mMonitor.getDirList();
                for (int i = 1; i < fname.length; i++) {
                    DirList temp = d.lookupDir(URLDecoder.decode(fname[i],
                            Charset.defaultCharset().toString()));
                    if (temp != null) {
                        d = temp;
                        append += URLDecoder.decode(fname[i], Charset
                                .defaultCharset().toString())
                                + "/";
                    }
                }

                if (d.isCustomList())
                    f = d.lookup(URLDecoder.decode(fname[fname.length - 1],
                            Charset.defaultCharset().toString()));
                else
                    f = getFile(strings[1]);

                Plugin p = mMonitor.getPluginManager().getPluginByIdentifier(
                        URLDecoder.decode(fname[fname.length - 1], Charset
                                .defaultCharset().toString()));
                System.out.println("Plugin: " + p);

                if (p != null) {
                    p.runPlugin(mSocket);
                } else if (f != null) {
                    sendFile(f); // send raw file
                } else if (d.isCustomList()) {
                    printDirList(d, append);
                } else {
                    printDirList2(strings[1]);
                    /*
                     * } else { // not reachable !
                     * NetworkInstance.HTMLBegin(outP); outP.println("NOT
                     * ALLOWED!<br>"); NetworkInstance.HTMLEnd(outP);
                     */}
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                mOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("CLOSING!");
        running = false;
        mMonitor.removeNetworkInstance(this);
    }

    public JLabel getNameLabel() {
        return mLabel;
    }

    /*
     * append has appended \\ already
     */
    private void printDirList(DirList dirList, String append) {
        mLogArea.append("File List Sent. ( "
                + mSocket.getInetAddress().getHostAddress() + " )\n");
        NetworkInstance.HTMLBegin(mOutP);
        mOutP.println("<table><tr>");

        // dirList.traverse();

        String[] dirs = dirList.getDirDirStrings();
        String[] files = dirList.getDirStrings();

        String[] upDirSplit = append.split("/");
        String upDir = "";

        for (int i = 0; i < upDirSplit.length - 1; i++) {
            upDir += upDirSplit[i] + "/";
        }

        if (dirList != mMonitor.getDirList())
            mOutP.println("<td align=\"right\" class=\"bread\">[ dir ] </td>"
                    + "<td class=\"bread\"><a href=\"/" + upDir + "\">.."
                    + "</a></td><tr>");

        for (String d : dirs) {
            mOutP.println("<td align=\"right\" class=\"bread\">[ dir ] </td>"
                    + "<td class=\"bread\"><a href=\"/" + append + d + "\">"
                    + d + "</a></td><tr>");
        }

        for (String s : files) {
            mOutP.println("<td align=\"right\" class=\"bread\">[ "
                    + fixDecimals((double) dirList.lookup(s).length() / 1024.0)
                    + " kB ] </td><td class=\"bread\"><a href=\"/" + append + s
                    + "\">" + s + "</a></td><tr>");
        }
        mOutP.println("</tr></table>");
        NetworkInstance.HTMLEnd(mOutP);

    }

    private File getFile(String place) {
        FSList fs = mMonitor.getFSList();
        File[] files = fs.getFiles(place);
        if (fs.isDir(place))
            return null;
        if (files != null && files.length > 0)
            return files[0];
        return null;
    }

    private void printDirList2(String place) {
        System.out.println("PLACE: " + place);
        FSList fs = mMonitor.getFSList();

        mLogArea.append("File List Sent. ( "
                + mSocket.getInetAddress().getHostAddress() + " )\n");
        NetworkInstance.HTMLBegin(mOutP);
        mOutP.println("<table><tr>");

        File[] dirs = fs.getDirs(place);
        File[] files = fs.getFiles(place);

        String[] upDirSplit = place.split("/");
        String upDir = "";

        for (int i = 0; i < upDirSplit.length - 1; i++) {
            upDir += upDirSplit[i] + "/";
        }

        if (!fs.isBaseDirectory(place))
            mOutP.println("<td align=\"right\" class=\"bread\">[ dir ] </td>"
                    + "<td class=\"bread\"><a href=\"" + upDir + "\">.."
                    + "</a></td><tr>");

        for (File d : dirs) {
            mOutP.println("<td align=\"right\" class=\"bread\">[ dir ] </td>"
                    + "<td class=\"bread\"><a href=\"" + place + "/"
                    + d.getName() + "\">" + d.getName() + "</a></td><tr>");
        }

        for (File s : files) {
            mOutP.println("<td align=\"right\" class=\"bread\">[ "
                    + fixDecimals((double) s.length() / 1024.0)
                    + " kB ] </td><td class=\"bread\"><a href=\"" + place + "/"
                    + s.getName() + "\">" + s.getName() + "</a></td><tr>");
        }
        mOutP.println("</tr></table>");
        NetworkInstance.HTMLEnd(mOutP);

    }

    public static void HTMLBegin(PrintStream outP) {
        outP.print(
                "HTTP/1.0 200 OK\r\n" + "Content-Type: " + guessContentType(".html") + "\r\n"
                + "Cache-Control: no-cache"
                + "\r\n\r\n");
        outP
                .println("<html><head><title>HFS/minimal</title><style type=\"text/css\">");
        outP.println("<!--");
        outP
                .println(".headline { font-size: 16px; color: black; line-height: normal; font-style: normal; font-family: Arial; font-variant: normal; font-weight: bold;}");
        outP
                .println(".bread { font-size: 12px; color: black; line-height: normal; font-style: normal; font-family: Arial; font-variant: normal;}");
        outP.println("-->");
        outP.println("</style></head><body>");
        outP.println("<p class=\"headline\">HFS/Minimal</p>");
        outP
                .println("<form action=\"/\" method=\"post\" enctype=\"multipart/form-data\"><input name=\"zipfile\" type=\"file\" /><br /><input type=\"submit\" name=\"submit\" value=\"Submit File\"></form>");
        outP.println("<p class=\"bread\">");
    }

    public static void HTMLEnd(PrintStream outP) {
        outP
                .println("</p>"
                        + "</body></html>");
    }

    private static String guessContentType(String path) {
        path = path.toLowerCase();
        if (path.endsWith(".html") || path.endsWith(".htm")) {
            return "text/html";
        } else if (path.endsWith(".txt") || path.endsWith(".java")) {
            return "text/plain";
        } else if (path.endsWith(".gif")) {
            return "image/gif";
        } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            return "image/jpeg";
        } else {
            return "application/octet-stream";
        }
    }

    private boolean receiveFile() throws InterruptedException, IOException {
        int buffSize = 1024;
        try {
            buffSize = mSocket.getReceiveBufferSize();
        } catch (SocketException e) {
            e.printStackTrace();
            return false;
        }

        // h�r efter �r in metodens in.
        //DataInputStream din = new DataInputStream(s.getInputStream());

        mProgress.setValue(0);
        // HTTP POST
        System.out.println("post!");

        // get the boundary
        String boundary = mRequests.get("Content-Type").split("boundary=")[1];
        long contentLength = Long.parseLong(mRequests.get("Content-Length"));

        long countFactor = getProgressBarFactor(contentLength);
        long max = contentLength / countFactor;

        mProgress.setMaximum((int) max);

        String preBound = "--" + boundary;
        // String postBound = "--" + boundary + "--";

        long contentLengthLeft = contentLength;
        // lets do this!
        String line = readLine();// in.readLine();
        contentLengthLeft -= line.length() + 4;
        while (line.equals(preBound)) {
            Hashtable<String, String> info = new Hashtable<String, String>();
            line = readLine();// in.readLine();
            contentLengthLeft -= line.length() + 4;
            while (line.length() > 0) {
                System.out.println("line: " + line);
                String[] splits = line.split(": ");
                if (splits.length == 2) {
                    info.put(splits[0], splits[1]);
                } else {
                    System.err
                            .println("Error, Request had non \": \" separator");
                }
                line = readLine();// in.readLine();
                contentLengthLeft -= line.length() + 4;
            }

            // h�r har vi f�tt ut "infon"
            String contentDisposition = info.get("Content-Disposition");
            if (contentDisposition != null) {
                String[] split = contentDisposition.split("filename=");
                String filename = "";
                if (split.length == 2) {
                    String[] split2 = split[1].replaceAll("\"", "").split(
                            "\\\\");
                    filename = split2[split2.length - 1];
                    File outF = new File(mMonitor.getUploadDir() + "\\"
                            + filename);
                    if (outF.exists()) {
                        mLogArea.append("File receiving aborted: " + filename
                                + " already exists ( "
                                + mSocket.getInetAddress().getHostAddress() + " )\n");
                        updateLabel("[R] File exists, " + filename + " / "
                                + fixDecimals(contentLength / 1024.0) + " kB");
                        // Funkar s�d�r va
                        // progress.setMaximum((int)contentLength);
                        // while(contentLengthLeft > 0 && running){
                        // contentLengthLeft -= in.skip(in.available());
                        // if(in.read() != -1);
                        // contentLengthLeft--;
                        // progress.setValue((int)(contentLength-contentLengthLeft));
                        //
                        // }
                        return false;
                    }
                    OutputStream outSF = new FileOutputStream(outF);

                    updateLabel("[R]" + filename + " ( "
                            + mSocket.getInetAddress().getHostAddress() + " )");
                    mLogArea.append("File receiving started: " + filename + "( "
                            + mSocket.getInetAddress().getHostAddress() + " )\n");

                    byte[] buffer = new byte[buffSize];
                    // char[] c = new char[buffSize];
                    long timeNanos = System.nanoTime() - 1000000000;
                    long lastSize = 0;

                    double tooMuch = 0;
                    while (contentLengthLeft > 0 && running) {

                        long nanosBefore = System.nanoTime();
                        int readBytes = mIn.read(buffer);

                        outSF.write(buffer, 0, readBytes);

                        contentLengthLeft -= readBytes;
                        mProgress
                                .setValue((int) ((contentLength - contentLengthLeft)/countFactor));

                        final long timeDiffNanos = nanosBefore - timeNanos;
                        if (timeDiffNanos > 1000000000L) {
                            double dBw = ((double) ((contentLength - contentLengthLeft) - lastSize))
                                    / (1000L * (timeDiffNanos/1000000000L));
                            mProgress
                                    .setString(fixDecimals((double) ((double) (contentLength - contentLengthLeft) / (double) contentLength) * 100)
                                            + "% / "
                                            + fixDecimals(dBw)
                                            + " kB/s");
                            timeNanos = System.nanoTime();
                            lastSize = (contentLength - contentLengthLeft);
                        }

                        long nanosAfter = System.nanoTime();

                        // current share
                        double currShare = mMonitor.getShare() * 0.000000001; // kB/ns
                        tooMuch = bandWidthLimit(tooMuch, currShare, nanosAfter - nanosBefore,
                                calcBandwidthUsed(nanosAfter - nanosBefore,
                                        buffSize));
                    }
                    outSF.close();

                    if (!running)
                        return false;

                    // nu fixar vi filhelvetet
                    RandomAccessFile raf = new RandomAccessFile(outF, "rw");
                    // detta �r bajs
                    if (raf.length() > 1024) {
                        raf.seek(raf.length() - 1024);
                    }

                    long removeBytes = 0;
                    // fult
                    while (!raf.readLine().equals(preBound))
                        ;
                    removeBytes = preBound.length() + 4;
                    try {
                        while (true) {
                            raf.readByte();
                            removeBytes++;
                        }
                    } catch (EOFException eofe) {
                        raf.setLength(raf.length() - removeBytes);
                        raf.close();
                    }

                    mLogArea.append("File receiving finished: " + filename
                            + "( " + mSocket.getInetAddress().getHostAddress()
                            + " )\n");
                    return true;
                }
            }
            line = readLine();// in.readLine();
        }
        return true;
    }

    private void sendFile(File f) throws InterruptedException {
        int buffSize = 1024;
        try {
            buffSize = mSocket.getSendBufferSize();
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }

        long contentLength = f.length();

        long countFactor = getProgressBarFactor(contentLength);
        long max = contentLength / countFactor;

        mProgress.setMaximum((int) max);
        // send file
        InputStream inputStream = null;
        long counter = 0;
        final byte[] buffer = new byte[buffSize];
        int result = 0;
        try {
            inputStream = new FileInputStream(f);

            System.out.println("avail " + inputStream.available());


            String header = "HTTP/1.0 200 OK\r\n" + "Content-Type: "
                    + guessContentType(f.toString()) + "\r\n"
                    + "Content-length: " + contentLength + "\r\n"
                    + "Content-Disposition: filename=\"" + f.getName() + "\""
                    + "\r\n\r\n";

            System.out.println(header);

            mOutP.print(header);
            mOutP.flush();

            updateLabel(f.getName() + " ( "
                    + mSocket.getInetAddress().getHostAddress() + " )");
            mLogArea.append("File sending started: " + f.getName() + "( "
                    + mSocket.getInetAddress().getHostAddress() + " )\n");

            counter = 0;
            long timeNanos = System.nanoTime() - 1000000000;
            long lastSize = 0;

            double tooMuch = 0;
            result = inputStream.read(buffer);
            while (result > 0 && running) {

                long nanosBefore = System.nanoTime();
                mOut.write(buffer, 0, result);

                final long timeDiffNanos = nanosBefore - timeNanos;
                if (timeDiffNanos > 1000000000L) {
                    double dBw = ((double) (counter - lastSize)) / (1000L * (timeDiffNanos/1000000000L));
                    mProgress
                            .setString(fixDecimals((double) ((double) counter / (double) (contentLength)) * 100)
                                    + "% / " + fixDecimals(dBw) + " kB/s");
                    timeNanos = System.nanoTime();
                    lastSize = counter;
                }
                counter += result;
                mProgress.setValue((int) (counter / countFactor));

                long nanosAfter = System.nanoTime();
                // double secondsDiff = calcSecondsDiff(nanosBefore,
                // nanosAfter);

                double currShare = mMonitor.getShare() * 0.000000001; // kB/ns
                tooMuch = bandWidthLimit(tooMuch, currShare, nanosAfter - nanosBefore, calcBandwidthUsed(
                        nanosAfter - nanosBefore, buffSize));

                result = inputStream.read(buffer);
            }
            // If we made it through
            counter = contentLength;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("was going to write " + result + " bytes + " + counter);
            System.out
                    .println("Exception in SendFile, probable cause: user aborted.");
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        mLogArea
                .append("File sending finished: " + f.getName() + "( "
                        + mSocket.getInetAddress().getHostAddress() + " ) "
                        + (double) ((double) counter / (double) contentLength) * 100
                        + "%\n");

    }

    private static long getProgressBarFactor(long contentLength) {
        long max = contentLength;
        long countFactor = 1;
        while (max > Integer.MAX_VALUE) {
            countFactor *= 1000;
            max = contentLength / countFactor;
        }
        return countFactor;
    }

    private static double calcBandwidthUsed(long nanoSecondsDiff, int buffSize) {
        return ((double) buffSize / 1024.0) / ((double) nanoSecondsDiff); // kB/ns
    }

    // v2
    /**
     * The return value must be fed back into the function as the tooMuch
     * parameter, when using continuously in a loop
     */
    private static double bandWidthLimit(double tooMuch, double currShare, long nanoSecondsDiff, double shareUsed) // kB/ns
            throws InterruptedException {
        if (currShare > 0 && shareUsed > currShare) {
            tooMuch += ((shareUsed - currShare) / currShare) * nanoSecondsDiff;

            if (tooMuch > 1000000000.0) {
                int nanoSleep = (int) (tooMuch % 999999);
                long milliSleep = (long) ((tooMuch - nanoSleep) * 0.000001);

                Thread.sleep(milliSleep, nanoSleep);
                tooMuch = 0;
            }
        }
        return tooMuch;
    }

    private static String fixDecimals(double val) {
        String value = Double.toString(val);
        String[] split = value.split("\\.");
        return split[0] + "." + split[1].substring(0, 1);
    }

    public JProgressBar getProgressBar() {
        return mProgress;
    }

    public void stopNetworkInstance() {
        running = false;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        System.out.println(e.getSource());
        if(e.getSource() == mLabel){
            mPopupMenu.show(mLabel, e.getX(), e.getY());
        }
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
    }

    @Override
    public void mouseExited(MouseEvent arg0) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent arg0) {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == mItem){
            running = false;
            try {
                mSocket.close();
            } catch (IOException e1) {
                System.out.println("Exception in NetworkInstance.mouseClicked() probable cause: operator shut down transfer");
            }
        }

    }
}
