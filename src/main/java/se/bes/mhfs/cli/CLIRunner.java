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

package se.bes.mhfs.cli;

import se.bes.mhfs.logger.CLILogger;
import se.bes.mhfs.manager.HFSMonitor;
import se.bes.mhfs.manager.SettingsManager;
import se.bes.mhfs.network.Network;

public class CLIRunner {
    private Network n;
    private HFSMonitor monitor;
    
    public CLIRunner(){
        CLILogger logger = new CLILogger();
        monitor = new HFSMonitor(logger);
        new SettingsManager(monitor).loadSettings();

        n = new Network(logger, monitor);

        n.start();
        
        monitor.setNetwork(true);
        
        printSettings();
    }
    
    public CLIRunner(int port, int speed){
        CLILogger logger = new CLILogger();
        monitor = new HFSMonitor(logger);
        new SettingsManager(monitor).loadSettings();

        monitor.setSpeed(speed);
        monitor.setPort(port);

        n = new Network(logger, monitor);

        n.start();
        
        monitor.setNetwork(true);
        printSettings();
    }
    
    public void printSettings(){
        System.out.println("Port: " + monitor.getInMemory().port);
        System.out.println("Max speed: " + monitor.getInMemory().speed + "kB/s (0 = unlimited)");
        System.out.println("Upload dir: " + monitor.getInMemory().uploadDir);
    }
}