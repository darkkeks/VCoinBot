package kek.darkkeks.twitch;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Strategy {

    private static final int INCOME_THRESHOLD = 25000;
    private static final int SHARE_THRESHOLD = 15000;
    private static final int TRANSFER_PACKET = 500000000;

    private int sinkUser;
    private ClientMonitor monitor;

    private VCoinClient biggestAccount;

    public Strategy(ClientMonitor monitor, ScheduledExecutorService executor) {
        this(-1, monitor, executor);
    }

    public Strategy(int sinkUser, ClientMonitor monitor, ScheduledExecutorService executor) {
        this.sinkUser = sinkUser;
        this.monitor = monitor;

        executor.scheduleAtFixedRate(monitor::print, 5, 5, TimeUnit.SECONDS);
    }

    private boolean hasBiggest() {
        return biggestAccount != null && biggestAccount.isActive();
    }

    public void onStart(VCoinClient client) {
        monitor.update(client);
    }

    public void onStop(VCoinClient client) {
        monitor.remove(client);
    }

    public void onStatusUpdate(VCoinClient client) {
        monitor.update(client);
        share(client);
        long currentIncome = client.getInventory().getIncome();
        if(currentIncome < INCOME_THRESHOLD) {
            buy(client);
        }
    }

    private void buy(VCoinClient client) {
        ItemStack item = client.getInventory().getBestItem();
        if(item != null) {
            if(client.getScore() >= item.getNextPrice()) {
                client.buyItem(item.getItem());
            }
        }
    }

    private void share(VCoinClient client) {
        if(client.getId() == sinkUser) {
            biggestAccount = client;
        }

        if(hasBiggest() && client.getId() != sinkUser) {
            if(client.getInventory().getIncome() < SHARE_THRESHOLD) {
                if(client.getScore() < TRANSFER_PACKET) {
					if(biggestAccount.getScore() >= TRANSFER_PACKET) {
                        biggestAccount.transfer(client.getId(), TRANSFER_PACKET);
                    }
                }
            }
        }
    }

    public void onTransferTick(VCoinClient client) {
        long currentIncome = client.getInventory().getIncome();
        if(currentIncome >= INCOME_THRESHOLD) {
            if(sinkUser != -1) {
                client.transfer(sinkUser, client.getScore());
            }
        }
    }
}
