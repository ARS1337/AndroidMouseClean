package com.example.androidmouseclean;

public class MouseTouchpad extends MouseWithTyper{

    private static MouseTouchpad mouseTouchpad=null;
    private final float TOUCHPAD_SENSITIVITY;

    private float xIncr = 0; // stores the fractional part after removing short
    private float yIncr = 0; // same

    private MouseTouchpad(int scrollSnsitity, float touchSensitty, String ip, int port){
        super(scrollSnsitity,ip,port);
        this.TOUCHPAD_SENSITIVITY=touchSensitty;
    }

    public static MouseTouchpad getMouseTouchpad(int scrollSnsitity, float touchSensitty, String ip, int port){
        if(mouseTouchpad==null) mouseTouchpad = new MouseTouchpad(scrollSnsitity, touchSensitty, ip, port);
        return mouseTouchpad;
    }

    @Override
    public void move(float x, float y) {
        xIncr  += x * 2.0f * TOUCHPAD_SENSITIVITY;
        yIncr  += y * 1.0f* TOUCHPAD_SENSITIVITY;
        this.x  = (short)((int)xIncr);
        this.y  = (short)((int)yIncr);
        xIncr   = (float) (xIncr-this.x);
        yIncr   = (float) (yIncr-this.y);
        touchPadEnable(true);
    }

    @Override
    public void stop() {
        xIncr = yIncr = 0;
        super.stop();
    }
}
