package com.darkkeks.vcoin.bot;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

public class Main {

    private static final int PORT = 8080;
    private static final int MY_ID = 456171173;
    private static final String TOKEN_VARIABLE = "BOT_TOKEN";

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(4);

        String token = Optional.ofNullable(System.getenv(TOKEN_VARIABLE)).orElseGet(() -> {
            System.out.println("WARNING: no token in env variables, using \"debug\"");
            return "debug";
        });

        Controller controller = new Controller(MY_ID, executor);
        Starter starter = new Starter(controller, executor);
        WebServer server = new WebServer(PORT, token, controller);

        readUrls().stream().filter(url -> !url.startsWith("#")).map(Util::getWsUrl).forEach(starter::add);

        executor.submit(starter);
        server.start();
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

    private class Starter implements Runnable {

        private ScheduledExecutorService executor;
        private Controller controller;
        private BlockingDeque<String> accounts;

        public Starter(Controller controller, ScheduledExecutorService executor) {
            this.controller = controller;
            this.accounts = new LinkedBlockingDeque<>();
            this.executor = executor;
        }

        public void add(String account) {
            accounts.add(account);
        }

        @Override
        public void run() {
            while(true) {
                try {
                    String account = accounts.take();
                    new VCoinClient(account, controller, executor, () -> {
                        executor.schedule(() -> {
                            accounts.add(account);
                        }, 5, TimeUnit.MINUTES);
                    });
                } catch (InterruptedException e) {
                    System.out.println("Starter was interrupted, stopping");
                    break;
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
