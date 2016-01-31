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
import se.bes.mhfs.filesystem.CustomDirList;
import se.bes.mhfs.filesystem.FSList;
import se.bes.mhfs.logger.MHFSLogger;
import se.bes.mhfs.network.NetworkInstance;
import se.bes.mhfs.plugin.PluginManager;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

public class HFSMonitor extends Observable implements Observer {

    public static final String NO_UPNP_IP = "UPnP IP: None";
    public static final String NO_UPNP_DEVICE = "UPnP Device: None";

    private final LinkedList<NetworkInstance> instanceList;
    private final CustomDirList customDirList;
    private final FSList fsList;
    private final PluginManager plugins;
    private final MHFSLogger logger;

    // Don't save these to file
    private String upnpHostNoSave;
    private boolean gotNetwork = false;

    // Defaults
    private volatile ImmutableSettings inMemory = new ImmutableSettings.Builder()
            .setPort(8080)
            .setBaseDir(".")
            .setUploadDir(new File(".").getAbsolutePath())
            .setUpnpIp(NO_UPNP_IP)
            .setUpnpDevice(NO_UPNP_DEVICE)
            .setCustomFiles(new HashMap<>())
            .build();

    private volatile ImmutableSettings inSettings = inMemory;

    public HFSMonitor(MHFSLogger logger) {
        this.logger = logger;

        instanceList = new LinkedList<>();
        customDirList = new CustomDirList(this);
        customDirList.addObserver(this);

        fsList = new FSList(this);
        fsList.addObserver(this);
        
        plugins = new PluginManager(this);
    }

    public ImmutableSettings getInSettings() {
        return inSettings;
    }

    public ImmutableSettings getInMemory() {
        return inMemory;
    }

    public boolean needsSave() {
        return !inMemory.equals(inSettings);
    }

    void inMemoryToSettings() {
        inSettings = inMemory;
        sendNotification(UpdateEvent.Type.SETTINGS);
    }

    public void setUpnpIp(String upnpIp) {
        inMemory = inMemory.buildUpon().setUpnpIp(upnpIp).build();
        sendNotification(UpdateEvent.Type.SETTINGS);
    }

    public void setUpnpDevice(String upnpDevice) {
        inMemory = inMemory.buildUpon().setUpnpDevice(upnpDevice).build();
        sendNotification(UpdateEvent.Type.SETTINGS);
    }

    public void setUploadDir(String dir){
        inMemory = inMemory.buildUpon().setUploadDir(dir).build();
        sendNotification(UpdateEvent.Type.SETTINGS);
    }

    public void setPort(int port) {
        inMemory = inMemory.buildUpon().setPort(port).build();
        sendNotification(UpdateEvent.Type.SETTINGS, UpdateEvent.Type.IP_ADDRESS);
    }

    public void setBaseDir(String baseDir) {
        inMemory = inMemory.buildUpon().setBaseDir(baseDir).build();
        sendNotification(UpdateEvent.Type.SETTINGS, UpdateEvent.Type.SHARED_FILES);
    }

    public void setSpeed(int speed) {
        inMemory = inMemory.buildUpon().setSpeed(speed).build();
        sendNotification(UpdateEvent.Type.SETTINGS);
    }

    public synchronized void addCustomFile(File file) {
        int newNum = 1;
        String name = file.getName();

        while (inMemory.customFiles.get(name) != null) {
            name = "[" + newNum + "]" + file.getName();
            newNum++;
        }

        inMemory = inMemory.buildUpon().addCustomFile(name, file).build();

        setChanged();
        UpdateEvent evt = new UpdateEvent();
        evt.addEvent(UpdateEvent.Type.SETTINGS, UpdateEvent.Type.SHARED_FILES, UpdateEvent.Type.CUSTOM_FILES);
        notifyObservers(evt);
    }

    public synchronized void removeCustomFile(String name) {
        inMemory = inMemory.buildUpon().removeCustomFile(name).build();
        setChanged();
        UpdateEvent evt = new UpdateEvent();
        evt.addEvent(UpdateEvent.Type.SETTINGS, UpdateEvent.Type.SHARED_FILES, UpdateEvent.Type.CUSTOM_FILES);
        notifyObservers(evt);
    }

    /*
     *  <DON'T SAVE TO DISK>
     */

    public boolean isCustomList() {
        return inMemory.customFiles.size() > 0;
    }

    public String getUpnpHost() {
        return upnpHostNoSave;
    }

    public void setUpnpHost(String upnpHost) {
        this.upnpHostNoSave = upnpHost;
    }

    public PluginManager getPluginManager(){
        return plugins;
    }

    public void setupPlugins(){
        plugins.setupPlugins();
    }

    public CustomDirList getCustomDirList(){
        return customDirList;
    }

    public FSList getFSList(){
        return fsList;
    }

    public MHFSLogger getLogger() {
        return logger;
    }

    public synchronized void addNetworkInstance(NetworkInstance instance) {
        instance.start();
        instance.setMonitor(this);
        instanceList.add(instance);
        sendNotification(UpdateEvent.Type.PROGRESS);
    }

    public synchronized void removeNetworkInstance(NetworkInstance instance) {
        instanceList.remove(instance);
        sendNotification(UpdateEvent.Type.PROGRESS);
    }

    public synchronized void stopNet() {
        for (NetworkInstance ni : instanceList) {
            ni.interrupt();
            ni.stopNetworkInstance();
        }
    }

    public synchronized LinkedList<NetworkInstance> getNetworkInstances() {
        return instanceList;
    }

    public double getShare() {
        if (inMemory.speed > 0) {
            return inMemory.speed / instanceList.size();
        }
        return 0;
    }

    public void setNetwork(boolean net) {
        this.gotNetwork = net;
        sendNotification(UpdateEvent.Type.SETTINGS, UpdateEvent.Type.PROGRESS);
    }

    public boolean getNetwork() {
        return gotNetwork;
    }

    private void sendNotification(UpdateEvent.Type... types) {
        setChanged();
        UpdateEvent evt = new UpdateEvent();
        evt.addEvent(types);
        notifyObservers(evt);
    }

    @Override
    public void update(Observable o, Object arg) {
        setChanged();
        notifyObservers(arg);
    }

    /*
     *  </DON'T SAVE TO DISK>
     */
}
