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

package se.bes.mhfs.upnp;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.support.igd.PortMappingListener;
import org.fourthline.cling.support.model.PortMapping;

import java.util.ArrayList;
import java.util.HashMap;

public class UPnpServiceProxy {

    private static final HashMap<String, UpnpInfo> ACTIVE_SERVICES = new HashMap<String, UpnpInfo>();

    public static class UpnpInfo {
        public final UpnpService service;
        public final ArrayList<String> devices = new ArrayList<String>();
        public final ArrayList<String> messages = new ArrayList<String>();

        private volatile OnChange onChange;

        private UpnpInfo(UpnpService service) {
            this.service = service;
        }

        public void setOnChange(OnChange onChange) {
            this.onChange = onChange;
        }

        private void notifyOnChange() {
            OnChange ch = onChange;
            if (ch != null) {
                ch.onChange();
            }
        }

        public interface OnChange {
            void onChange();
        }
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                synchronized (ACTIVE_SERVICES) {
                    for (UpnpInfo info : ACTIVE_SERVICES.values()) {
                        info.service.shutdown();
                    }
                }
            }
        });
    }

    public static UpnpInfo getMapping(int port, String host) {
        synchronized (ACTIVE_SERVICES) {
            String identifier = host + ":" + port;
            return ACTIVE_SERVICES.get(identifier);
        }
    }

    public static UpnpInfo updateMapping(final int port, final String host) {
        synchronized (ACTIVE_SERVICES) {
            String identifier = host + ":" + port;

            UpnpInfo info = ACTIVE_SERVICES.get(identifier);
            if (info == null) {
                PortMapping desiredMapping = new PortMapping(
                        port,
                        host,
                        PortMapping.Protocol.TCP,
                        "My Port Mapping"
                );

                UpnpService upnpService =
                        new UpnpServiceImpl(
                                new PortMappingListener(desiredMapping) {
                                    @Override
                                    public synchronized void deviceAdded(Registry registry, Device device) {
                                        super.deviceAdded(registry, device);
                                        UpnpInfo info = getMapping(port, host);
                                        if (info != null) {
                                            if (!info.devices.contains(device.getDisplayString())) {
                                                info.devices.add(device.getDisplayString());
                                                info.notifyOnChange();
                                            }
                                        }
                                    }

                                    @Override
                                    public synchronized void deviceRemoved(Registry registry, Device device) {
                                        super.deviceRemoved(registry, device);
                                        UpnpInfo info = getMapping(port, host);
                                        if (info != null) {
                                            if (info.devices.remove(device.getDisplayString())) {
                                                info.notifyOnChange();
                                            }
                                        }
                                    }

                                    @Override
                                    protected void handleFailureMessage(String s) {
                                        super.handleFailureMessage(s);
                                        UpnpInfo info = getMapping(port, host);
                                        if (info != null) {
                                            if (!info.messages.contains(s)) {
                                                info.messages.add(s);
                                            }
                                            info.notifyOnChange();
                                        }
                                    }
                                }
                        );

                info = new UpnpInfo(upnpService);
                ACTIVE_SERVICES.put(identifier, info);

                upnpService.getControlPoint().search();
            }
            return info;
        }
    }

    public static void closeMapping(int port, String host) {
        synchronized (ACTIVE_SERVICES) {
            String identifier = host + ":" + port;
            UpnpInfo info = ACTIVE_SERVICES.remove(identifier);
            if (info != null) {
                info.service.shutdown();
                info.notifyOnChange();
                info.setOnChange(null);
            }
        }
    }
}
