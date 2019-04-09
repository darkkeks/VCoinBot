package com.darkkeks.vcoin.bot;

import com.google.gson.Gson;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.util.*;

public class WebServer {

    private int port;
    private Gson gson;
    private String token;
    private Undertow server;

    private Controller controller;

    public WebServer(int port, String token, Controller controller) {
        this.port = port;
        this.token = token;
        this.gson = new Gson();
        this.controller = controller;
    }

    public void start() {
        server = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                .setIoThreads(1)
                .setWorkerThreads(1)
                .setHandler(Handlers.path()
                        .addExactPath(String.format("/%s/status", token), this::handleStatus)
                        .addExactPath(String.format("/%s/transfer", token), this::handleTransfer))
                .build();
        server.start();
    }

    private void handleStatus(HttpServerExchange exchange) {
        exchange.getResponseHeaders()
                .add(HttpString.tryFromString("Content-Type"), "application/json")
                .add(HttpString.tryFromString("Access-Control-Allow-Origin"), "*");
        exchange.getResponseSender().send(gson.toJson(new BotStatus(controller.getStorage())));
    }

    private void handleTransfer(HttpServerExchange exchange) {
        exchange.getResponseHeaders()
                .add(HttpString.tryFromString("Content-Type"), "application/json")
                .add(HttpString.tryFromString("Access-Control-Allow-Origin"), "*");

        System.out.println(exchange.getQueryParameters());
        System.out.println(queryParam(exchange, "to"));
        System.out.println(queryParam(exchange, "amount"));
        Optional<Integer> to = queryParam(exchange, "to").map(Integer::parseInt);
        Optional<Long> amount = queryParam(exchange, "amount").map(Long::parseLong);
        System.out.println(to);
        System.out.println(amount);

        TransferStatus result;
        if (to.isPresent() && amount.isPresent()) {
            if (controller.hasBiggest()) {
                VCoinClient client = controller.getBiggestAccount();
                Optional<String> response = client.transferBlocking(to.get(), amount.get());
                result = response.map(TransferStatus::new).orElseGet(TransferStatus::new);
            } else {
                result = new TransferStatus("Missing sink account");
            }
        } else {
            result = new TransferStatus("Missing required params");
        }
        exchange.getResponseSender().send(gson.toJson(result));
    }

    private Optional<String> queryParam(HttpServerExchange exchange, String name) {
        return Optional.ofNullable(exchange.getQueryParameters().get(name))
                .map(Deque::getFirst);
    }


    @SuppressWarnings("unused")
    private static class TransferStatus {
        private boolean success;
        private String message;

        public TransferStatus() {
            this.success = true;
        }

        public TransferStatus(String message) {
            this.success = false;
            this.message = message;
        }
    }


    @SuppressWarnings("unused")
    private static class BotStatus {
        private long score;
        private int income;

        public BotStatus(AccountStorage storage) {
            storage.getClients().forEach((id, client) -> {
                score += client.getScore();
                income += client.getInventory().getIncome();
            });
        }
    }
}
