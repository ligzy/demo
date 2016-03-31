package com.realtime.demo;




public class TestWithVolatile {
    private static volatile boolean bChanged;

    public static void main(String[] args) throws InterruptedException {
        new Thread() {

            @Override
            public void run() {
                for (;;) {
                    if (bChanged == !bChanged) {
                        System.out.println("!=");
                        System.exit(0);
                    }
                }
            }
        }.start();
        Thread.sleep(1);
        new Thread() {

            @Override
            public void run() {
                for (;;) {
                    bChanged = !bChanged;
                }
            }
        }.start();
    }

}
