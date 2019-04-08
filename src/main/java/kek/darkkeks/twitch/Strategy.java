package kek.darkkeks.twitch;

import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Strategy {

    private static final int INCOME_THRESHOLD = 25000;

    private static final int TRANSFER_THRESHOLD = 300 * 1000000;
    private static final int TRANSFER_LEAVE = 50 * 1000000;

    private static final int SHARE_PACKET = 500 * 1000000;

    private static final Set<Integer> blacklist;

    static {
        blacklist = new HashSet<>();
        blacklist.add(349519176);
        blacklist.add(410103684);
        blacklist.add(244896869);
        blacklist.add(191281578);
    }

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
        onTransferTick(client);
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
            if(client.getInventory().getIncome() < INCOME_THRESHOLD) {
                if(client.getScore() < SHARE_PACKET) {
                    if(biggestAccount.getScore() >= SHARE_PACKET) {
                        biggestAccount.transfer(client.getId(), SHARE_PACKET);
                    }
                }
            }
        }
    }

    public void onTransferTick(VCoinClient client) {
        if(blacklist.contains(client.getId())) return;

        long currentIncome = client.getInventory().getIncome();
        if(currentIncome >= INCOME_THRESHOLD) {
            if(client.getId() != sinkUser && sinkUser != -1) {
                if(client.getScore() >= TRANSFER_THRESHOLD) {
                    client.transfer(sinkUser, client.getScore() - TRANSFER_LEAVE);
                }
            }
        }
    }
}
