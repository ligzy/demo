package com.realtime.demo;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateTest {

    public static void main(String[] args) {
        {
            Date dayTemp = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(dayTemp);
            SimpleDateFormat indexFormat = new SimpleDateFormat("yyyyMMdd");
            cal.add(Calendar.DATE, -5);

            for (int i = 0; i < 15; i++) {
                cal.add(Calendar.DATE, -1);
                String sName = indexFormat.format(cal.getTime()) + "_" + "";
                System.out.println(sName);
            }

            String sName = indexFormat.format(dayTemp) + "_" + "";

            System.out.println(sName);

        }
    }
}
