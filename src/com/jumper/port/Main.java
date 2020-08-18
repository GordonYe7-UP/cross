package com.jumper.port;

import com.jumper.port.command.CommandThread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author fyypumpkin on 2018/10/26
 */
public class Main {
    public static void main(String[] args) {
        ExecutorService service = Executors.newFixedThreadPool(2);

        service.execute(new CommandThread());
    }
}
