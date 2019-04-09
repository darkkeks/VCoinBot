package com.darkkeks.vcoin.bot;

import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Controller {

    private static final int NO_USER = -1;
    private static final int INCOME_THRESHOLD = 25_000;
    private static final int TRANSFER_THRESHOLD = 300_000_000;
    private static final int TRANSFER_LEAVE = 50_000_000;
    private static final int SHARE_PACKET = 500_000_000;

    private static final Set<Integer> blacklist;

    static {
        blacklist = new HashSet<>();
        blacklist.add(349519176);
        blacklist.add(410103684);
        blacklist.add(244896869);
        blacklist.add(191281578);
    }

    private int sinkUser;
    private AccountStorage storage;
    private ClientMonitor monitor;
    private ScheduledExecutorService executor;

    private VCoinHandler biggestAccount;

    public Controller() {
        this(NO_USER);
    }

    public Controller(int sinkUser) {
        this.sinkUser = sinkUser;
        this.storage = new AccountStorage();
        this.monitor = new ClientMonitor();
        this.executor = new ScheduledThreadPoolExecutor(8);

        executor.scheduleAtFixedRate(() -> {
            monitor.print(storage);
        }, 10, 5, TimeUnit.SECONDS);
    }

    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    public AccountStorage getStorage() {
        return storage;
    }

    public boolean hasBiggest() {
        return biggestAccount != null;
    }

    public VCoinHandler getBiggestAccount() {
        return biggestAccount;
    }

    public void onStart(VCoinHandler client) {
        storage.update(client);
    }

    public void onStop(VCoinHandler client) {
        storage.remove(client);
        if(biggestAccount == client) {
            biggestAccount = null;
        }
    }

    public void onStatusUpdate(VCoinHandler client) {
        storage.update(client);
        share(client);
        long currentIncome = client.getInventory().getIncome();
        if(currentIncome < INCOME_THRESHOLD) {
            buy(client);
        }
        onTransferTick(client);
    }

    private void buy(VCoinHandler client) {
        ItemStack item = client.getInventory().getBestItem();
        if(item != null) {
            if(client.getScore() >= item.getNextPrice()) {
                client.buyItem(item.getItem());
            }
        }
    }

    private void share(VCoinHandler client) {
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

    private void onTransferTick(VCoinHandler client) {
        if(blacklist.contains(client.getId())) return;

        long currentIncome = client.getInventory().getIncome();
        if(currentIncome >= INCOME_THRESHOLD) {
            if(client.getId() != sinkUser && sinkUser != NO_USER) {
                if(client.getScore() >= TRANSFER_THRESHOLD) {
                    client.transfer(sinkUser, client.getScore() - TRANSFER_LEAVE);
                }
            }
        }
    }
}
