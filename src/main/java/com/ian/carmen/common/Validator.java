package com.ian.carmen.common;

import static com.ian.carmen.common.Connection.walletPorts;

public class Validator {

    public static boolean firstArgIsWalletName(final String[] args) {
        if (args.length < 1) {
            return false;
        }

        final String potentialWalletName = args[0];

        for (final String walletName : walletPorts.keySet()) {
            if (walletName.equals(potentialWalletName))
                return true;
        }

        return false;
    }
}
