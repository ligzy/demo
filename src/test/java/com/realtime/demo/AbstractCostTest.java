/**
 * 
 * Title:        United Risk Management System
 * Description:  
 * Copyright:    Copy Right (c) 2015—2016
 * Company:      ROOTNET
 * @author       Jeremy Li
 * @date         2016年5月17日
 */
package com.realtime.demo;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Jeremy Li
 *
 */
public abstract class AbstractCostTest {

    private int count = 1000000;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */

    /**
     * @throws java.lang.Exception
     */
    @Before
    public abstract void setUp() throws Exception;

    @After
    public void tearDown() throws Exception {
        System.out.println("tearDown");
    }

    @Test
    public void test() {
        try {

            long timeStart = System.nanoTime();

            int i = 0;
            while (i < count) {
                execute(i);
                i++;
            }
            long timeEnd = System.nanoTime();
            System.out.printf(
                "count:%10d, cost(ms):%10d, avg(us):%4.1f , rate(count/s):%10.1f\r\n",
                (timeEnd - timeStart) / 1000 / 1000,
                count,
                (double) (timeEnd - timeStart) / count / 1000,
                (double) count / (timeEnd - timeStart) * 1000000000);

        } finally {
            close();
        }
    }

    public abstract void execute(int index);

    public abstract void close();

}
