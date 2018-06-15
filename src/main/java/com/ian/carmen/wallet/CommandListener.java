package com.ian.carmen.wallet;

public abstract class CommandListener {
    private final String command;

    public CommandListener(final String cmdToListenFor) {
        command = cmdToListenFor;
    }

    public String getCommand() {
        return command;
    }

    abstract void commandReceived(final String[] args);
}
