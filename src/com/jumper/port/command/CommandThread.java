package com.jumper.port.command;

import com.jumper.port.proxy.Proxy;

import java.util.Scanner;


public class CommandThread implements Runnable {
    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("input remote ip：");
        String targetIp = scanner.next();
        System.out.println("input remote port：");
        Integer targetPort = scanner.nextInt();
        System.out.println("input local listen port：");
        Integer listenPort = scanner.nextInt();
        System.out.println("input max buffer bytes：");
        Integer maxBuffer = scanner.nextInt();

        new Proxy.Builder().setTargetIp(targetIp).setTargetPort(targetPort).setListenPort(listenPort).setByteBufferAllocate(maxBuffer).build().run();

    }
}
