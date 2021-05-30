package com.example.androidmouseclean;

import android.util.Log;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

public abstract class MouseWithTyper {

    // used for string required in intents/bundle and also sharedPreferences
    public final static String IP_STRING                   = "IP";
    public final static String PORT_STRING                 = "PORT";
    public final static String RESOLUTION_X_STRING         = "RESOLUTION_X";
    public final static String RESOLUTION_Y_STRING         = "RESOLUTION_Y";
    public final static String TOUCHPAD_ENABLE_STRING      = "TOUCHPAD_ENABLE";
    public final static String SCROLL_SENSITIVITY_STRING   = "SCROLL_SENSITIVITY";
    public final static String TOUCHPAD_SENSITIVITY_STRING = "TOUCHPAD_SENSITIVITY";

    // default fianl values
    public final static String IP_DEFAULT = "192.168.1.2";
    public final static int PORT_DEFAULT = 49001;
    public final static int RESOLUTION_X_DEFAULT = 1080;
    public final static int RESOLUTION_Y_DEFAULT = 720;
    public final static boolean TOUCHPAD_ENABLE_DEFAULT = false;
    public final static float SCROLL_SENSITIVITY_DEFAULT = 2.0f;
    public final static float TOUCHPAD_SENSITIVITY_DEFAULT = 2.0f;

    //scroll sensitivity
    private final float SCROLL_SENSITIVITY ;

    // scrollVal : to keep track of scroll value passed to SCroll();

    private float scrollVal = 0;

    // mouse status flags like left click, right click, if touchpad is enabled, keys like ENTER , BACKSPACE
    // 8 -> vacant(sign bit, do not use) | 7 -> resetBit | 6 -> RightClickDown | 5 -> LeftClickDown | 4-> ENTER | 3 -> BACKSPACE | 2 -> RightClickUp | 1 -> LeftClickUp
    protected byte mouseStatusAndSpecialKeys = 0b0000_0000;

    // set (x, y) to move
    protected short x = 0;
    protected short y = 0;

    // byte array in which all the mouse status flags are set and also the string typed is stored
    // this will be sent by the Sender class in a UDP packet
    // 0 -> this will indicate the length of the string if typed/sent, its not sent if this is zero, so check this for non zero value before running the string typer code
    // 1 -> mouseStatusAndSpecialKeys
    // 2 -> Higher byte of short X value
    // 3 -> Lower byte of short X value
    // 4 -> Higher byte of short Y value
    // 5 -> Lower byte of short Y value
    // 6 -> Scroll Value from -120 to 120 , (-120, -1) for Scroll Down , (120 , 1) for Scroll Up
    // (7 - 49) -> String typed by user, if is typed, set the 0th elemtnt indicating string length
    // 50 -> syncbyte to detect packet drops
    private final byte[] dataPacket = new byte[51];
    private byte syncByte = 0;

    // udpSender class
    private SenderClass senderClass = null;

    // IPString and port
    protected final String ip;
    protected final int port;

    // FOR SETTING CORRESPONDING BITS IN THE mouseStatusAndSpecialKeys
    private final byte SET_LEFT_CLICK_UP       = 0b0_0_0_0_0_0_0_1;     // 1
    private final byte SET_RIGHT_CLICK_UP      = 0b0_0_0_0_0_0_1_0;     // 2
    private final byte SET_ENTER               = 0b0_0_0_0_0_1_0_0;     // 4
    private final byte SET_BACKSPACE           = 0b0_0_0_0_1_0_0_0;     // 8
    private final byte SET_LEFT_CLICK_DOWN     = 0b0_0_0_1_0_0_0_0;     // 16
    private final byte SET_RIGHT_CLICK_DOWN    = 0b0_0_1_0_0_0_0_0;     // 32
    private final byte SET_MOUSE_RESET         = 0b0_1_0_0_0_0_0_0;     // 64

    // constructor call from subclass
    public MouseWithTyper(float scrollSensitiviy,String ip, int port){
        this.SCROLL_SENSITIVITY = scrollSensitiviy;
        this.ip = ip;
        this.port = port;
    }

    // perform a left Mouse button Up
    protected void leftClickUp(){
        mouseStatusAndSpecialKeys=(byte)(mouseStatusAndSpecialKeys|SET_LEFT_CLICK_UP);
    }

    // perform a right mouse button Up
    protected void rightClickUp(){
        mouseStatusAndSpecialKeys=(byte)(mouseStatusAndSpecialKeys|SET_RIGHT_CLICK_UP);
    }

    // Perform a left mouse button down
    protected void leftClickDown(){
        mouseStatusAndSpecialKeys=(byte)(mouseStatusAndSpecialKeys|SET_LEFT_CLICK_DOWN);
    }

    // perform a right mouse button down
    protected void rightClickDown(){
        mouseStatusAndSpecialKeys=(byte)(mouseStatusAndSpecialKeys|SET_RIGHT_CLICK_DOWN);
    }

    // perform a left click
    protected void leftClick(){
        leftClickDown();
        leftClickUp();
    }

    // perform a right click
    protected void rightClick(){
        rightClickDown();
        rightClickUp();
    }

    // perform enter key press
    protected void enter(){
        mouseStatusAndSpecialKeys=(byte)(mouseStatusAndSpecialKeys|SET_ENTER);
    }

    // perform backspace press
    protected void backSpace(){
        mouseStatusAndSpecialKeys=(byte)(mouseStatusAndSpecialKeys|SET_BACKSPACE);
    }

    // scroll up or down
    // scroll : if true then scroll has happened
    // isUp   : if true then scroll up has happnened
    //          else it was a down scroll
    protected void scroll(boolean scrollBool,boolean isUp){
        if(scrollBool && isUp){
            if(scrollVal<120) scrollVal+=SCROLL_SENSITIVITY;
            else scrollVal =120.0f;
        }
        else if(scrollBool || isUp){
            if(scrollVal>-120) scrollVal-=SCROLL_SENSITIVITY;
            else scrollVal =-120.0f;
        }
        else scrollVal = 0.0f;
    }

    // Send the typed string
    protected void type(String typedString){
        char[] typedStringLocal = typedString.toCharArray();
        int i =7;
        // string starts at index 7
        int dataPacketLength = 50;
        for(char c : typedStringLocal){
            dataPacket[i]=(byte) ((int)c);
            if((i<50) && (i<(typedStringLocal.length+7))) i++;
            else break;
        }
        dataPacket[0]=(byte) (i - 7);                             // length of string is stored in index 0
    }

    // implemented differently in both subclasses
    protected abstract void move(float x, float y);

    // in pc performs mouseUP if mouse was down already and puts the cursor in the center
    protected void resetMouse(){
        mouseStatusAndSpecialKeys = (byte) (mouseStatusAndSpecialKeys|SET_MOUSE_RESET);
    }

    //puts all data in the byte array and sends the byte in datagram packeet and then resets all status flags
    protected void perform() throws IOException {
        dataPacket[1] = mouseStatusAndSpecialKeys;
        dataPacket[2] = (byte) (x&0xff);
        dataPacket[3] = (byte) ((x>>8)&0xff);
        dataPacket[4] = (byte) (y&0xff);
        dataPacket[5] = (byte) ((y>>8)&0xff);
        dataPacket[6] = (byte) ((int) scrollVal);
        dataPacket[50]= syncByte++;
        //Log.i("coords" , x +" "+y);
        senderClass.send(dataPacket);                        // implement this
        x = y = mouseStatusAndSpecialKeys = 0;
        dataPacket[0]  = 0;
        //scrollVal = 0.0f;
        if(syncByte==127) syncByte = 0;
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
    protected void stop() {
        senderClass.close();
        // call this method in onpause, onresume, ondestory, on backpresesd, etc
        // call SenderClass.close in this method
    }

}
