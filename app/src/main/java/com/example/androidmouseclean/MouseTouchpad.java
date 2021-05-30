package com.example.androidmouseclean;

import android.util.Log;

public class MouseTouchpad extends MouseWithTyper{

    private static MouseTouchpad mouseTouchpad=null;
    private final float TOUCHPAD_SENSITIVITY;

    private float xIncr = 0; // stores the fractional part after removing short
    private float yIncr = 0; // same

    private MouseTouchpad(float scrollSnsitity, float touchSensitty, String ip, int port){
        super(scrollSnsitity,ip,port);
        this.TOUCHPAD_SENSITIVITY=touchSensitty;
    }

    public static MouseTouchpad getMouseTouchpad(float scrollSnsitity, float touchSensitty, String ip, int port){
        if(mouseTouchpad==null) mouseTouchpad = new MouseTouchpad(scrollSnsitity, touchSensitty, ip, port);
        Log.i("MouseWithTyper",mouseTouchpad.TOUCHPAD_SENSITIVITY+" ");
        return mouseTouchpad;
    }

    @Override
    public void move(float x, float y) {
        xIncr  += x * 2.0f * TOUCHPAD_SENSITIVITY;       // mobile screen is potrait and pc screen is landscape hence to  balance it out
        yIncr  += y * 1.0f* TOUCHPAD_SENSITIVITY;
        this.x  = (short)((int)xIncr);                   // set the integral part in this.x and this.y
        this.y  = (short)((int)yIncr);
        xIncr   = (float) (xIncr-this.x);                // store the fractional part
        yIncr   = (float) (yIncr-this.y);
    }

    @Override
    public void stop() {
        xIncr = yIncr = 0;
        super.stop();
        mouseTouchpad = null;
    }
}
