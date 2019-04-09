package com.darkkeks.vcoin.bot;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class WSClient extends WebSocketClient {

    private VCoinClient client;

    public WSClient(String url, VCoinClient client) throws URISyntaxException {
        super(new URI(url));
        this.client = client;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Connected " + client.getId());
    }

    @Override
    public void onMessage(String message) {
        client.process(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Closed " + client.getId() + " " + reason + " " + code);
        client.stop();
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("Error");
        ex.printStackTrace();
    }
}
