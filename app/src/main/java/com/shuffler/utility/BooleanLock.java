package com.shuffler.utility;

public class BooleanLock {

    private boolean value;

    public BooleanLock(){
        value = false;
    }

    public boolean toggle(){
        value = !value;
        return value;
    }

    public boolean getValue(){
        return value;
    }

    public void setValue(boolean value){
        this.value = value;
    }
}
