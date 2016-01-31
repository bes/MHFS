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
import se.bes.mhfs.network.NetworkInstance;

import java.io.PrintStream;
import java.net.Socket;

public class FirstPlugin extends Plugin{

    private static final long serialVersionUID = 1L;
    protected PrintStream printStream;

    public FirstPlugin(HFSMonitor m){
        super(m, "My First Plugin");
    }

    public void runPlugin(Socket s) {
        try {
            printStream = new PrintStream(s.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
        NetworkInstance.HTMLBegin(printStream);
        printStream.println("Nice Test");
        NetworkInstance.HTMLEnd(printStream);
    }

    @Override
    public void updateGfx() {
    }

}
