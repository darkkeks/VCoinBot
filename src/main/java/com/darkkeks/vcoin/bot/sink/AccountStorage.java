package com.darkkeks.vcoin.bot.sink;

import com.darkkeks.vcoin.bot.network.VCoinHandler;

import java.util.Map;
import java.util.TreeMap;

public class AccountStorage {

    private Map<Integer, VCoinHandler> clients;

    public AccountStorage() {
        clients = new TreeMap<>();
    }

    public synchronized void update(VCoinHandler client) {
        clients.put(client.getId(), client);
    }

    public synchronized void remove(VCoinHandler client) {
        clients.remove(client.getId());
    }

    public synchronized Map<Integer, VCoinHandler> getClients() {
        return clients;
    }
}
