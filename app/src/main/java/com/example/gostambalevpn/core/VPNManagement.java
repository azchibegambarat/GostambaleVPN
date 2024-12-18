package com.example.gostambalevpn.core;

public interface VPNManagement extends Runnable{
    boolean vpnStop();
    void vpnReconnect();
    VPNConnectionState running();
    public static enum VPNConnectionState{
        Connecting,
        Connected,
        Exit,
        RemoteClosed
    }
}
