package com.ian.carmen.wallet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandServer {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    private List<CommandListener> listeners = new ArrayList<>();

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        waitForConnection();

        String clientMessage;
        while (true) {
            clientMessage = in.readLine();
            if ("hello server".equals(clientMessage)) {
                sendMessage("hello client");
            } else if ("shutdown".equals(clientMessage)) {
                stop();
                return;
            } else if (clientMessage == null) {
                System.out.println("client disconnected");
                waitForConnection();
            } else {
                for (final CommandListener listener : listeners) {
                    // only trigger listeners who are listening for command sent by client
                    if (clientMessage.startsWith(listener.getCommand())) {
                        String[] messageArr = clientMessage.split(" ");
                        String[] onlyArgs = Arrays.copyOfRange(messageArr, 1, messageArr.length);
                        listener.commandReceived(onlyArgs);
                    }
                }
            }
        }
    }

    private void waitForConnection() throws IOException {
        System.out.println("waiting for client connection");
        clientSocket = serverSocket.accept();

        // set readers and writers for new client connection
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        System.out.println("client connected");
    }

    public void sendMessage(final String msg) {
        out.println(msg);
    }

    public void stop() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
        serverSocket.close();
    }

    public void addListener(final CommandListener listener) {
        listeners.add(listener);
    }

}
