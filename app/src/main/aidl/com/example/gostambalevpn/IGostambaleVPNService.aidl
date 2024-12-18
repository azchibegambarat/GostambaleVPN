// IGostambaleVPNService.aidl
package com.example.gostambalevpn;

interface IGostambaleVPNService {

    boolean protect(int fd);

    void userPause(boolean b);

    /**
     * @param replaceConnection True if the VPN is connected by a new connection.
     * @return true if there was a process that has been send a stop signal
     */
    boolean stopVPN(boolean replaceConnection);
}