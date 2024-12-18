package com.example.gostambalevpn.core;

public interface VPNManagement extends Runnable{
    boolean vpnStop();
    void vpnReconnect();
    boolean running();
}
