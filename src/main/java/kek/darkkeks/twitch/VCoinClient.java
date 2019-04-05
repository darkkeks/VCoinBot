package kek.darkkeks.twitch;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.*;
import java.util.concurrent.*;

public class VCoinClient {

    private static final Random random = new Random();
    private static final JsonParser parser = new JsonParser();
    private static final String ALREADY_CONNECTED = "ALREADY_CONNECTED";
    private static final String WAIT_FOR_LOAD = "WAIT_FOR_LOAD";
    private static final String SELF_DATA = "SELF_DATA";
    private static final String BROKEN = "BROKEN";
    private static final String MISS = "MISS";
    private static final String TR = "TR";
    private static final String INIT = "INIT";

    private WSClient client;
    private ScheduledExecutorService executor;
    private int requestId;
    private Map<Integer, CompletableFuture<String>> requests;

    private int userId;
    private Strategy strategy;

    private Inventory inventory;
    private long score;
    private int place;
    private int randomId;
    private int tick;
    private int ccp;
    private boolean firstTime;

    private int missStreak;

    public VCoinClient(Strategy strategy, int userId) {
        this.userId = userId;
        executor = new ScheduledThreadPoolExecutor(1);
        requests = new HashMap<>();
        this.strategy = strategy;
    }

    public int getId() {
        return userId;
    }

    public int getPlace() {
        return place;
    }

    public long getScore() {
        return score;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void init(WSClient client) {
        this.client = client;
    }

    public void process(String message) {
        switch (message.charAt(0)) {
            case '{': {
                JsonObject msg = parser.parse(message).getAsJsonObject();
                if(msg.get("type").getAsString().equals(INIT)) {
                    score = msg.get("score").getAsLong();
                    place = msg.get("place").getAsInt();
                    randomId = msg.get("randomId").getAsInt();
                    tick = msg.get("tick").getAsInt();
                    ccp = msg.get("ccp").getAsInt();
                    firstTime = msg.get("firstTime").getAsBoolean();
                    inventory = new Inventory(msg.get("items"));

                    if(msg.has("pow")) {
                        String pow = msg.get("pow").getAsString();
                        int result = Util.evaluateJS(pow);
                        sendPacket(id -> String.format("C%d %d %d", id, randomId, result));
                    }

                    startClient();
                } else {
                    System.err.println("Unknown message type: " + message);
                }
                break;
            }
            case 'R': {
                message = message.substring(1);
                String[] parts = message.split(" ");
                int id = Integer.valueOf(parts[0]);
                requests.get(id).completeExceptionally(new IllegalStateException(message));
                requests.remove(id);
                break;
            }
            case 'C': {
                message = message.substring(1);
                String[] parts = message.split(" ");
                int id = Integer.valueOf(parts[0]);
                requests.get(id).complete(message.substring(message.indexOf(' ') + 1));
                requests.remove(id);
                break;
            }
            default: {
                if(message.startsWith(ALREADY_CONNECTED)) {
                    stop();
                } else if(message.startsWith(WAIT_FOR_LOAD)) {
                    // ignore
                } else if(message.startsWith(SELF_DATA)) {
                    missStreak = 0;
                    message = message.replaceFirst(SELF_DATA + " ", "");
                    String[] parts = message.split(" ");

                    place = Integer.valueOf(parts[0]);
                    score = Long.valueOf(parts[1]);
                    randomId = Integer.valueOf(parts[2]);

                    strategy.onStatusUpdate(this);
                } else if(message.startsWith(BROKEN)) {
                    stop();
                } else if(message.startsWith(MISS)) {
                    message = message.replaceFirst(MISS + " ", "");
                    randomId = Integer.valueOf(message);
                    if(++missStreak >= 10) {
                        stop();
                    }
                } else if(message.startsWith(TR)) {
                    message = message.replaceFirst(TR + " ", "");
                    String[] parts = message.split(" ");
                    long delta = Long.valueOf(parts[0]);
                    score += delta;
                    String from = parts[1];
                    System.err.printf("Transfer of %d coins from %s%n", delta, from);
                    strategy.onStatusUpdate(this);
                } else {
                    System.err.println("Unknown message: " + message);
                }
            }
        }
    }

    private void stop() {
        client.close();
        executor.shutdownNow();
    }

    public boolean isActive() {
        return !executor.isShutdown();
    }

    private void startClient() {
        strategy.onStart(this);

        executor.scheduleAtFixedRate(() -> {
            int cnt = Math.min(random.nextInt(30) + 1, ccp);
            sendPacket(id -> String.format("C%d %d %d", cnt, randomId, 1));
        }, 2000, 1200, TimeUnit.MILLISECONDS);

        executor.scheduleAtFixedRate(() -> {
            strategy.onTransferTick(this);
        }, 1, 3 + (int)(3 * Math.random()), TimeUnit.MINUTES);
    }

    public void buyItem(Item item) {
        sendPacket(id -> String.format("P%d B %s", id, item.getName())).thenAccept(response -> {
            JsonObject obj = parser.parse(response).getAsJsonObject();
            score = obj.get("score").getAsLong();
            inventory = new Inventory(obj.get("items"));
        });
    }

    public void transfer(int user, long amount) {
        score -= amount;
        sendPacket(id -> String.format("P%d T %d %d", id, user, amount)).thenAccept(response -> {
            JsonObject obj = parser.parse(response).getAsJsonObject();
            score = obj.get("score").getAsLong();
        });
    }

    private CompletableFuture<String> sendPacket(Packet packet) {
        CompletableFuture<String> result = new CompletableFuture<>();
        String msg = packet.serialize(requestId);
        client.send(msg);
        requests.put(requestId, result);
        requestId++;
        return result;
    }
}
