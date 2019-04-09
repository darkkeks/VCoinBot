package com.darkkeks.vcoin.bot;

import java.util.Map;
import java.util.TreeMap;

public class AccountStorage {

    private Map<Integer, VCoinClient> clients;

    public AccountStorage() {
        clients = new TreeMap<>();
    }

    public void update(VCoinClient client) {
        clients.put(client.getId(), client);
    }

    public void remove(VCoinClient client) {
        clients.remove(client.getId());
    }

    public int size() {
        return clients.size();
    }

    public Map<Integer, VCoinClient> getClients() {
        return clients;
    }
}
