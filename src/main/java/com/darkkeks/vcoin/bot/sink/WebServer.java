package com.darkkeks.vcoin.bot.sink;

import com.darkkeks.vcoin.bot.network.VCoinHandler;
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

    private SinkController sinkController;

    public WebServer(String token, SinkController sinkController) {
        this.token = token;
        this.gson = new Gson();
        this.sinkController = sinkController;
    }

    public void start() {
        ClassPathResourceManager resourceManager = new ClassPathResourceManager(
                getClass().getClassLoader(), "public/");

        HttpHandler loginHandler = new SetCookieHandler("token", token, 228, Handlers.redirect("/"));
        HttpHandler logoutHandler = new SetCookieHandler("token", "", -1, Handlers.redirect("/"));

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
                                .addExactPath("/logout", logoutHandler)
                                .addPrefixPath("/", Handlers.resource(resourceManager)),
                        Handlers.path()
                                .addPrefixPath(String.format("/%s", token), Handlers.path()
                                        .addExactPath("/status", this::handleStatus)
                                        .addExactPath("/transfer", this::handleTransfer)
                                        .addExactPath("/auth", loginHandler))
                ))
                .build();
        server.start();
    }

    private void handleStatus(HttpServerExchange exchange) {
        exchange.getResponseHeaders()
                .add(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender().send(gson.toJson(new BotStatus(sinkController.getStorage())));
    }

    private void handleSink(HttpServerExchange exchange) {
        exchange.getResponseHeaders()
                .add(Headers.CONTENT_TYPE, "application/json");

        Optional<Long> threshold = queryParam(exchange, "threshold").map(Long::parseLong);
        Optional<Long> leave = queryParam(exchange, "leave").map(Long::parseLong);

        if(threshold.isPresent() && leave.isPresent()) {
            long result = sinkController.sink(threshold.get(), leave.get());
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
            if (sinkController.hasBiggest()) {
                VCoinHandler client = sinkController.getBiggestAccount();
                Optional<String> response = client.transfer(to.get(), amount.get()).join();
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
        private List<AccountData> accounts;

        public BotStatus(AccountStorage storage) {
            accounts = new ArrayList<>();
            storage.getClients().forEach((id, client) -> {
                score += client.getScore();
                accounts.add(new AccountData(
                        client.getId(),
                        client.getScore(),
                        client.getPlace()));
            });
        }

        @SuppressWarnings({"unused", "FieldCanBeLocal"})
        private static class AccountData {

            private int id;
            private int place;
            private long score;

            public AccountData(int id, long score, int place) {
                this.id = id;
                this.score = score;
                this.place = place;
            }
        }
    }

    private static class SetCookieHandler implements HttpHandler {

        private String name;
        private String value;
        private int expiry;
        private HttpHandler next;

        public SetCookieHandler(String name,
                                String value,
                                int expiry,
                                HttpHandler next) {
            this.name = name;
            this.value = value;
            this.expiry = expiry;
            this.next = next;
        }

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, expiry);
            exchange.setResponseCookie(new CookieImpl(name)
                    .setValue(value)
                    .setPath("/")
                    .setExpires(calendar.getTime()));
            next.handleRequest(exchange);
        }
    }

}
