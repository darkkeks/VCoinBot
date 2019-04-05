package kek.darkkeks.twitch;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class WSClient extends WebSocketClient {

    private VCoinClient client;
    private Runnable onClose;

    public WSClient(String url, VCoinClient client, Runnable onClose) throws URISyntaxException {
        super(new URI(url));
        this.client = client;
        this.onClose = onClose;
        client.init(this);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Connected");
    }

    @Override
    public void onMessage(String message) {
        client.process(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Closed");
        onClose.run();
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("Error");
        ex.printStackTrace();
    }
}
