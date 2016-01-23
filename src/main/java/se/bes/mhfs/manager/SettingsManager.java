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
    
    public SettingsManager(HFSMonitor monitor){
        this.monitor = monitor;
    }
    
    public void saveSettings(){
        PrintWriter buffer;
        
        try {
            buffer = new PrintWriter(new FileOutputStream(saveFile));
            buffer.println("Port::" + monitor.getPort());
            buffer.println("Speed::" + monitor.getSpeed());
            buffer.println("Directory::" + monitor.getFSList().getBaseDir());
            buffer.println("UploadDirectory::" + monitor.getUploadDir());
            if(monitor.getDirList().isCustomList()){
                File [] files = monitor.getDirList().getDirFiles();
                for(File f : files){
                    buffer.println("CustomFile::" + f.getAbsolutePath());
                }
            }
            buffer.println();
            buffer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    public void loadSettings(){
        BufferedReader buffer;
        
        try {
            buffer = new BufferedReader(new FileReader(saveFile));
            
            String line = buffer.readLine();
            while(line != null){
                String [] split = line.split("::");
                
                if(split[0].equals("Port")){
                    monitor.setPort(Integer.parseInt(split[1]));
                }else if(split[0].equals("Speed")){
                    monitor.setSpeed(Double.parseDouble(split[1]));
                }else if(split[0].equals("Directory")){
                    monitor.getFSList().setBaseDir(split[1]);
                }else if(split[0].equals("UploadDirectory")){
                    monitor.setUploadDir(split[1]);
                }else if(split[0].equals("CustomFile")){
                    monitor.getDirList().addCustom(new File(split[1]));
                }
                
                line = buffer.readLine();
            }
            
            buffer.close();
        } catch (FileNotFoundException e) {
            System.err.println("hfs.settings does not exist, not a problem.");
        } catch (IOException e){
            e.printStackTrace();
        }
        
    }
}
