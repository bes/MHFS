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

import java.io.File;
import java.io.FileFilter;
import java.util.Hashtable;

public class PluginManager {
    private Hashtable<String, Plugin> plugins;
    private HFSMonitor monitor;

    public PluginManager(HFSMonitor monitor) {
        this.monitor = monitor;
        plugins = new Hashtable<String, Plugin>();
    }

    public synchronized void registerPlugin(Plugin plugin) {
        plugins.put(plugin.getIdentifier(), plugin);
    }

    public synchronized Plugin getPluginByIdentifier(String identifier) {
        return plugins.get(identifier);
    }

    public synchronized String[] getPluginStrings() {
        String[] strings = new String[plugins.size()];
        plugins.keySet().toArray(strings);

        return strings;
    }

    public void setupPlugins() {
        File pluginDir = new File("./plugins/");

        if(pluginDir == null)
            return;
        
        File[] files = pluginDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().endsWith(".hfsplugin");
            }
        });
        
        if(files == null)
            return;
        
        PluginClassLoader pcl = new PluginClassLoader();
        for(File f:files){
            try {
                Class c = pcl.loadClass(f.getName());
                c.getConstructors()[0].newInstance((new Object[]{monitor}));
                System.out.println("Added Plugin: " + f.getName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
