package com.example.androidmouseclean;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

public class DataStore {
    private static DataStore dataStore = null;

    private SharedPreferences sharedPreferences ;
    private SharedPreferences.Editor editor;

    private Bundle bundle ;

    public static DataStore getDataStore(){
        if(dataStore==null) {
            dataStore         = new DataStore();
            dataStore.bundle  = new Bundle();
        }
        return dataStore;
    }

    // save all the data in  bundle and return
    public void setDataInBundle(String key,int value){
        bundle.putInt(key,value);
    }

    public void setDataInBundle(String key,float value){
        bundle.putFloat(key,value);
    }

    public void setDataInBundle(String key,boolean value){
        bundle.putBoolean(key,value);
    }

    public void setDataInBundle(String key,String value){
        bundle.putString(key,value);
    }

    public Bundle getBundle(){
        bundle = new Bundle();
        return bundle;
    }

    // data storage operatoins in shared preference

    //call this method first to initialize sharedPreferences editor
    public void openDataStore(Context context){
        sharedPreferences = context.getSharedPreferences("AndroidMouseClean",0);
        editor = sharedPreferences.edit();
    }
    public void storeData(String key,int value){
        editor.putInt(key,value);
    }

    public void storeData(String key,float value){
        editor.putFloat(key,value);
    }

    public void storeData(String key,boolean value){
        editor.putBoolean(key,value);
    }

    public void storeData(String key,String value){
        editor.putString(key,value);
    }

    // call this methos to close the editor and set shared preferences to null;
    public void closeDataStore(){
        editor.apply();
        sharedPreferences = null;
    }

    //all data fetch operations here

    // call this method first to initialize sharedPreferences
    public void openDataGetter(Context context){
        sharedPreferences = context.getSharedPreferences("AndroidMouseClean",0);
    }

    public int getData(String key,int value){
        return sharedPreferences.getInt(key,value);
    }

    public float getData(String key,float value){
        return sharedPreferences.getFloat(key,value);
    }

    public boolean getData(String key,boolean value){
        return sharedPreferences.getBoolean(key,value);
    }

    public String getData(String key,String value){
        return sharedPreferences.getString(key,value);
    }

    // Close the shared preferences
    public void closeDataGetter(){
        sharedPreferences = null;
    }

}
