package kek.darkkeks.twitch;

public class Strategy {

    private static final int INCOME_THRESHOLD = 20000;
    private static final int TRANSFER_THRESHOLD = 500000;

    private int sinkUser;
    private VCoinClient biggestAccount;

    public Strategy() {
        this(-1);
    }

    public Strategy(int sinkUser) {
        this.sinkUser = sinkUser;
    }

    private boolean hasBiggest() {
        return biggestAccount != null && biggestAccount.isActive();
    }

    public void onStart(VCoinClient client) {
        log(client, "Accepted by strategy");
    }

    public void onStatusUpdate(VCoinClient client) {
        long currentIncome = client.getInventory().getIncome();
        log(client, String.format("Income -> %d, Score -> %d", currentIncome, client.getScore()));

        share(client);

        if(currentIncome < INCOME_THRESHOLD) {
            buy(client);
        }
    }

    private void share(VCoinClient client) {
        if(client.getId() == sinkUser) {
            biggestAccount = client;
        }

        if(hasBiggest() && client.getId() != sinkUser) {
            if(client.getInventory().getIncome() < INCOME_THRESHOLD) {
                long amount = biggestAccount.getScore() / 2;
                if(amount >= TRANSFER_THRESHOLD) {
                    biggestAccount.transfer(client.getId(), amount);
                }
            }
        }
    }

    private void buy(VCoinClient client) {
        ItemStack item = client.getInventory().getBestItem(client.getScore());
        if(item != null) {
            if(client.getScore() >= item.getNextPrice()) {
                client.buyItem(item.getItem());
            } else {
                log(client, String.format("Aiming for %s - %d/%d",
                        item.getItem().getName(), client.getScore(), item.getNextPrice()));
            }
        } else {
            log(client, "Everything is too expensive :(");
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

    private void log(VCoinClient client, String message) {
        System.out.printf("[%d] %s%n", client.getId(), message);
    }
}
