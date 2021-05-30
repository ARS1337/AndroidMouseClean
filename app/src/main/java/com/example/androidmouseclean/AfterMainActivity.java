package com.example.androidmouseclean;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AfterMainActivity extends AppCompatActivity implements SensorEventListener, View.OnTouchListener {

    // all gui widgets here
    private Button leftButton;
    private Button rightButton;
    private Button scrollUpButton;
    private Button scrollDownButton;
    private EditText typeTextBox;

    // all variables here
    private boolean isTouchpad;
    private int resolutionX;
    private int resolutionY;
    private int port;
    private String ip;
    private float scrollSensitivity;
    private float touchpadSensitivity;

    //all flags here
    private volatile boolean mouseLeftDown = false;                                   // mouse left  button down
    private volatile boolean mouseRightDown = false;                                  // mouse right  button down
    private volatile boolean mouseLeftUP = false;                                     // mouse left  button up
    private volatile boolean mouseRightUp = false;                                    // mouse right  button up
    private volatile boolean isScroll = false;                                        // a scroll has happened
    private volatile boolean isScrollUp = false;                                      // scroll up has happened if true, else down scroll, this is checked only if isScroll is true
    private volatile boolean isEnter = false;                                         // enter was pressed
    private volatile boolean isBackSpace = false;                                     // backspace was pressed
    private volatile boolean isSend = false;                                          // send was pressed, if true then only check the typeTextBox for text to send
    private volatile boolean threadKillFlag = false;                                  // used to kill threads
    private volatile boolean isMove = false;                                          // used to check if the user moved between three events, if not it was a tap event i.e. mouseLeftClick
    private volatile boolean isReset = false;                                         // used to reset if mouse left down was happened, and center cursor
    private volatile boolean areAnglesUpdatead = false;                               // to check if the angles in orientationAngles are updated or not, not used currently, can be used in optimisation
    private volatile boolean isLeftClick = false;                                     // leftMouseDown -> leftMouseUp combined
    private volatile boolean isDrag = false;                                          // is set if the minTimeForDrag and isPrevTap both are true, i.e. an motionDown -> motionUp -> motionDown -> motionMove
    private volatile boolean isPrevTap = false;                                       // stores if the prev event on touchpad was move just a tap, is used while setting isDrag

    // all uncategorised variables here
    // x,y relative : stores the relative distance moves on the user screen
    private float xRel = 0.0f;
    private float yRel = 0.0f;

    // used to calculate xRel, yRel, stores the value of users
    // position on the screen so the we can subtract it with
    // current postion to get xRel, yRel
    // use this in mouse.move(xRel,yRel)
    private float x0 = 0.0f;
    private float y0 = 0.0f;

    // counter to keep track of how much time user tapped the back button
    // long to keep track of time between induvidual taps
    private int counter = 2;
    private long time = 0;
    private long minTimeForDrag = 0 ;                                                  // stores time between induvidual taps to determine if a touchpad drag is drag or moving across touchpad without drag

    // main mouse
    private MouseWithTyper mouseWithTyper;

    // position sensor magic
    private SensorManager sensorManager;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_after_main);

        // initialising all gui widgets
        leftButton = (Button) findViewById(R.id.leftButton);
        rightButton = (Button) findViewById(R.id.rightButton);
        scrollUpButton = (Button) findViewById(R.id.scrollUpButton);
        scrollDownButton = (Button) findViewById(R.id.scrollDownButton);
        typeTextBox = (EditText) findViewById(R.id.typeTextBox);

        // all event listeners here
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        leftButton.setOnTouchListener(this);
        rightButton.setOnTouchListener(this);
        scrollUpButton.setOnTouchListener(this);
        scrollDownButton.setOnTouchListener(this);

        // initalisng all core app logic here

        // here get bundle and initialise some local variables, so they can be used in the initialisation of the mouseWithTyper

        initialDataFromBundle(getIntent().getExtras());
        Toast.makeText(getApplicationContext(),""+ ip + " " + port + resolutionX + " " + resolutionY + " " + touchpadSensitivity + " " + scrollSensitivity+"",Toast.LENGTH_LONG).show();


        // initialise what user wants

        if(!isTouchpad){
        new Thread(new Runnable() {
            @Override
            public void run() {
                mouseWithTyper = MouseGyroscope.getMouseGyroscope(resolutionX,resolutionY,scrollSensitivity,ip,port);

                try{
                    mouseWithTyper.start();
                    while (!threadKillFlag) {
                            //if(areAnglesUpdatead){
                                updateOrientationAngles();
                            //    areAnglesUpdatead = false;
                            //}
                            if(mouseLeftDown) mouseWithTyper.leftClickDown();
                            if(mouseRightDown) mouseWithTyper.rightClickDown();
                            if(mouseLeftUP) mouseWithTyper.leftClickUp();
                            if(mouseRightUp) mouseWithTyper.rightClickUp();
                            if(isEnter) mouseWithTyper.enter();
                            if(isBackSpace) mouseWithTyper.backSpace();
                            if(isSend && (typeTextBox.getText().length()!=0) ){
                                mouseWithTyper.type(typeTextBox.getText().toString());
                                typeTextBox.setText("");
                            }
                            if(isReset) mouseWithTyper.resetMouse();
                            mouseWithTyper.move(orientationAngles[2],orientationAngles[1]);
                            mouseWithTyper.scroll(isScroll,isScrollUp);
                            mouseWithTyper.perform();
                            mouseRightUp = mouseLeftUP = mouseRightDown = mouseLeftDown = isEnter = isBackSpace = isSend = isReset = false;
                            xRel = yRel = 0.0f;
                            Thread.sleep(30);
                            }
                        }
                    catch(Exception e){
                        Log.i("Error",e.getMessage().toString()+e.getStackTrace());
                    }
                }
            }).start();
        }

        else
            new Thread(new Runnable() {
            @Override
            public void run() {
                mouseWithTyper = MouseTouchpad.getMouseTouchpad(scrollSensitivity,touchpadSensitivity,ip,port);
                try{
                    mouseWithTyper.start();
                    while (!threadKillFlag) {
                        //updateOrientationAngles();
                        if(mouseLeftDown) {
                            mouseWithTyper.leftClickDown();
                            Log.i("Drag","Mouse started : Action DOWN");
                        }
                        if(mouseRightDown) mouseWithTyper.rightClickDown();
                        if(mouseLeftUP){
                            mouseWithTyper.leftClickUp();
                            Log.i("Drag","Mouse started : Action UP");
                        }
                        if(mouseRightUp) mouseWithTyper.rightClickUp();
                        if(isLeftClick) mouseWithTyper.leftClick();
                        if(isEnter) mouseWithTyper.enter();
                        if(isBackSpace) mouseWithTyper.backSpace();
                        if(isSend && (typeTextBox.getText().length()!=0) ){
                            mouseWithTyper.type(typeTextBox.getText().toString());
                            typeTextBox.setText("");
                        }
                        if(isReset) mouseWithTyper.resetMouse();
                        mouseWithTyper.move(xRel,yRel);
                        mouseWithTyper.scroll(isScroll,isScrollUp);
                        mouseWithTyper.perform();
                        mouseRightUp = mouseLeftUP = mouseRightDown = mouseLeftDown = isEnter = isBackSpace = isSend = isReset = isLeftClick = false;
                        xRel = yRel = 0.0f;
                        Thread.sleep(40);
                    }
                }
                catch(Exception e){
                    Log.i("Error",e.getMessage());
                }
            }
        }).start();
    }


    // sets enter flag to true when onClick
    // unset by Thread after Mouse.Enter()
    public void onEnterClick(View view){
        isEnter = true;
    }

    // sets reset flag to true when onClick
    // unset by Thread after Mouse.reset()
    public void onReset(View view){
        isReset = true;
    }

    // sets send flag to true when onClick
    // unset by Thread after Mouse.Type()
    public void onSendClick(View view){
        isSend = true;
    }

    // sets backspace flag to true when onClick
    // unset by Thread after Mouse.BackSpace()
    public void onBackSpaceClick(View view){
        isBackSpace = true;
    }

    public void initialDataFromBundle(Bundle bundle){
        ip = bundle.getString(MouseWithTyper.IP_STRING,MouseWithTyper.IP_DEFAULT);
        port = bundle.getInt(MouseWithTyper.PORT_STRING,MouseWithTyper.PORT_DEFAULT);
        resolutionX = bundle.getInt(MouseWithTyper.RESOLUTION_X_STRING,MouseWithTyper.RESOLUTION_X_DEFAULT);
        resolutionY = bundle.getInt(MouseWithTyper.RESOLUTION_Y_STRING,MouseWithTyper.RESOLUTION_Y_DEFAULT);
        isTouchpad = bundle.getBoolean(MouseWithTyper.TOUCHPAD_ENABLE_STRING,MouseWithTyper.TOUCHPAD_ENABLE_DEFAULT);
        scrollSensitivity = bundle.getFloat(MouseWithTyper.SCROLL_SENSITIVITY_STRING,MouseWithTyper.SCROLL_SENSITIVITY_DEFAULT);
        touchpadSensitivity = bundle.getFloat(MouseWithTyper.TOUCHPAD_SENSITIVITY_STRING,MouseWithTyper.TOUCHPAD_SENSITIVITY_DEFAULT);

    }

    // mouse left, right and scroll up and downs
    // add this to those four buttons set on touch listeners
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                view.setPressed(true);
                Log.i("onTouch"," on Touch Down");
                // mouse (left, right, scroll up, down )'s Down events must be implemented here
                if(view.getId()==R.id.leftButton) mouseLeftDown = true;
                if(view.getId()==R.id.rightButton) mouseRightDown = true;
                if(view.getId()==R.id.scrollDownButton) {
                    isScroll = true;
                    isScrollUp = false;
                }
                if(view.getId()==R.id.scrollUpButton){
                    isScroll = true;
                    isScrollUp = true;

                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_CANCEL:
                view.setPressed(false);
                Log.i("onTouch"," on Touch Up");
                // mouse (left, right, scroll up, down )'s Up events must be implemented here
                if(view.getId()==R.id.leftButton) mouseLeftUP = true;
                if(view.getId()==R.id.rightButton) mouseRightUp = true;
                if(view.getId()==R.id.scrollDownButton) isScroll = isScrollUp = false;
                if(view.getId()==R.id.scrollUpButton) isScroll = isScrollUp = false;
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
        }
        return true;
    }

    // users position on screen and also check if the user tapped on the screen
    // part of MainView, no need to add, called by the initialiser of this activity just like onCreate, onDestroy, etc
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE :

                float x1 = event.getX();
                float y1 = event.getY();
                xRel = x1 - x0;
                yRel = y1 - y0;
                x0 = x1;
                y0 = y1;
                isMove = true;
                if(isPrevTap && ((System.currentTimeMillis()-minTimeForDrag)<300)) {
                    isDrag = true;
                    mouseLeftDown = true;
                    isPrevTap = false;
                    Log.i("Drag","Drag started : Action DOWN");
                }
                break;

            case MotionEvent.ACTION_DOWN:
                Log.i("onTouchEvent","onTouchEvent : Action DOWN");
                x0 = event.getX();
                y0 = event.getY();
                break;

            case MotionEvent.ACTION_UP:
                Log.i("onTouchEvent","onTouchEvent : Action UP");
                if(isMove && isDrag){
                    isMove = false;
                    mouseLeftUP = true;
                    isDrag = isPrevTap = false;
                    Log.i("Drag","Drag stopped : Action UP");
                }
                else if(isMove) isMove = false;
                else {
                    isLeftClick = isPrevTap = mouseLeftDown = mouseLeftUP = true;
                    minTimeForDrag = System.currentTimeMillis();
                }
                break;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        counter--;
        long dTime = System.currentTimeMillis()-time;
        if(counter==1){
            time = System.currentTimeMillis();
        }
        else if(counter==0 && dTime < 2500 ){
            threadKillFlag = true;
            mouseWithTyper.stop();
            finish();
        }
        else {
            counter = 1;
            time = System.currentTimeMillis();
        }
    }

    @Override
    protected void onDestroy() {
        threadKillFlag = true;
        // if the use exit without using back button then do the same
        // preDestory opertions here also
        super.onDestroy();
    }



    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // all positon sensor code here

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_UI, 0);
        }
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_UI, 0);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Don't receive any more updates from either sensor.
        sensorManager.unregisterListener(this);
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading,
                    0, accelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading,
                    0, magnetometerReading.length);
        }
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(rotationMatrix, null,
                accelerometerReading, magnetometerReading);

        // "rotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(rotationMatrix, orientationAngles);

        // "orientationAngles" now has up-to-date information.
    }

}