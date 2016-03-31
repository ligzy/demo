package com.realtime.demo;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class TimerTest {

    public static void main(String[] args) {
        //for dead lock

        File file = new File("a.txt");
        if (file.exists()) {
            System.out.println(System.currentTimeMillis() - file.lastModified());
        } else {
            try {
                file.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Run");
                timer.cancel();
            }
        }, 1000);

        try {
            Thread.sleep(10000);
            System.gc();
            System.out.println("Hi,out");
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
