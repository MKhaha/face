package com.xieguotao.face.service;

import lombok.extern.slf4j.Slf4j;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

@Slf4j
public class LocalIpAddress {

    public static String getIpAddress(String internetCardName) {

        try {
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            while (enumeration.hasMoreElements()) {
                NetworkInterface networkInterface = enumeration.nextElement();
                System.out.println("name:");
                System.out.println(networkInterface.getName());
                System.out.println("displayname");
                System.out.println(networkInterface.getDisplayName());
                if (networkInterface.getName() != null && networkInterface.getDisplayName().equals(internetCardName)) {
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

    public static void main(String[] args) {

        try {
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            while (enumeration.hasMoreElements()) {
                NetworkInterface networkInterface = enumeration.nextElement();
                System.out.println("name:");
                System.out.println(networkInterface.getName());
                System.out.println("displayname");
                System.out.println(networkInterface.getDisplayName());
            }

            System.out.println("________");
            System.out.println("+++");
            String s = LocalIpAddress.getIpAddress(System.getenv("WLANNAME"));
            System.out.println(s);
        } catch (Exception ignored) {
        }

    }
}
