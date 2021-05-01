package com.example.androidmouseclean;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class SenderClass
{
    private static SenderClass senderClass = null;
    private Inet4Address ipAdress = null;
    private DatagramSocket socket = null;
    private int port = 1024;

    private SenderClass(Inet4Address ip,int port){
         ipAdress = ip;
         this.port = port;
    }

    // handle exception in mouseSubclasses
    public static SenderClass getSenderClass(String ip,int port) throws UnknownHostException, SocketException {
        if(senderClass==null){
            Inet4Address ipAddr = (Inet4Address) InetAddress.getByName(ip);
            senderClass = new SenderClass(ipAddr,port);
            senderClass.socket = new DatagramSocket();
        }
        return senderClass;
    }

    // handle exception in mouse subclasses
    public void send(byte[] data) throws IOException {
            socket.send(new DatagramPacket(data, data.length,ipAdress,port));
    }

    // close the connection
    public void close(){
        senderClass=null;
        ipAdress = null;
        socket.close();
        socket=null;
    }
}
