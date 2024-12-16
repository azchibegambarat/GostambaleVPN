package com.example.gostambalevpn.core;

public interface VPNManagement extends Runnable{
    void vpnStop();
    void vpnReconnect();
    boolean running();
}
