package com.darkkeks.vcoin.bot.network;

import com.darkkeks.vcoin.bot.Util;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class VCoinHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(VCoinListener.class);

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

    private VCoinListener listener;
    private int userId;

    private long score;
    private int place;
    private int randomId;

    public VCoinHandler(int userId, VCoinListener listener) {
        this.userId = userId;
        this.listener = listener;
        requests = new HashMap<>();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        channel = ctx.channel();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if(msg instanceof String) {
            process((String) msg);
        } else {
            logger.info("Invalid message: " + msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        close();
    }

    private void process(String message) {
        logger.debug(message);
        switch (message.charAt(0)) {
            case '{': {
                JsonObject msg = parser.parse(message).getAsJsonObject();
                if(msg.get("type").getAsString().equals(INIT)) {
                    score = msg.get("score").getAsLong();
                    place = msg.get("place").getAsInt();
                    randomId = msg.get("randomId").getAsInt();

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
                    message = message.replaceFirst(SELF_DATA + " ", "");
                    String[] parts = message.split(" ");

                    place = Integer.valueOf(parts[0]);
                    score = Long.valueOf(parts[1]);
                    randomId = Integer.valueOf(parts[2]);

                    listener.onStatusUpdate(this);
                } else if(message.startsWith(BROKEN)) {
                    logger.error(getId() + " BROKEN");
                    close();
                } else if(message.startsWith(MISS)) {
                    message = message.replaceFirst(MISS + " ", "");
                    randomId = Integer.valueOf(message);
                } else if(message.startsWith(TR)) {
                    message = message.replaceFirst(TR + " ", "");
                    String[] parts = message.split(" ");
                    long delta = Long.valueOf(parts[0]);
                    int from = Integer.valueOf(parts[1]);
                    score += delta;
                    listener.onTransfer(this, delta, from);
                    listener.onStatusUpdate(this);
                } else if(message.startsWith(MSG)) {
                    message = message.replaceFirst(MSG + " ", "");
                    logger.info(message);
                    close();
                } else {
                    logger.info("Unknown message: " + message);
                }
            }
        }
    }

    private void close() {
        listener.onStop(this);
        requests.forEach((id, future) -> {
            future.completeExceptionally(new IOException("Connection closed"));
        });
        if(updateTask != null) {
            updateTask.cancel(true);
        }
        if(channel.isOpen()) {
            channel.close();
        }
    }

    private void startClient() {
        listener.onStart(this);

        updateTask = channel.eventLoop().scheduleAtFixedRate(() -> {
            updateScore();
            listener.onStatusUpdate(this);
        }, 2, 5, TimeUnit.SECONDS);
    }

    private void updateScore() {
        sendRequest(Packets.getScores(Collections.singletonList(getId()))).thenAccept(response -> {
            JsonObject obj = parser.parse(response).getAsJsonObject();
            score = obj.get(String.valueOf(getId())).getAsLong();
        });
    }

    public CompletableFuture<Optional<String>> transfer(int user, long amount) {
        score -= amount;
        return sendRequest(Packets.transfer(user, amount)).thenApply(response -> {
            try {
                JsonObject obj = parser.parse(response).getAsJsonObject();
                score = obj.get("score").getAsLong();
                return Optional.empty();
            } catch (JsonParseException ex) {
                return Optional.of(response);
            }
        });
    }

    private void sendPacket(Packet packet) {
        ++requestId;
        String msg = packet.serialize(requestId);
        logger.debug(msg);
        if(channel.isOpen()) {
            channel.writeAndFlush(msg)
                    .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }
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
}
