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

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ImmutableSettings {
    public final int port;
    public final int speed;
    public final String baseDir;
    public final String uploadDir;
    public final String upnpIp;
    public final String upnpDevice;
    public final Map<String, File> customFiles;

    private ImmutableSettings(int port, int speed, String baseDir, String uploadDir, String upnpIp, String upnpDevice, HashMap<String, File> customFiles) {
        this.port = port;
        this.speed = speed;
        this.baseDir = baseDir;
        this.uploadDir = uploadDir;
        this.upnpIp = upnpIp;
        this.upnpDevice = upnpDevice;
        this.customFiles = Collections.unmodifiableMap(customFiles);
    }

    Builder buildUpon() {
        Builder b = new Builder();
        b.port = port;
        b.speed = speed;
        b.baseDir = baseDir;
        b.uploadDir = uploadDir;
        b.upnpIp = upnpIp;
        b.upnpDevice = upnpDevice;
        b.customFiles = new HashMap<>(customFiles);
        return b;
    }

    @Override
    public boolean equals(Object o) {
        // Generated using IntelliJ
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImmutableSettings that = (ImmutableSettings) o;

        if (port != that.port) return false;
        if (Integer.compare(that.speed, speed) != 0) return false;
        if (baseDir != null ? !baseDir.equals(that.baseDir) : that.baseDir != null) return false;
        if (uploadDir != null ? !uploadDir.equals(that.uploadDir) : that.uploadDir != null) return false;
        if (upnpIp != null ? !upnpIp.equals(that.upnpIp) : that.upnpIp != null) return false;
        if (customFiles != null) {
            if (that.customFiles != null) {
                if (customFiles.size() == that.customFiles.size()) {
                    for (Map.Entry<String, File> entry : customFiles.entrySet()) {
                        File otherFile = that.customFiles.get(entry.getKey());
                        File thisFile = entry.getValue();
                        if (thisFile == null || !thisFile.equals(otherFile)) {
                            return false;
                        }
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            if (that.customFiles != null) {
                return false;
            }
        }
        return upnpDevice != null ? upnpDevice.equals(that.upnpDevice) : that.upnpDevice == null;
    }

    static class Builder {
        private int port;
        private int speed;
        private String baseDir;
        private String uploadDir;
        private String upnpIp;
        private String upnpDevice;
        private HashMap<String, File> customFiles;

        Builder setPort(int port) {
            this.port = port;
            return this;
        }

        Builder setSpeed(int speed) {
            this.speed = speed;
            return this;
        }

        Builder setBaseDir(String baseDir) {
            this.baseDir = baseDir;
            return this;
        }

        Builder setUploadDir(String uploadDir) {
            this.uploadDir = uploadDir;
            return this;
        }

        Builder setUpnpIp(String upnpIp) {
            this.upnpIp = upnpIp;
            return this;
        }

        Builder setUpnpDevice(String upnpDevice) {
            this.upnpDevice = upnpDevice;
            return this;
        }

        Builder setCustomFiles(HashMap<String, File> customFiles) {
            this.customFiles = customFiles;
            return this;
        }

        Builder addCustomFile(String name, File file) {
            customFiles.put(name, file);
            return this;
        }

        Builder removeCustomFile(String name) {
            customFiles.remove(name);
            return this;
        }

        ImmutableSettings build() {
            return new ImmutableSettings(port, speed, baseDir, uploadDir, upnpIp, upnpDevice, customFiles);
        }
    }
}
