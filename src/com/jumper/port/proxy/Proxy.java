package com.jumper.port.proxy;

import com.jumper.port.DisplayMsg;
import com.jumper.port.Utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


public class Proxy {
    private static String targetIp;
    private static Integer targetPort;
    private static Integer listenPort;
    private static Selector serverSelector;
    private static Selector selectors;
    private static ByteBuffer buffer;
    private static ProxyWrite proxyWrite;
    private static final AtomicInteger count = new AtomicInteger(0);
    private static final Map<String, SelectionKey> keyMap = new ConcurrentHashMap<>(8);

    private Proxy(Builder builder) {
        targetIp = builder.targetIp;
        targetPort = builder.targetPort;
        listenPort = builder.listenPort;
        buffer = builder.byteBuffer;
        proxyWrite = new ProxyWrite(buffer);
        synchronized (this) {
            try {
                if (serverSelector == null) {
                    serverSelector = Selector.open();
                }

                if (selectors == null) {
                    selectors = Selector.open();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void run() {
        ExecutorService service = Executors.newFixedThreadPool(2);
        service.execute(new ProxyServer());
        service.execute(new ExchangeService());
        System.out.println("start success...");
    }

    class ProxyServer implements Runnable {
        @Override
        public void run() {
            try {
                serverSelector = Selector.open();
                ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.socket().bind(new InetSocketAddress(listenPort));
                serverSocketChannel.register(serverSelector, SelectionKey.OP_ACCEPT);

                while (true) {
                    try {
                        if (serverSelector.select() > 0) {
                            Iterator<SelectionKey> iterator = serverSelector.selectedKeys().iterator();
                            while (iterator.hasNext()) {
                                SelectionKey key = iterator.next();
                                iterator.remove();

                                if (key.isAcceptable()) {
                                    try {
                                        SocketChannel targetChannel = SocketChannel.open();
                                        targetChannel.connect(new InetSocketAddress(targetIp, targetPort));
                                        targetChannel.configureBlocking(false);
                                        String targetName = ConstantsEnum.TARGET.getName() + "_" + count;
                                        keyMap.put(targetName, targetChannel.register(selectors, SelectionKey.OP_READ, targetName));
                                        Utils.put(new DisplayMsg("new remote connect：", targetChannel.getRemoteAddress() + " " + targetName));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    SocketChannel client = ((ServerSocketChannel) key.channel()).accept();
                                    client.configureBlocking(false);
                                    String userName = ConstantsEnum.CLIENT.getName() + "_" + count;
                                    keyMap.put(userName, client.register(selectors, SelectionKey.OP_READ, userName));
                                    Utils.put(new DisplayMsg("new user connect：", client.getRemoteAddress() + " " + userName));

                                    count.incrementAndGet();
                                }
                            }
                        }
                    } catch (IOException e) {
                        handleError(e);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ExchangeService implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    if (selectors.select(1) > 0) {
                        {
                            Iterator<SelectionKey> selectionKeys = selectors.selectedKeys().iterator();
                            while (selectionKeys.hasNext()) {
                                SelectionKey key = selectionKeys.next();
                                selectionKeys.remove();

                                ConstantsEnum type = ConstantsEnum.nameOf(((String) key.attachment()).split("_")[0]);
                                String serviceCount = ((String) key.attachment()).split("_")[1];

                                if (key.isReadable()) {
                                    try {
                                        Utils.put(new DisplayMsg("message from：", ((SocketChannel) key.channel()).getRemoteAddress() + " " + key.attachment()));
                                        assert type != null;
                                        if ("target".equals(type.getName())) {
                                            synchronized (this) {
                                                SelectionKey clientKey = keyMap.get(type.getTo() + serviceCount);
                                                int count = ((SocketChannel) key.channel()).read(buffer);
                                                if (count > 0) {
                                                    proxyWrite.handleWrite(clientKey, count);
                                                } else {
                                                    String targetName = (String) key.attachment();
                                                    keyMap.remove(targetName);
                                                    Utils.put(new DisplayMsg("remote close connection：", ((SocketChannel) key.channel()).getRemoteAddress() + " " + key.attachment()));
                                                    key.channel().close();
                                                    handleError();
                                                }
                                            }
                                        } else if ("client".equals(type.getName())) {
                                            synchronized (this) {
                                                SelectionKey targetKey = keyMap.get(type.getTo() + serviceCount);
                                                int count = ((SocketChannel) key.channel()).read(buffer);
                                                if (count > 0) {
                                                    proxyWrite.handleWrite(targetKey, count);
                                                } else {
                                                    String clientName = (String) key.attachment();
                                                    keyMap.remove(clientName);
                                                    Utils.put(new DisplayMsg("user close connection：", ((SocketChannel) key.channel()).getRemoteAddress() + " " + key.attachment()));

                                                    key.channel().close();
                                                    handleError();
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        handleError(e);
                                    }
                                }
                            }

                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static class Builder {
        private String targetIp;
        private Integer targetPort;
        private Integer listenPort;
        private ByteBuffer byteBuffer;

        public Builder setTargetIp(String ip) {
            this.targetIp = ip;
            return this;
        }

        public Builder setTargetPort(Integer port) {
            this.targetPort = port;
            return this;
        }

        public Builder setListenPort(Integer port) {
            this.listenPort = port;
            return this;
        }

        public Builder setByteBufferAllocate(Integer allocate) {
            this.byteBuffer = ByteBuffer.allocate(allocate);
            return this;
        }

        public Proxy build() {
            return new Proxy(this);
        }
    }

    private void handleError(Exception... e) {
        if (e.length > 0) {
            Utils.put(new DisplayMsg("error type：", e[0].getMessage()));
            e[0].printStackTrace();
        }
        List<String> list = new ArrayList<>();
        keyMap.forEach((key, value) -> {
            if (key != null && (key.startsWith("client") || key.startsWith("target"))) {
                String[] temp = key.split("_");
                if (list.contains(temp[1])) {
                    list.remove(temp[1]);
                } else {
                    list.add(temp[1]);
                }
            }
        });

        list.stream().parallel().forEach(item -> {
            SelectionKey key = keyMap.get("client_" + item);
            SelectionKey target = keyMap.get("target_" + item);
            if (key != null) {
                SocketChannel channel = (SocketChannel) key.channel();
                try {
                    channel.close();
                    keyMap.remove("client_" + item);
                    System.out.println("close " + key.attachment() + " connection");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

            if (target != null) {
                SocketChannel channel = (SocketChannel) target.channel();
                try {
                    channel.close();
                    keyMap.remove("target_" + item);
                    System.out.println("close " + target.attachment() + " connection");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

}
