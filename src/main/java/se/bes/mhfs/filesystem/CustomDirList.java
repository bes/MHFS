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

package se.bes.mhfs.filesystem;

import se.bes.mhfs.manager.HFSMonitor;

import java.io.File;
import java.util.*;

public class CustomDirList extends Observable {
    private final static ArraysComp comparator = new ArraysComp();
    private final HFSMonitor monitor;

    public CustomDirList(HFSMonitor monitor) {
        this.monitor = monitor;
    }

    public synchronized File[] getDirFiles() {
        Map<String, File> customFiles = monitor.getInMemory().customFiles;
        File[] filesA = new File[customFiles.size()];
        customFiles.values().toArray(filesA);
        return filesA;
    }

    public synchronized String[] getDirStrings() {
        Map<String, File> customFiles = monitor.getInMemory().customFiles;
        String[] strings = new String[customFiles.size()];
        customFiles.keySet().toArray(strings);
        Arrays.sort(strings, 0, strings.length, CustomDirList.comparator);
        return strings;
    }

    public synchronized File lookup(String s) {
        Map<String, File> customFiles = monitor.getInMemory().customFiles;
        return customFiles.get(s);
    }

    private static class ArraysComp implements Comparator<String> {
        public int compare(String o1, String o2) {
            return o1.toLowerCase().compareTo(o2.toLowerCase());
        }
    }
}
