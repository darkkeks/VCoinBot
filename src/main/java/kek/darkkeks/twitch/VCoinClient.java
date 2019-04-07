package kek.darkkeks.twitch;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
    private static final String MSG = "MSG";

    private WSClient client;
    private ScheduledExecutorService executor;
    private int requestId;
    private Map<Integer, CompletableFuture<String>> requests;
    private ScheduledFuture<?> updateTask;
    private final Runnable onClose;
    private boolean stopped;

    private int userId;
    private Strategy strategy;
    private int missStreak;

    private Inventory inventory;
    private long score;
    private int place;
    private int randomId;
    private int tick;
    private int ccp;
    private boolean firstTime;

    public VCoinClient(String account, Strategy strategy, ScheduledExecutorService executor, Runnable onClose) throws URISyntaxException {
        userId = Util.extractUserId(account);
        this.strategy = strategy;
        this.executor = executor;
        this.onClose = onClose;
        requests = new HashMap<>();

        client = new WSClient(account, this);
        client.connect();
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
                        String result = Util.evaluateJS(pow);
                        sendPacket(id -> String.format("C%d %d %s", id, randomId, result));
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
                    System.out.println(getId() + " BROKEN");
                    stop();
                } else if(message.startsWith(MISS)) {
                    message = message.replaceFirst(MISS + " ", "");
                    randomId = Integer.valueOf(message);
                    if(++missStreak >= 10) {
                        System.out.println(getId() + " Critical MISS streak");
                        stop();
                    }
                } else if(message.startsWith(TR)) {
                    message = message.replaceFirst(TR + " ", "");
                    String[] parts = message.split(" ");
                    long delta = Long.valueOf(parts[0]);
                    score += delta;
                    strategy.onStatusUpdate(this);
                } else if(message.startsWith(MSG)) {
                    message = message.replaceFirst(MSG + " ", "");
                    System.out.println(message);
                    stop();
                } else {
                    System.err.println("Unknown message: " + message);
                }
            }
        }
    }

    public boolean isActive() {
        return !updateTask.isCancelled();
    }

    public void stop() {
        if(!stopped) {
            stopped = true;
            if(updateTask != null) {
                updateTask.cancel(true);
            }
            client.close();
            strategy.onStop(this);
            onClose.run();
        }
    }

    private void startClient() {
        strategy.onStart(this);

        updateTask = executor.scheduleAtFixedRate(() -> {
            int cnt = Math.min(random.nextInt(30) + 1, ccp);
            sendPacket(id -> String.format("C%d %d %d", cnt, randomId, 1));
        }, 2000, 1200, TimeUnit.MILLISECONDS);
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
