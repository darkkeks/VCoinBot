package kek.darkkeks.twitch;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {

    private static final int MY_ID = 456171173;

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(4);

        Starter starter = new Starter(new Strategy(new ClientMonitor(), executor), executor);

        readUrls().stream().map(Util::getWsUrl).forEach(starter::add);

        executor.submit(starter);
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
        private Strategy strategy;
        private BlockingDeque<String> accounts;

        public Starter(Strategy strategy, ScheduledExecutorService executor) {
            this.strategy = strategy;
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
                    new VCoinClient(account, strategy, executor, () -> {
                        accounts.add(account);
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
