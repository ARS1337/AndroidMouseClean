package com.example.androidmouseclean;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

public abstract class MouseWithTyper {

    // used for string required in intents/bundle and also sharedPreferences
    public final static String IP_STRING = "IP";
    public final static String PORT_STRING = "PORT";
    public final static String RESOLUTION_X_STRING = "RESOLUTION_X";
    public final static String RESOLUTION_Y_STRING = "RESOLUTION_Y";
    public final static String TOUCHPAD_ENABLE_STRING = "TOUCHPAD_ENABLE";
    public final static String SCROLL_SENSITIVITY_STRING = "SCROLL_SENSITIVITY";
    public final static String TOUCHPAD_SENSITIVITY_STRING = "TOUCHPAD_SENSITIVITY";

    //scroll sensitivity
    private final int SCROLL_SENSITIVITY ;

    // scrollVal : to keep track of scroll value passed to SCroll();

    private int scrollVal=0;

    // mouse status flags like left click, right click, if touchpad is enabled, keys like ENTER , BACKSPACE
    // 7 -> vacant(sign bit, do not use) | 6 -> vacant | 5 -> vacant | 4 -> BACKSPACE | 3 -> ENTER | 2 -> if TouchPad is enabled | 1 -> RightClick | 0 -> LeftClick
    protected byte mouseStatusAndSpecialKeys = 0b000_00_0_00;

    // set (x, y) to move
    protected short x = 0;
    protected short y = 0;

    // byte array in which all the mouse status flags are set and also the string typed is stored
    // this will be sent by the Sender class in a UDP packet
    // 0 -> this will indicate the length of the string if typed/sent, its not sent if this is zero, so check this for non zero value before running the string typer code
    // 1 -> mouseStatusAndSpecialKeys i.e. leftclick, righClick, Enter, BackSpace, if Touchpad is enabled
    // 2 -> Higher byte of short X value
    // 3 -> Lower byte of short X value
    // 4 -> Higher byte of short Y value
    // 5 -> Lower byte of short Y value
    // 6 -> Scroll Value from -120 to 120 , (-120, -1) for Scroll Down , (120 , 1) for Scroll Up
    // (7 - 49) -> String typed by user, if is typed, set the 0th elemtnt indicating string length
    private final byte[] dataPacket = new byte[50];

    // udpSender class
    private SenderClass senderClass = null;

    // IPString and port
    protected final String ip;
    protected final int port;

    // FOR SETTING AND UNSETTING CORRESPONDING BITS IN THE mouseStatusAndSpecialKeys
    private final byte SET_LEFT_CLICK           = 0b000_00_0_01;
    private final byte SET_RIGHT_CLICK          = 0b000_00_0_10;
    protected final byte SET_TOUCHPAD_ENABLE    = 0b000_00_1_00;
    private final byte SET_ENTER                = 0b000_01_0_00;
    private final byte SET_BACKSPACE            = 0b000_10_0_00;

    private final byte UNSET_LEFT_CLICK         = 0b000_11_1_10;
    private final byte UNSET_RIGHT_CLICK        = 0b000_11_1_01;
    protected final byte UNSET_TOUCHPAD_ENABLE  = 0b000_11_0_11;
    private final byte UNSET_ENTER              = 0b000_10_1_11;
    private final byte UNSET_BACKSPACE          = 0b000_01_1_11;

    // constructor call from subclass
    public MouseWithTyper(int scrollSensitiviy,String ip, int port){
        this.SCROLL_SENSITIVITY = scrollSensitiviy;
        this.ip = ip;
        this.port = port;
    }

    // perform a leftclick
    protected void leftClick(boolean isLeftClick){
        if(isLeftClick) mouseStatusAndSpecialKeys=(byte)(mouseStatusAndSpecialKeys|SET_LEFT_CLICK);
        else mouseStatusAndSpecialKeys=(byte) (mouseStatusAndSpecialKeys&UNSET_LEFT_CLICK);
    }

    // perform a rightclick
    protected void rightClick(boolean isRightClick){
        if(isRightClick) mouseStatusAndSpecialKeys=(byte)(mouseStatusAndSpecialKeys|SET_RIGHT_CLICK);
        else mouseStatusAndSpecialKeys=(byte) (mouseStatusAndSpecialKeys&UNSET_RIGHT_CLICK);
    }

    // enter key press
    protected void enter(boolean isEnter){
        if(isEnter) mouseStatusAndSpecialKeys=(byte)(mouseStatusAndSpecialKeys|SET_ENTER);
        else mouseStatusAndSpecialKeys=(byte) (mouseStatusAndSpecialKeys&UNSET_ENTER);
    }

    // backspace press
    protected void backSpace(boolean isBackSpace){
        if(isBackSpace) mouseStatusAndSpecialKeys=(byte)(mouseStatusAndSpecialKeys|SET_BACKSPACE);
        else mouseStatusAndSpecialKeys=(byte) (mouseStatusAndSpecialKeys&UNSET_BACKSPACE);
    }

    protected void touchPadEnable(boolean isTouchPadEnable){
        if(isTouchPadEnable) mouseStatusAndSpecialKeys=(byte)(mouseStatusAndSpecialKeys|SET_TOUCHPAD_ENABLE);
        else mouseStatusAndSpecialKeys=(byte) (mouseStatusAndSpecialKeys&UNSET_TOUCHPAD_ENABLE);
    }

    // scroll up or down
    // pass data after doing bounds checking
    // scroll : if true then scroll has happened
    // isUp   : if true then scroll up has happnened
    //          else down scroll
    protected void scroll(boolean scrollBool,boolean isUp){
        if(scrollBool && isUp){
            if(scrollVal<120) scrollVal++;
            else scrollVal =120;
        }
        else if(scrollBool || isUp){
            if(scrollVal>-120) scrollVal--;
            else scrollVal =-120;
        }
        else scrollVal = 0;
        dataPacket[6]=(byte) scrollVal;
    }

    // Send the typed string
    protected void type(String typedString){
        char[] typedStringLocal = typedString.toCharArray();
        dataPacket[0]=(byte) typedStringLocal.length;                             // length of string is stored in index 0
        int i =7;                                                                 // string starts at index 7
        for(char c : typedStringLocal){
            dataPacket[i]=(byte) ((int)c);
            if((i<dataPacket.length) && (i<typedStringLocal.length)) i++;
        }
    }

    // implemented differently in both subclasses
    protected abstract void move(float x, float y);

    //puts all data in the byte array and sends the byte in datagram packeet and then resets all status flags
    protected void perform() throws IOException {
        dataPacket[1] = mouseStatusAndSpecialKeys;
        dataPacket[2] = (byte) (x&0xff);
        dataPacket[3] = (byte) ((x>>8)&0xff);
        dataPacket[4] = (byte) (y&0xff);
        dataPacket[5] = (byte) ((y>>8)&0xff);

        senderClass.send(dataPacket);                        // implement this

        x = y = mouseStatusAndSpecialKeys = 0;
        dataPacket[0] = dataPacket[1] = dataPacket[2] = dataPacket[3] = dataPacket[4] = dataPacket[5] = dataPacket[6] = 0;
    }

    // initialise all values like resolutiuon, angle detect, maxValue ,scroll sensitivy, touchpad sensitivity
    // also setup udp socket, return true if setup succesful else false, implemented differentlly in both class
    // call get sender class in this code since it throws exception
    // succesfull start will return a true value, any socket error will return a false value then try diff socket
    protected boolean start( ) throws SocketException, UnknownHostException {
            senderClass = SenderClass.getSenderClass(this.ip,this.port);
            return false;
    }

    // release all resources, call it in onDestroy/onBackPres
    public void stop() {
        senderClass.close();
        // call this method in onpause, onresume, ondestory, on backpresesd, etc
        // call SenderClass.close in this method
    }

}
