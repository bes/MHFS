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

import se.bes.mhfs.events.UpdateEvent;
import se.bes.mhfs.filesystem.DirList;
import se.bes.mhfs.filesystem.FSList;
import se.bes.mhfs.network.NetworkInstance;
import se.bes.mhfs.plugin.PluginManager;

import java.io.File;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

public class HFSMonitor extends Observable implements Observer{

    public static final String NO_UPNP_IP = "UPnP IP: None";
    public static final String NO_UPNP_DEVICE = "UPnP Device: None";

    private final LinkedList<NetworkInstance> instanceList;
    private final DirList dirList;
    private final FSList fsList;
    private final PluginManager plugins;
    
    private double speed = 0;
    private int port = 8080;
    private boolean alternateHTMLSet = false;
    private String alternateHTML = "";
    private boolean gotNetwork = false;
    private String uploadDir;

    private String upnpHostNoSave;

    public static class ImmutableSettings {
        public final String upnpIp;
        public final String upnpDevice;

        private ImmutableSettings(String upnpIp, String upnpDevice) {
            this.upnpIp = upnpIp;
            this.upnpDevice = upnpDevice;
        }
    }

    private ImmutableSettings inMemory = new ImmutableSettings(NO_UPNP_IP, NO_UPNP_DEVICE);

    private ImmutableSettings inSettings = new ImmutableSettings(NO_UPNP_IP, NO_UPNP_DEVICE);

    public HFSMonitor() {
        instanceList = new LinkedList<NetworkInstance>();
        dirList = new DirList();
        dirList.addObserver(this);
        
        fsList = new FSList(".");
        fsList.addObserver(this);
        
        plugins = new PluginManager(this);
        
        File dir = new File(".");
        uploadDir = dir.getAbsolutePath();
    }

    public ImmutableSettings getInSettings() {
        return inSettings;
    }

    public ImmutableSettings getInMemory() {
        return inMemory;
    }

    public void inMemoryToSettings() {
        inSettings = inMemory;
    }

    public void setUpnpIp(String upnpIp) {
        inMemory = new ImmutableSettings(upnpIp, inMemory.upnpDevice);
    }

    public void setUpnpDevice(String upnpDevice) {
        inMemory = new ImmutableSettings(inMemory.upnpIp, upnpDevice);
    }

    public String getUpnpHost() {
        return upnpHostNoSave;
    }

    public void setUpnpHost(String upnpHost) {
        this.upnpHostNoSave = upnpHost;
    }

    public void setupPlugins(){
        plugins.setupPlugins();
    }

    public int getPort() {
        return port;
    }

    public String getUploadDir(){
        return uploadDir;
    }
    
    public void setUploadDir(String dir){
        this.uploadDir = dir;
        setChanged();
        UpdateEvent evt = new UpdateEvent();
        evt.addEvent("settings", true);
        notifyObservers(evt);
    }
    
    public void setPort(int port) {
        this.port = port;
        setChanged();
        UpdateEvent evt = new UpdateEvent();
        evt.addEvent("settings", true);
        evt.addEvent("ipAddress", true);
        notifyObservers(evt);
    }
    
    public DirList getDirList(){
        return dirList;
    }
    
    public FSList getFSList(){
        return fsList;
    }
    
    public PluginManager getPluginManager(){
        return plugins;
    }

    public synchronized void addNetworkInstance(NetworkInstance instance) {
        instance.start();
        instance.setMonitor(this);
        instanceList.add(instance);
        setChanged();
        UpdateEvent evt = new UpdateEvent();
        evt.addEvent("progress", true);
        notifyObservers(evt);
    }

    public synchronized void removeNetworkInstance(NetworkInstance instance) {
        instanceList.remove(instance);
        setChanged();
        UpdateEvent evt = new UpdateEvent();
        evt.addEvent("progress", true);
        notifyObservers(evt);
    }

    public void setSpeed(double speed) {
        this.speed = speed;
        setChanged();
        UpdateEvent evt = new UpdateEvent();
        evt.addEvent("settings", true);
        notifyObservers(evt);
    }
    
    public double getSpeed(){
        return speed;
    }

    public synchronized LinkedList<NetworkInstance> getNetworkInstances() {
        return instanceList;
    }

    public double getShare() {
        if (speed > 0) {
            return speed / instanceList.size();
        }
        return 0;
    }

    public synchronized void stopNet() {
        for (NetworkInstance ni : instanceList) {
            ni.interrupt();
            ni.stopNetworkInstance();
        }
    }

    public void setAlternateHTML(String text) {
        alternateHTML = text;
        if (text.equals("")) {
            alternateHTMLSet = false;
        } else {
            alternateHTMLSet = true;
        }
    }

    public boolean isAlternateHTML() {
        return alternateHTMLSet;
    }

    public String getAlternateHTML() {
        return alternateHTML;
    }

    public void setNetwork(boolean net) {
        this.gotNetwork = net;
        setChanged();
        UpdateEvent evt = new UpdateEvent();
        evt.addEvent("settings", true);
        evt.addEvent("progress", true);
        notifyObservers(evt);
    }

    public boolean getNetwork() {
        return gotNetwork;
    }

    public void update(Observable o, Object arg) {
        setChanged();
        notifyObservers(arg);
    }
}
