package com.example.androidmouseclean;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity  {

    private boolean isTouchPadEnable ;
    private String ip = "";
    private int port ;
    private int resolutionX ;
    private int resolutionY ;
    private float scrollSensitivity = 1.0f;
    private float touchpadSensitivity = 1.0f;

    // all gui widgets here
    private Intent intent;
    private RadioGroup radioGroup1;
    private EditText ipTextbox;
    private EditText portTextbox;
    private EditText resolutionXTextbox;
    private EditText resolutionYTextbox;
    private SeekBar touchpadSeekbar;
    private SeekBar scrollSeekbar;

    private DataStore dataStore;
    private AtomicInteger atomicIntegerTouchpad = new AtomicInteger();
    private AtomicInteger atomicIntegerScroll = new AtomicInteger();

    //@RequiresApi(api = Build.VERSION_CODES.O)
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialising all widgets here
        radioGroup1 = (RadioGroup) findViewById(R.id.radioGroup1);
        ipTextbox = (EditText) findViewById(R.id.ipTextbox);
        portTextbox = (EditText) findViewById(R.id.portTextbox);
        resolutionXTextbox = (EditText) findViewById(R.id.resolutionX);
        resolutionYTextbox = (EditText) findViewById(R.id.resolutionY);
        touchpadSeekbar = (SeekBar) findViewById(R.id.touchpadSeekbar);
        scrollSeekbar = (SeekBar) findViewById(R.id.scrollSeekbar);
        dataStore = DataStore.getDataStore();

        //adding all event listerners here
        intent = new Intent(getApplicationContext(),AfterMainActivity.class);

        radioGroup1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {

                RadioButton radioButton = (RadioButton) radioGroup.findViewById(i);
                boolean isChecked = radioButton.isChecked();
                if(radioButton.getId() == R.id.radioButton1) isTouchPadEnable = isChecked;
                else if(radioButton.getId() == R.id.radioButton2 ) isTouchPadEnable = !isChecked;
                else isTouchPadEnable = false;
            }
        });

        SeekBar.OnSeekBarChangeListener seekBarChangeListener= new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int seekBarId = seekBar.getId();
                switch (seekBarId) {
                    case R.id.touchpadSeekbar :
                        atomicIntegerTouchpad.set(i);
                        break;
                    case R.id.scrollSeekbar :
                        atomicIntegerScroll.set(i);
                        break;
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        touchpadSeekbar.setOnSeekBarChangeListener(seekBarChangeListener);
        scrollSeekbar.setOnSeekBarChangeListener(seekBarChangeListener);

        // call only after initialising datastore
        // sets all the previously set data on the GUI
        initialiseDataFromDatastoreToGUI();
    }

    public void onStartClick(View view){

        initialiseDataFromGUItoVariables();

        dataStore.setDataInBundle(MouseWithTyper.IP_STRING,ip);
        dataStore.setDataInBundle(MouseWithTyper.PORT_STRING,port);
        dataStore.setDataInBundle(MouseWithTyper.RESOLUTION_X_STRING,resolutionX);
        dataStore.setDataInBundle(MouseWithTyper.RESOLUTION_Y_STRING,resolutionY);
        dataStore.setDataInBundle(MouseWithTyper.SCROLL_SENSITIVITY_STRING,scrollSensitivity);
        dataStore.setDataInBundle(MouseWithTyper.TOUCHPAD_SENSITIVITY_STRING,touchpadSensitivity);
        dataStore.setDataInBundle(MouseWithTyper.TOUCHPAD_ENABLE_STRING,isTouchPadEnable);

        intent.putExtras(dataStore.getBundle());
        startActivity(intent);
    }

    public void initialiseDataFromDatastoreToGUI(){
        // retrieving data from datastore and setting gui
        dataStore.openDataGetter(getApplicationContext());
        ipTextbox.setText(dataStore.getData(MouseWithTyper.IP_STRING,MouseWithTyper.IP_DEFAULT));
        portTextbox.setText(Integer.toString(dataStore.getData(MouseWithTyper.PORT_STRING,MouseWithTyper.PORT_DEFAULT)));
        resolutionXTextbox.setText(Integer.toString(dataStore.getData(MouseWithTyper.RESOLUTION_X_STRING,MouseWithTyper.RESOLUTION_X_DEFAULT)));
        resolutionYTextbox.setText(Integer.toString(dataStore.getData(MouseWithTyper.RESOLUTION_Y_STRING,MouseWithTyper.RESOLUTION_Y_DEFAULT)));
        touchpadSeekbar.setProgress((int)(dataStore.getData(MouseWithTyper.TOUCHPAD_SENSITIVITY_STRING,MouseWithTyper.TOUCHPAD_SENSITIVITY_DEFAULT)*100.0f));
        scrollSeekbar.setProgress((int)(dataStore.getData(MouseWithTyper.SCROLL_SENSITIVITY_STRING,MouseWithTyper.SCROLL_SENSITIVITY_DEFAULT)*100.0f));
        dataStore.closeDataGetter();
    }

    public void initialiseDataFromGUItoVariables(){
        ip = ipTextbox.getText().toString();
        port = Integer.parseInt(portTextbox.getText().toString());
        resolutionX = Integer.parseInt(resolutionXTextbox.getText().toString());
        resolutionY = Integer.parseInt(resolutionYTextbox.getText().toString());
        touchpadSensitivity = 1.0f + (float)( atomicIntegerTouchpad.get()/100.0f);
        scrollSensitivity = 1.0f + (float)( atomicIntegerScroll.get()/100.0f);
        Log.i("bundle1", ip+ " "+ port+ resolutionX +" "+resolutionY+" "+touchpadSensitivity+" "+scrollSensitivity);
    }

    @Override
    public void onResume() {
        initialiseDataFromGUItoVariables();
        super.onResume();
    }

    @Override
    public void onPause() {

        super.onPause();

    }



    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        initialiseDataFromGUItoVariables();
        dataStore.openDataStore(getApplicationContext());
        dataStore.storeData(MouseWithTyper.IP_STRING,ip);
        dataStore.storeData(MouseWithTyper.PORT_STRING,port);
        dataStore.storeData(MouseWithTyper.RESOLUTION_X_STRING,resolutionX);
        dataStore.storeData(MouseWithTyper.RESOLUTION_Y_STRING,resolutionY);
        dataStore.storeData(MouseWithTyper.TOUCHPAD_SENSITIVITY_STRING,(touchpadSensitivity-1.0f));
        dataStore.storeData(MouseWithTyper.SCROLL_SENSITIVITY_STRING,(scrollSensitivity-1.0f));
        dataStore.closeDataStore();
        // save all data here
        super.onDestroy();
    }
    // put all pre destory storage opertions in a funcition
    // call that funcition in onDestory
    // so that it gets called regardless of whether onBackPressed was called or not
    // since finish() calls onDestory() down the stack
}