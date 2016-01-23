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

import se.bes.mhfs.events.UpdateEvent;
import se.bes.mhfs.manager.HFSMonitor;
import se.datadosen.component.RiverLayout;

import java.awt.*;
import java.net.Socket;
import java.util.Observable;
import java.util.Observer;

public abstract class Plugin extends Container implements Observer{
    private String identifier;
    private String name;
    private Socket s;
    protected final HFSMonitor monitor;
    private static final long serialVersionUID = 1L;

    public Plugin(HFSMonitor monitor, String name, String identifier){
        this.identifier = identifier;
        this.name = name;
        this.monitor = monitor;
        monitor.getPluginManager().registerPlugin(this);

        setLayout(new RiverLayout());
        monitor.addObserver(this);
    }
    
    public abstract void updateGfx();

    public void update(Observable o, Object arg) {
        if (((UpdateEvent) arg).getEvent(identifier))
            updateGfx();
    }
    
    public abstract void runPlugin(Socket s);

    
    public Socket getSocket(){
        return s;
    }
    
    public String getIdentifier(){
        return identifier;
    }
    
    public String getName(){
        return name;
    }
}
