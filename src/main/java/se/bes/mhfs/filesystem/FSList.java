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
import java.io.FileFilter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Observable;

public class FSList extends Observable {
    private String baseDir;

    public FSList(String baseDir) {
        this.baseDir = baseDir;
    }
    
    public boolean isDir(String place){
        try {
            place = URLDecoder.decode(place, Charset.defaultCharset().toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        File f = new File(baseDir + "/" + place);
        return f.isDirectory();
    }

    public File[] getFiles(String dirString) {
        try {
            dirString = URLDecoder.decode(dirString, Charset.defaultCharset().toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        File dir = new File(baseDir + "/" + dirString);
        if (!dir.getAbsolutePath().contains(baseDir))
            return (new File[] { new File(baseDir) });
        
        if(dir.isFile())
            return (new File[] { dir });

        File[] ff = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                if (pathname.isFile())
                    return true;
                return false;
            }
        });

        return ff;
    }

    public File[] getDirs(String dirString) {
        try {
            dirString = URLDecoder.decode(dirString, Charset.defaultCharset().toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        File dir = new File(baseDir + "/" + dirString);
        if (!dir.getAbsolutePath().contains(baseDir))
            return (new File[] { new File(baseDir) });

        File[] fd = dir.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                if (pathname.isDirectory())
                    return true;
                return false;
            }

        });

        return fd;
    }
    
    public boolean isBaseDirectory(String isBase){
        try {
            isBase = URLDecoder.decode(isBase, Charset.defaultCharset().toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        File isBaseF = new File(baseDir + "/" + isBase);
        File baseDirF = new File(baseDir);
        
        return isBaseF.getAbsolutePath().equals(baseDirF.getAbsolutePath());
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
        setChanged();
        UpdateEvent evt = new UpdateEvent();
        evt.addEvent("settings", true);
        evt.addEvent("sharedFiles", true);
        notifyObservers(evt);
    }
    
    public String getBaseDir(){
        return baseDir;
    }
}
