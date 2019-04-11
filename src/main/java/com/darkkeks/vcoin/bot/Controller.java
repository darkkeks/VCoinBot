package com.darkkeks.vcoin.bot;

import java.util.concurrent.atomic.AtomicLong;

public class Controller {

    private static final int NO_USER = -1;
    private static final int INCOME_THRESHOLD = 25_000;
    private static final int TRANSFER_THRESHOLD = 300_000_000;
    private static final int TRANSFER_LEAVE = 50_000_000;

    private int sinkUser;
    private AccountStorage storage;

    private VCoinHandler biggestAccount;

    public Controller() {
        this(NO_USER);
    }

    public Controller(int sinkUser) {
        this.sinkUser = sinkUser;
        this.storage = new AccountStorage();
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
        updateBiggest(client);
    }

    public void onStop(VCoinHandler client) {
        storage.remove(client);
        if(biggestAccount == client) {
            biggestAccount = null;
        }
    }

    private void updateBiggest(VCoinHandler client) {
        if(client.getId() == sinkUser) {
            biggestAccount = client;
        }
    }

    public void onStatusUpdate(VCoinHandler client) {
        storage.update(client);
        updateBiggest(client);
        onTransferTick(client, TRANSFER_THRESHOLD, TRANSFER_LEAVE);
    }

    private void onTransferTick(VCoinHandler client, long threshold, long leave) {
        long currentIncome = client.getInventory().getIncome();
        if(currentIncome >= INCOME_THRESHOLD) {
            if(client.getId() != sinkUser && sinkUser != NO_USER) {
                if(client.getScore() >= threshold) {
                    client.transfer(sinkUser, client.getScore() - leave);
                }
            }
        }
    }

    public long sink(Long threshold, Long leave) {
        AtomicLong result = new AtomicLong();
        storage.getClients().forEach((id, client) -> {
            onTransferTick(client, threshold, leave);
        });
        return result.get();
    }
}
