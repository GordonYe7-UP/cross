package com.jumper.port;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author fyypumpkin on 2018/10/26
 */
public class Utils {
    private static Thread thread;
    private static Queue<DisplayMsg> msg;

    private static void print(Object left, Object right) {
        String format = "%-20s";
        System.out.printf(format, left);
        System.out.println(right);
    }

    private static void init() {
        synchronized (Utils.class) {
            if (msg == null) {
                msg = new ArrayBlockingQueue<DisplayMsg>(100);
            }
            if (thread == null) {
                thread = new Thread(() -> {
                    while (true) {
                        DisplayMsg message = msg.poll();
                        if (message != null) {
                            print(message.getLeft(), message.getRight());
                        }

                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            thread.start();
        }
    }

    public static void put(DisplayMsg message) {
        synchronized (Utils.class) {
            while (thread == null || msg == null) {
                init();
            }

            msg.offer(message);
        }
    }
}
