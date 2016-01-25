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
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.message.header.STAllHeader;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.igd.PortMappingListener;
import org.fourthline.cling.support.igd.callback.PortMappingAdd;
import org.fourthline.cling.support.igd.callback.PortMappingDelete;
import org.fourthline.cling.support.model.PortMapping;
import se.bes.mhfs.logger.MHFSLogger;
import se.bes.mhfs.manager.HFSMonitor;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class UPnpServiceProxy {

    private final UpnpService service;

    private final ArrayList<OnServiceChange> listeners = new ArrayList<>();

    private final ArrayList<String> LOG = new ArrayList<>();

    private final HashMap<String, UpnpMapping> upnpMappings = new HashMap<>();

    public interface OnServiceChange {
        Void onChange();
    }

    public static class UpnpMapping {
        public final int port;
        public final String host;
        public final Service service;
        public final PortMapping portMapping;

        private UpnpMapping(int port, String host, Service service, PortMapping portMapping) {
            this.port = port;
            this.host = host;
            this.service = service;
            this.portMapping = portMapping;
        }
    }

    private static final HashMap<String, UpnpService> MAPPED_INTERFACES = new HashMap<>();

    private final MHFSLogger logger;

    public UPnpServiceProxy(MHFSLogger logger) {
        this.logger = logger;

        RegistryListener listener = new RegistryListener() {

            public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {}

            public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex) {
                synchronized (LOG) {
                    LOG.add("Discovery failed: " + device.getDisplayString() + " => " + ex);
                }
                notifyOnServiceChangeListeners();
            }

            public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
                System.out.println("Added " + device.getDisplayString());
                notifyOnServiceChangeListeners();
            }

            public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
            }

            public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
                System.out.println("Removed " + device.getDisplayString());
                notifyOnServiceChangeListeners();
            }

            public void localDeviceAdded(Registry registry, LocalDevice device) {}
            public void localDeviceRemoved(Registry registry, LocalDevice device) {}
            public void beforeShutdown(Registry registry) {}
            public void afterShutdown() {}
        };

        service = new UpnpServiceImpl(listener);
        service.getControlPoint().search(new STAllHeader());

        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                synchronized (service) {
                    service.shutdown();
                }
                unmapAllRegistered();
            }
        });
    }

    public void addListener(OnServiceChange onServiceChange) {
        synchronized (listeners) {
            listeners.add(onServiceChange);
        }
    }

    public void removeListener(OnServiceChange onServiceChange) {
        synchronized (listeners) {
            listeners.remove(onServiceChange);
        }
    }

    private void notifyOnServiceChangeListeners() {
        synchronized (listeners) {
            for (OnServiceChange listener : listeners) {
                listener.onChange();
            }
        }
    }

    public void mapAllInterfaces(HFSMonitor monitor) {
        try {
            InetAddress[] all = InetAddress.getAllByName(InetAddress
                    .getLocalHost().getHostName());
            for (InetAddress inetAddress : all) {
                final String hostAddress = inetAddress.getHostAddress();
                final int port = monitor.getPort();

                final String identifier = hostAddress + ":" + port;

                synchronized (MAPPED_INTERFACES) {
                    UpnpService service = MAPPED_INTERFACES.get(identifier);
                    if (service == null) {
                        PortMapping desiredMapping =
                                new PortMapping(
                                        port,
                                        hostAddress,
                                        PortMapping.Protocol.TCP,
                                        identifier
                                );

                        service =
                                new UpnpServiceImpl(
                                        new PortMappingListener(desiredMapping)
                                );

                        service.getControlPoint().search();
                        MAPPED_INTERFACES.put(identifier, service);
                    }
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        notifyOnServiceChangeListeners();
    }

    public void unmapAllRegistered() {
        synchronized (MAPPED_INTERFACES) {
            Iterator<UpnpService> it = MAPPED_INTERFACES.values().iterator();
            while (it.hasNext()) {
                UpnpService service = it.next();
                service.shutdown();
                it.remove();
            }
        }
        notifyOnServiceChangeListeners();
    }

    public UpnpService getMapped(int port, String host) {
        final String identifier = host + ":" + port;
        return MAPPED_INTERFACES.get(identifier);
    }

    public static final DeviceType CONNECTION_DEVICE_TYPE = new UDADeviceType("WANConnectionDevice", 1);
    public static final ServiceType IP_SERVICE_TYPE = new UDAServiceType("WANIPConnection", 1);
    public static final ServiceType PPP_SERVICE_TYPE = new UDAServiceType("WANPPPConnection", 1);

    public void addPortMapping(final RemoteDevice device, int port, String host) {

        Device[] connectionDevices = device.findDevices(CONNECTION_DEVICE_TYPE);
        if (connectionDevices.length == 0) {
            logger.append("IGD doesn't support '" + CONNECTION_DEVICE_TYPE + "': " + device);
            return;
        }

        Device connectionDevice = connectionDevices[0];
        logger.append("Using first discovered WAN connection device: " + connectionDevice);

        Service ipConnectionService = connectionDevice.findService(IP_SERVICE_TYPE);
        Service pppConnectionService = connectionDevice.findService(PPP_SERVICE_TYPE);

        if (ipConnectionService == null && pppConnectionService == null) {
            logger.append("IGD doesn't support IP or PPP WAN connection service: " + device);
        }

        Service service = ipConnectionService != null ? ipConnectionService : pppConnectionService;
        if (service != null) {
            final String identifier = host + ":" + port;

            PortMapping desiredMapping =
                    new PortMapping(
                            port,
                            host,
                            PortMapping.Protocol.TCP,
                            identifier
                    );

            synchronized (upnpMappings) {
                upnpMappings.put(identifier, new UpnpMapping(port, host, service, desiredMapping));
            }

            this.service.getControlPoint().execute(
                    new PortMappingAdd(service, desiredMapping) {

                        @Override
                        public void success(ActionInvocation invocation) {
                            synchronized (LOG) {
                                LOG.add("Successfully added " + identifier + " on " + device.getDisplayString());
                            }
                            notifyOnServiceChangeListeners();
                        }

                        @Override
                        public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                            synchronized (LOG) {
                                LOG.add(defaultMsg);
                            }
                            notifyOnServiceChangeListeners();
                        }
                    }
            );
        } else {
            System.out.println("Couldn't find a service for " + device.getDisplayString());
        }
    }

    public void removePortMapping(int port, String host) {
        final String identifier = host + ":" + port;

        final UpnpMapping upnpMapping;
        synchronized (upnpMappings) {
            upnpMapping = upnpMappings.get(identifier);
        }

        if (upnpMapping != null) {
            service.getControlPoint().execute(
                    new PortMappingDelete(upnpMapping.service, upnpMapping.portMapping) {

                        @Override
                        public void success(ActionInvocation invocation) {
                            synchronized (upnpMappings) {
                                upnpMappings.remove(identifier);
                            }
                            synchronized (LOG) {
                                LOG.add("Successfully removed " + identifier);
                            }
                            notifyOnServiceChangeListeners();
                        }

                        @Override
                        public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                            synchronized (upnpMappings) {
                                upnpMappings.remove(identifier);
                            }
                            synchronized (LOG) {
                                LOG.add(defaultMsg);
                            }
                            notifyOnServiceChangeListeners();
                        }
                    }
            );
        }
    }

    public ArrayList<String> getLog() {
        synchronized (LOG) {
            ArrayList<String> log = new ArrayList<>(LOG);
            Collections.reverse(log);
            return log;
        }
    }

    public Collection<Device> getDevices() {
        return service.getRegistry().getDevices();
    }
}
