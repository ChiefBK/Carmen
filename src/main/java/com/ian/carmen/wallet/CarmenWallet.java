package com.ian.carmen.wallet;

import com.ian.carmen.common.Connection;
import com.subgraph.orchid.encoders.Hex;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.bitcoinj.core.*;
import org.bitcoinj.core.listeners.TransactionConfidenceEventListener;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.wallet.AllowUnconfirmedCoinSelector;
import org.bitcoinj.wallet.CoinSelector;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.KeyChainEventListener;
import org.bitcoinj.wallet.listeners.ScriptsChangeEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Scanner;

import static com.ian.carmen.common.Connection.DANCER_WALLET_NAME;
import static com.ian.carmen.common.Connection.USER_WALLET_NAME;
import static com.ian.carmen.common.Connection.walletPorts;

/*

TODO

- Set fee to 1 satoshi per byte

 */

public class CarmenWallet {
    private static String dancerAddress = null;
    private static Coin oneDollarOfBitcoins = null;

    public static void main(String[] args) throws IOException {
        // Qualify arguments
        if (args.length != 1) {
            System.out.println("Must specify wallet name as first argument");
            return;
        }

        final String walletName = args[0];

        if (!walletName.equals(DANCER_WALLET_NAME) && !walletName.equals(USER_WALLET_NAME)) {
            System.out.println("must specify valid wallet name as first argument");
            return;
        }

        final int walletPort = walletPorts.get(walletName);

        System.out.printf("Using wallet: %s%n", walletName);

        NetworkParameters params = RegTestParams.get();
        WalletAppKit kit = new WalletAppKit(params, new File("."), walletName);

        // Start synchronizing with nodes
        kit.startAsync();

        // While syncing get the amount of Bitcoin in 1 dollar
        try {
            oneDollarOfBitcoins = getOneDollarOfBitcoin();
        } catch (final Exception e) {
            System.out.println("Error while determining the amount of Bitcoin in 1 dollar");
            e.printStackTrace();
            return;
        }

        // Wait for sync
        kit.awaitRunning();


        kit.wallet().addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {
            @Override
            public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                System.out.println("-----> coins resceived: " + tx.getHashAsString());
                System.out.println("received: " + tx.getValue(wallet));
            }
        });

        kit.wallet().addCoinsSentEventListener(new WalletCoinsSentEventListener() {
            @Override
            public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                System.out.println("coins sent");
            }
        });

        kit.wallet().addKeyChainEventListener(new KeyChainEventListener() {
            @Override
            public void onKeysAdded(List<ECKey> keys) {
                System.out.println("new key added");
            }
        });

        kit.wallet().addScriptsChangeEventListener(new ScriptsChangeEventListener() {
            @Override
            public void onScriptsChanged(Wallet wallet, List<Script> scripts, boolean isAddingScripts) {
                System.out.println("new script added");
            }
        });

        kit.wallet().addTransactionConfidenceEventListener(new TransactionConfidenceEventListener() {
            @Override
            public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
                System.out.println("-----> confidence changed: " + tx.getHashAsString());
                TransactionConfidence confidence = tx.getConfidence();
                System.out.println("new block depth: " + confidence.getDepthInBlocks());
            }
        });

        System.out.printf("send money to: %s%n", kit.wallet().currentReceiveAddress().toString());

        // Create server and listeners to accept commands from client
        CommandServer server = new CommandServer();

        server.addListener(new CommandListener("getBalance") {
            @Override
            void commandReceived(final String[] args) {
                System.out.println("Getting balance");
                Coin balance = kit.wallet().getBalance();
                Coin unconfirmedBalance = kit.wallet().getBalance(AllowUnconfirmedCoinSelector.get());

                final String messageToSendBack = String.format("Balance: %s - Unconfirmed Balance: %s", balance.toFriendlyString(), unconfirmedBalance.toFriendlyString());
                server.sendMessage(messageToSendBack);
            }
        });

        server.addListener(new CommandListener("tip") {
            @Override
            void commandReceived(final String[] args) {
                // TODO
                final PeerGroup peerGroup = kit.peerGroup();
                final Wallet wallet = kit.wallet();

                if (dancerAddress == null) {
                    server.sendMessage("Must set dancer address before tipping");
                    return;
                }

                try {
                    System.out.printf("Sending tip to dancer address: %s%n", dancerAddress);
                    final Address address = Address.fromBase58(kit.params(), dancerAddress);

                    if (oneDollarOfBitcoins == null) {
                        throw new IllegalStateException("dollar worth of bitcoin needs to be set before tipping");
                    }

                    final Transaction tx = wallet.createSend(address, oneDollarOfBitcoins);
                    peerGroup.broadcastTransaction(tx);

                    server.sendMessage("tip broadcasted successfully");
                } catch (Exception e) {
                    server.sendMessage("exception while creating and sending tip");
                    e.printStackTrace();
                }

            }
        });

        server.addListener(new CommandListener("setDancerAddress") {
            @Override
            void commandReceived(final String[] args) {
                // TODO

                if (args.length != 1) {
                    server.sendMessage("setDancerAddress command must have one argument");
                    return;
                }

                dancerAddress = args[0];
                server.sendMessage(String.format("dancer address set to: %s", dancerAddress));

            }
        });

        server.start(walletPort);
    }

    /**
     * Determines the amount of Bitcoin in one dollar
     *
     * @return the amount
     * @throws IOException
     * @throws ParseException
     */
    public static Coin getOneDollarOfBitcoin() throws IOException, ParseException {
        // create request
        URL url = new URL("https://min-api.cryptocompare.com/data/price?fsym=BTC&tsyms=USD");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.connect();

        // handle status code of response
        int responseCode = con.getResponseCode();

        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Could not retrieve current price due to bad HTTP response");
        }

        // read response
        Scanner sc = new Scanner(url.openStream());
        StringBuilder response = new StringBuilder();
        while(sc.hasNext())
        {
            response.append(sc.nextLine());
        }

        sc.close();

        // create JSON object from response and get price
        JSONParser parse = new JSONParser();
        JSONObject obj = (JSONObject)parse.parse(response.toString());
        double price = (Double) obj.get("USD");
        double dollarWorthOfBitcoin = 1 / price;

        // Get number of satoshis in a dollar and return Coin equivalent
        double dollarWorthOfSatoshis = dollarWorthOfBitcoin * 100000000;
        long satoshis = Math.round(dollarWorthOfSatoshis);

        return Coin.SATOSHI.multiply(satoshis);
    }
}
