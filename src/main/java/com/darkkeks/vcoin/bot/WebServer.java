package com.darkkeks.vcoin.bot;

import com.google.gson.Gson;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.util.Headers;

import java.util.*;

public class WebServer {

    private static final int PORT = 8080;

    private Gson gson;
    private String token;
    private Undertow server;

    private Controller controller;

    public WebServer(String token, Controller controller) {
        this.token = token;
        this.gson = new Gson();
        this.controller = controller;
    }

    public void start() {
        ClassPathResourceManager resourceManager = new ClassPathResourceManager(
                getClass().getClassLoader(), "public/");

        server = Undertow.builder()
                .addHttpListener(PORT, "0.0.0.0")
                .setIoThreads(1)
                .setWorkerThreads(1)
                .setHandler(Handlers.predicate(exchange -> {
                            Cookie cookie = exchange.getRequestCookies().get("token");
                            return cookie != null && token.equals(cookie.getValue());
                        },
                        Handlers.path()
                                .addExactPath("/status", this::handleStatus)
                                .addExactPath("/transfer", this::handleTransfer)
                                .addExactPath("/sink", this::handleSink)
                                .addPrefixPath("/", Handlers.resource(resourceManager)),
                        Handlers.path()
                                .addPrefixPath(String.format("/%s", token), Handlers.path()
                                        .addExactPath("/status", this::handleStatus)
                                        .addExactPath("/transfer", this::handleTransfer)
                                        .addExactPath("/auth", new SetCookieHandler(
                                                "token", token, Handlers.redirect("/"))))
                ))
                .build();
        server.start();
    }

    private void handleStatus(HttpServerExchange exchange) {
        exchange.getResponseHeaders()
                .add(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender().send(gson.toJson(new BotStatus(controller.getStorage())));
    }

    private void handleSink(HttpServerExchange exchange) {
        exchange.getResponseHeaders()
                .add(Headers.CONTENT_TYPE, "application/json");

        Optional<Long> threshold = queryParam(exchange, "threshold").map(Long::parseLong);
        Optional<Long> leave = queryParam(exchange, "leave").map(Long::parseLong);

        if(threshold.isPresent() && leave.isPresent()) {
            long result = controller.sink(threshold.get(), leave.get());
            exchange.getResponseSender().send("Success " + (result / 1000.0));
        } else {
            exchange.getResponseSender().send("Missing params");
        }
    }

    private void handleTransfer(HttpServerExchange exchange) {
        exchange.getResponseHeaders()
                .add(Headers.CONTENT_TYPE, "application/json");

        Optional<Integer> to = queryParam(exchange, "to").map(Integer::parseInt);
        Optional<Long> amount = queryParam(exchange, "amount").map(Long::parseLong);

        TransferStatus result;
        if (to.isPresent() && amount.isPresent()) {
            if (controller.hasBiggest()) {
                VCoinHandler client = controller.getBiggestAccount();
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


    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private static class TransferStatus {

        private boolean success;
        private String message;
        public TransferStatus() {
            this.success = true;
            this.message = null;
        }

        public TransferStatus(String message) {
            this.success = false;
            this.message = message;
        }

    }

    @SuppressWarnings({"unused", "FieldCanBeLocal", "MismatchedQueryAndUpdateOfCollection"})
    private static class BotStatus {

        private long score;
        private long income;
        private List<AccountData> accounts;

        public BotStatus(AccountStorage storage) {
            accounts = new ArrayList<>();
            storage.getClients().forEach((id, client) -> {
                score += client.getScore();
                income += client.getInventory().getIncome();
                accounts.add(new AccountData(
                        client.getId(),
                        client.getInventory().getIncome(),
                        client.getScore(),
                        client.getPlace()));
            });
        }

        @SuppressWarnings({"unused", "FieldCanBeLocal"})
        private static class AccountData {

            private int id;
            private int place;
            private long income;
            private long score;
            public AccountData(int id, long income, long score, int place) {
                this.id = id;
                this.income = income;
                this.score = score;
                this.place = place;
            }

        }
    }

    private static class SetCookieHandler implements HttpHandler {

        private String name;
        private String value;
        private HttpHandler next;

        public SetCookieHandler(String name, String value, HttpHandler next) {
            this.name = name;
            this.value = value;
            this.next = next;
        }

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            exchange.setResponseCookie(new CookieImpl(name)
                    .setValue(value)
                    .setPath("/"));
            next.handleRequest(exchange);
        }
    }

}
