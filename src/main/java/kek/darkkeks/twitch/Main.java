package kek.darkkeks.twitch;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

public class Main {

    private static final int MY_ID = 456171173;

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        List<String> urls = readUrls();

        urls = urls.stream().map(Util::getWsUrl).collect(Collectors.toList());

        Starter starter = new Starter(new Strategy(MY_ID));

        urls.forEach(starter::add);

        starter.start();
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

    private class Starter extends Thread {

        private Strategy strategy;
        private BlockingDeque<String> accounts;

        public Starter(Strategy strategy) {
            this.strategy = strategy;
            this.accounts = new LinkedBlockingDeque<>();
        }

        public void add(String account) {
            accounts.add(account);
        }

        @Override
        public void run() {
            while(true) {
                try {
                    String account = accounts.take();
                    int userId = Util.extractUserId(account);
                    WSClient client = new WSClient(account, new VCoinClient(strategy, userId), () -> {
                        accounts.add(account);
                    });
                    client.connect();
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
