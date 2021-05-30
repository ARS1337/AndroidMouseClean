package com.example.androidmouseclean;

import android.util.Log;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;

public class MouseGyroscope extends MouseWithTyper{
    // Resolution of PC i.e. 1080 x 720 or 1920 x 1080 , etc
    private static MouseGyroscope mouseGyroscope = null;
    private final short resolutionX ;
    private final short resolutionY ;

    // How much can user tilt his mobile device in horizontal or vertical direction, i.e. from -75 to 75 degrees which that totals to 150
    private final float ANGLE_ROTATION_HORIZONTAL = 150.0f;
    private final float ANGLE_ROTATION_VERTICAL = 150.0f;

    // This will be Angle Angle_Rotation divide by resloution
    // we get how angle per one pixel i.e. at how much angles should pc cursor be moved by one pixel
    private float ANGLE_DETECT_HORIZONTAL = 0.0f;
    private float ANGLE_DETECT_VERTICAL = 0.0f;

    // Whats the max value of coords , which in this case is Resolution divide by 2 since cursor is centered at pc
    // since we get angle values in the form of positive/negative values he coors will also be from -reslolutoin/2 to resolution/2
    // Such that we can assume that pc's (0,0) coords is at the center rather than at top left
    private final short MAX_VALUE_HORIZONTAL ;
    private final short MAX_VALUE_VERTICAL ;

    private MouseGyroscope(short resolutionX,short resolutionY,float scrollSensitivity,String ip, int port){
        super(scrollSensitivity,ip,port);
        this.resolutionX             = resolutionX;
        this.resolutionY             = resolutionY;
        this.ANGLE_DETECT_HORIZONTAL = ANGLE_ROTATION_HORIZONTAL/resolutionX;
        this.ANGLE_DETECT_VERTICAL   = ANGLE_ROTATION_VERTICAL/resolutionY;
        this.MAX_VALUE_HORIZONTAL    = (short)((resolutionX/2)-1);            // since reslution go form 0 to 1079 or 0 to 719
        this.MAX_VALUE_VERTICAL      = (short)((resolutionY/2)-1);            // same
    }

    public static MouseGyroscope getMouseGyroscope(int resolutionX,int resolutionY,float scrollSeniitivy,String ip, int port){
        if(mouseGyroscope==null){
            mouseGyroscope = new MouseGyroscope((short) resolutionX,(short) resolutionY,scrollSeniitivy,ip,port);
        }

        return mouseGyroscope;
    }



    // gets the angles and converts it to coords
    @Override
    public void move(float xPar, float yPar) {
            // setting X
            short tempX = (short)(Math.toDegrees(xPar)/ANGLE_DETECT_HORIZONTAL);                       // convert to coords with Angel_detect
            if(tempX >= -MAX_VALUE_HORIZONTAL && tempX <= MAX_VALUE_HORIZONTAL) this.x = tempX;        // do bounds checking
            else this.x = (short)(MAX_VALUE_HORIZONTAL*(tempX/(Math.abs(tempX))));                     // if out of bounds, get the sign and multiply by maxValue
            // setting Y
            short tempY = (short)(Math.toDegrees(yPar)/ANGLE_DETECT_VERTICAL);
            if(tempY >= -MAX_VALUE_VERTICAL && tempY <= MAX_VALUE_VERTICAL) this.y = tempY;
            else this.y = (short)(MAX_VALUE_VERTICAL*(tempY/(Math.abs(tempY))));
    }

    @Override
    protected void stop() {
        super.stop();
        mouseGyroscope = null;
    }
}
