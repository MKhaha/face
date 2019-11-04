package com.xieguotao.face.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

@Slf4j
@Component("localIpAddress")
public class LocalIpAddress {

    public String getIpAddress(String internetCardName) {

        try {
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            while (enumeration.hasMoreElements()) {
                NetworkInterface networkInterface = enumeration.nextElement();
//                System.out.println("name:");
//                System.out.println(networkInterface.getName());
//                System.out.println("displayname");
//                System.out.println(networkInterface.getDisplayName());
                if (networkInterface.getName() != null && networkInterface.getName().equals(internetCardName)) {
                    Enumeration<InetAddress> en = networkInterface.getInetAddresses();
                    while (en.hasMoreElements()) {
                        InetAddress address = en.nextElement();
                        if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                            return address.getHostAddress();
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
