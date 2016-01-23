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

import se.bes.mhfs.events.UpdateEvent;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Observable;

public class DirList extends Observable {
    private Hashtable<String, File> files;
    private Hashtable<String, DirList> directories;
    private boolean customList = false;
    private final static ArraysComp comparator = new ArraysComp();
//    private String baseDir;

    public DirList(/*String baseDir*/) {
        files = new Hashtable<String, File>();
        directories = new Hashtable<String, DirList>();

//        File dir = new File(baseDir);
//        this.baseDir = dir.getAbsolutePath();

        //traverse();
    }

//    public String getBaseDir() {
//        return baseDir;
//    }

//    public void setBaseDir(String baseDir) {
//        this.baseDir = baseDir;
//        traverse();
//        setChanged();
//        UpdateEvent evt = new UpdateEvent();
//        evt.addEvent("settings", true);
//        evt.addEvent("sharedFiles", true);
//        notifyObservers(evt);
//    }

    public synchronized File[] getDirFiles() {
        //traverse();
        File[] filesA = new File[files.size()];
        files.values().toArray(filesA);
        return filesA;
    }

    public synchronized String[] getDirStrings() {
        //traverse();
        
        String[] strings = new String[files.size()];
        files.keySet().toArray(strings);
        Arrays.sort(strings, 0, strings.length, DirList.comparator);
        
        return strings;
    }
    
    public synchronized File[] getDirDirFiles() {
        //traverse();
        File[] filesA = new File[directories.size()];
        directories.values().toArray(filesA);
        return filesA;
    }
    
    
    public synchronized String[] getDirDirStrings(){
        String[] dirs = new String[directories.size()];
        directories.keySet().toArray(dirs);
        Arrays.sort(dirs, 0, dirs.length, DirList.comparator);

        return dirs;
    }

    public synchronized File lookup(String s) {
        return files.get(s);
    }
    
    public synchronized DirList lookupDir(String d){
        return directories.get(d);
    }

    public synchronized void addCustom(File file) {
        if (!customList){
            files.clear();
            directories.clear();
        }
        int newNum = 1;
        String name = file.getName();
        while (files.get(name) != null) {
            name = "[" + newNum + "]" + file.getName();
            newNum++;
        }
        files.put(name, file);
        customList = true;

        setChanged();
        UpdateEvent evt = new UpdateEvent();
        evt.addEvent("settings", true);
        evt.addEvent("sharedFiles", true);
        evt.addEvent("customFiles", true);
        notifyObservers(evt);
    }

    public synchronized void removeCustom(String name) {
        files.remove(name);
        if (files.size() == 0) {
            customList = false;
//            traverse();
        }

        setChanged();
        UpdateEvent evt = new UpdateEvent();
        evt.addEvent("settings", true);
        evt.addEvent("sharedFiles", true);
        evt.addEvent("customFiles", true);
        notifyObservers(evt);
    }

    public boolean isCustomList() {
        return customList;
    }

//    public synchronized void traverse() {
//        if (!customList) {
//            files.clear();
//            directories.clear();
//            File dir = new File(baseDir);
//            String[] children = dir.list();
//            if (children != null) {
//                for (int i = 0; i < children.length; i++) {
//                    File inf = new File(baseDir + "\\" + children[i]);
//                    if (inf.isFile()) {
//                        files.put(inf.getName(), inf);
//                    } else if (inf.isDirectory() && !inf.getName().equals("..")
//                            && !inf.getName().equals(".")) {
//                        directories.put(inf.getName(), new DirList(inf
//                                .getAbsolutePath()));
//                    }
//                }
//            }
//        }
//    }

    public synchronized boolean contains(String fname) {
        return files.contains(fname);
    }

    private static class ArraysComp implements Comparator<String> {

        public int compare(String o1, String o2) {
            return o1.toLowerCase().compareTo(o2.toLowerCase());
        }
    }
}
