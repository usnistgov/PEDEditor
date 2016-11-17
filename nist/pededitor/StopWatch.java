package gov.nist.pededitor;
/*
    Copyright (c) 2005, Corey Goldberg

    StopWatch.java is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    (Trivially modified by EB)
*/

import java.text.*;

public class StopWatch {
    
    private long startTime = 0;
    private long stopTime = 0;
    private boolean running = false;
    public String name = "StopWatch";

    
    public void start() {
        startTime = System.currentTimeMillis();
        running = true;
    }

    
    public void stop() {
        stopTime = System.currentTimeMillis();
        running = false;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    // elapsed time in milliseconds
    public long getElapsedTime() {
        long elapsed;
        if (running) {
             elapsed = (System.currentTimeMillis() - startTime);
        }
        else {
            elapsed = (stopTime - startTime);
        }
        return elapsed;
    }
    
    
    // elapsed time in seconds
    public long getElapsedTimeSecs() {
        long elapsed;
        if (running) {
            elapsed = ((System.currentTimeMillis() - startTime) / 1000);
        }
        else {
            elapsed = ((stopTime - startTime) / 1000);
        }
        return elapsed;
    }

    public void ping() {
        String what = (new DecimalFormat("###.###"))
            .format(getElapsedTime()/1000.0);
        System.out.println(name + " elapsed time: " + what + " seconds");
    }
    
    //sample usage
    public static void main(String[] args) {
        StopWatch s = new StopWatch();
        s.start();
        //code you want to time goes here
        s.stop();
        System.out.println("elapsed time in milliseconds: " + s.getElapsedTime());
    }
}
