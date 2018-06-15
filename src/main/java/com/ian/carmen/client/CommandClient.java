package com.ian.carmen.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static com.ian.carmen.common.Connection.walletPorts;
import static com.ian.carmen.common.Validator.firstArgIsWalletName;

public class CommandClient {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public void startConnection(String ip, int port) throws IOException {
        clientSocket = new Socket(ip, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public String sendMessage(String msg) throws IOException {
        out.println(msg);
        String resp = in.readLine();
        return resp;
    }

    public void stopConnection() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 2) {
            System.out.println("Must provide both wallet name and message");
            return;
        }

        if (!firstArgIsWalletName(args)) {
            System.out.println("First arg must be wallet name");
            return;
        }

        final String walletName = args[0];
        final int walletPort = walletPorts.get(walletName);

        CommandClient client = new CommandClient();

        final String[] messageArr = Arrays.copyOfRange(args, 1, args.length);

        if (messageArr.length < 1) {
            System.out.println("Command must be specified");
            return;
        }

        final String messageToSend = String.join(" ", messageArr);

        client.startConnection("127.0.0.1", walletPort);
        System.out.println("Started connection to server\n");
        System.out.printf("Message to send: %n%s%n%n", messageToSend);

        String response = client.sendMessage(messageToSend);

        System.out.printf("Server Response: %n%s%n%n", response);

//        for (int i = 0; i < 5; i++) {
//            String response;
//            if (i == 0) {
//                response = client.sendMessage("hello server");
//            } else {
//                response = client.sendMessage("test");
//            }
//
//            System.out.println("Server Response: " + response);
//
//            TimeUnit.SECONDS.sleep(1);
//        }

        client.stopConnection();
        System.out.println("DONE");
    }
}