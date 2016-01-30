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

package se.bes.mhfs.manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class SettingsManager {
    private final File saveFile = new File("hfs.settings");
    private HFSMonitor monitor;

    public SettingsManager(HFSMonitor monitor) {
        this.monitor = monitor;
    }

    public void saveSettings() {
        PrintWriter buffer;

        try {
            final ImmutableSettings inMemory = monitor.getInMemory();

            System.out.println("Saving to " + saveFile.getAbsolutePath());
            buffer = new PrintWriter(new FileOutputStream(saveFile));
            buffer.println("Port::" + inMemory.port);
            buffer.println("Speed::" + inMemory.speed);
            buffer.println("Directory::" + inMemory.baseDir);
            buffer.println("UploadDirectory::" + inMemory.uploadDir);
            buffer.println("UPnPInterface::" + inMemory.upnpIp);
            buffer.println("UPnPDevice::" + inMemory.upnpDevice);
            if (monitor.isCustomList()) {
                File[] files = monitor.getCustomDirList().getDirFiles();
                for (File f : files) {
                    buffer.println("CustomFile::" + f.getAbsolutePath());
                }
            }
            buffer.println();
            buffer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            monitor.inMemoryToSettings();
        }
    }

    public void loadSettings() {
        BufferedReader buffer;

        try {
            buffer = new BufferedReader(new FileReader(saveFile));

            String line = buffer.readLine();
            while (line != null) {
                String[] split = line.split("::");

                switch (split[0]) {
                    case "Port":
                        try {
                            monitor.setPort(Integer.parseInt(split[1]));
                        } catch (NumberFormatException ignore) {
                            monitor.setPort(8080);
                        }
                        break;
                    case "Speed":
                        try {
                            monitor.setSpeed(Integer.parseInt(split[1]));
                        } catch (NumberFormatException ignore) {
                            monitor.setSpeed(0);
                        }
                        break;
                    case "Directory":
                        monitor.setBaseDir(split[1]);
                        break;
                    case "UploadDirectory":
                        monitor.setUploadDir(split[1]);
                        break;
                    case "CustomFile":
                        monitor.addCustomFile(new File(split[1]));
                        break;
                    case "UPnPInterface":
                        monitor.setUpnpIp(split[1]);
                        break;
                    case "UPnPDevice":
                        monitor.setUpnpDevice(split[1]);
                        break;
                }

                line = buffer.readLine();
            }

            buffer.close();
        } catch (FileNotFoundException e) {
            System.err.println("hfs.settings does not exist, not a problem.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            monitor.inMemoryToSettings();
        }
    }
}
