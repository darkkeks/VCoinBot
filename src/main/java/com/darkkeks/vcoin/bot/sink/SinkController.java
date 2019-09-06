package com.darkkeks.vcoin.bot.sink;

import com.darkkeks.vcoin.bot.network.VCoinHandler;
import com.darkkeks.vcoin.bot.network.VCoinListener;

public class SinkController implements VCoinListener {

    private static final int NO_USER = -1;

    private int sinkUser;
    private AccountStorage storage;

    private VCoinHandler biggestAccount;

    public SinkController(int sinkUser) {
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

    @Override
    public void onStart(VCoinHandler client) {
        storage.update(client);
        updateBiggest(client);
    }

    @Override
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

    @Override
    public void onStatusUpdate(VCoinHandler client) {
        storage.update(client);
        updateBiggest(client);
    }

    @Override
    public void onTransfer(VCoinHandler client, long delta, int from) {

    }

    public void sink(Long threshold, Long leave) {
        storage.getClients().values().forEach(client -> {
            if(client.getId() != sinkUser && sinkUser != NO_USER) {
                if(client.getScore() >= threshold) {
                    client.transfer(sinkUser, client.getScore() - leave);
                }
            }
        });
    }
}
