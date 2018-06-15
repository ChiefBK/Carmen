package com.ian.carmen.common;

import java.util.HashMap;
import java.util.Map;

public class Connection {
    public final static String USER_WALLET_NAME = "userWallet";
    public final static String DANCER_WALLET_NAME = "dancerWallet";

    public final static int USER_WALLET_PORT = 6666;
    public final static int DANCER_WALLET_PORT = 7777;

    public static Map<String, Integer> walletPorts = new HashMap<>();

    static {
        walletPorts.put(USER_WALLET_NAME, USER_WALLET_PORT);
        walletPorts.put(DANCER_WALLET_NAME, DANCER_WALLET_PORT);
    }
}
