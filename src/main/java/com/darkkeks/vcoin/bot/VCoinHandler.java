package com.darkkeks.vcoin.bot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class VCoinHandler extends ChannelInboundHandlerAdapter {

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

    private int requestId;
    private Map<Integer, CompletableFuture<String>> requests;
    private ScheduledFuture<?> updateTask;
    private Channel channel;

    private Controller controller;
    private int userId;
    private int missStreak;

    private Inventory inventory;
    private long score;
    private int place;
    private int randomId;
    private int tick;
    private int ccp;
    private boolean firstTime;

    public VCoinHandler(int userId, Controller controller) {
        this.userId = userId;
        this.controller = controller;
        requests = new HashMap<>();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        channel = ctx.channel();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if(msg instanceof String) {
            process((String) msg);
        } else {
            System.out.println("Invalid message: " + msg);
        }
    }

    private void process(String message) {
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
                        sendPacket(Packets.captcha(randomId, result));
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
                CompletableFuture<String> request = requests.get(id);
                if(request != null) {
                    request.complete(message.substring(message.indexOf(' ') + 1));
                }
                requests.remove(id);
                break;
            }
            default: {
                if(message.startsWith(ALREADY_CONNECTED)) {
                    close();
                } else if(message.startsWith(WAIT_FOR_LOAD)) {
                    // ignore
                } else if(message.startsWith(SELF_DATA)) {
                    missStreak = 0;
                    message = message.replaceFirst(SELF_DATA + " ", "");
                    String[] parts = message.split(" ");

                    place = Integer.valueOf(parts[0]);
                    score = Long.valueOf(parts[1]);
                    randomId = Integer.valueOf(parts[2]);

                    controller.onStatusUpdate(this);
                } else if(message.startsWith(BROKEN)) {
                    System.out.println(getId() + " BROKEN");
                    close();
                } else if(message.startsWith(MISS)) {
                    message = message.replaceFirst(MISS + " ", "");
                    randomId = Integer.valueOf(message);
                    if(++missStreak >= 10) {
                        System.out.println(getId() + " Critical MISS streak");
                        close();
                    }
                } else if(message.startsWith(TR)) {
                    message = message.replaceFirst(TR + " ", "");
                    String[] parts = message.split(" ");
                    long delta = Long.valueOf(parts[0]);
                    score += delta;
                    controller.onStatusUpdate(this);
                } else if(message.startsWith(MSG)) {
                    message = message.replaceFirst(MSG + " ", "");
                    System.out.println(message);
                    close();
                } else {
                    System.err.println("Unknown message: " + message);
                }
            }
        }
    }

    private void close() {
        if(channel.isOpen()) {
            requests.forEach((id, future) -> {
                future.completeExceptionally(new IOException("Connection closed"));
            });
            updateTask.cancel(true);
            controller.onStop(this);
            channel.close();
        }
    }

    private void startClient() {
        controller.onStart(this);

        updateTask = controller.getExecutor().scheduleAtFixedRate(() -> {
            int cnt = Math.min(random.nextInt(30) + 1, ccp);
            sendPacket(Packets.click(cnt, randomId));
        }, 2000, 1200, TimeUnit.MILLISECONDS);
    }

    public void buyItem(Item item) {
        sendRequest(Packets.buy(item)).thenAccept(response -> {
            JsonObject obj = parser.parse(response).getAsJsonObject();
            score = obj.get("score").getAsLong();
            inventory = new Inventory(obj.get("items"));
        });
    }

    public void transfer(int user, long amount) {
        score -= amount;
        sendRequest(Packets.transfer(user, amount)).thenAccept(response -> {
            JsonObject obj = parser.parse(response).getAsJsonObject();
            score = obj.get("score").getAsLong();
        });
    }

    public Optional<String> transferBlocking(int user, long amount) {
        score -= amount;
        String response = sendRequest(Packets.transfer(user, amount)).join();

        try {
            JsonObject obj = parser.parse(response).getAsJsonObject();
            score = obj.get("score").getAsLong();
            return Optional.empty();
        } catch (JsonParseException ex) {
            return Optional.of(response);
        }
    }

    private void sendPacket(Packet packet) {
        ++requestId;
        String msg = packet.serialize(requestId);
        channel.writeAndFlush(msg).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    private CompletableFuture<String> sendRequest(Packet packet) {
        sendPacket(packet);
        CompletableFuture<String> result = new CompletableFuture<>();
        requests.put(requestId, result);
        return result;
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
}
