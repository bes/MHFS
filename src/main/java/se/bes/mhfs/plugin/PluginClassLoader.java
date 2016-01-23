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

import java.io.FileInputStream;

public class PluginClassLoader extends ClassLoader{
    public PluginClassLoader(){
        super(PluginClassLoader.class.getClassLoader());
    }
    
    public byte[] findClassBytes(String className){

        try{
            String pathName = "plugins/" + className;
            FileInputStream inFile = new
                FileInputStream(pathName);
            byte[] classBytes = new
                byte[inFile.available()];
            inFile.read(classBytes);
            return classBytes;
        }
        catch (java.io.IOException ioEx){
            return null;
        }
    }

    public Class<?> findClass(String name)throws
        ClassNotFoundException{

        byte[] classBytes = findClassBytes(name);
        if (classBytes==null){
            throw new ClassNotFoundException();
        }
        else{
            return defineClass("plugin." + name.replace(".hfsplugin", ""), classBytes,
                0, classBytes.length);
        }
    }

    public Class findClass(String name, byte[]
        classBytes)throws ClassNotFoundException{

        if (classBytes==null){
            throw new ClassNotFoundException(
                "(classBytes==null)");
        }
        else{
            return defineClass(name, classBytes,
                0, classBytes.length);
        }
    }
/*
    public void execute(String codeName,
        byte[] code){

        Class klass = null;
        try{
            klass = findClass(codeName, code);
            TaskIntf task = (TaskIntf)
                klass.newInstance();
            task.execute();
        }
        catch(Exception exception){
            exception.printStackTrace();
        }
    }*/
}
