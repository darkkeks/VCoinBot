package com.darkkeks.vcoin.bot;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Main {

    private static final int MY_ID = 456171173;
    private static final String TOKEN_VARIABLE = "BOT_TOKEN";

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        String token = Optional.ofNullable(System.getenv(TOKEN_VARIABLE)).orElseGet(() -> {
            System.out.println("WARNING: no token in env variables, using \"debug\"");
            return "debug";
        });

        Controller controller = new Controller(MY_ID);
        WebServer server = new WebServer(token, controller);
        WSFactory websocketFactory = new WSFactory(controller);

        server.start();

        readUrls().stream()
                .filter(url -> !url.startsWith("#"))
                .map(Util::getWsUrl)
                .map(this::toUri)
                .filter(Objects::nonNull)
                .forEach(websocketFactory::connect);
    }

    private List<String> readUrls() {
        List<String> result = new ArrayList<>();

        try {
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(
                    "urls.txt"))));

            while((line = reader.readLine()) != null) {
                result.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private URI toUri(String account) {
        try {
            return new URI(account);
        } catch (URISyntaxException | NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }
}
