package com.darkkeks.vcoin.bot;

import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_FixedWidth;

import java.util.concurrent.atomic.AtomicLong;

public class ClientMonitor {

    private long lastScore;
    private long lastIncome;

    public void print(AccountStorage storage) {
        AsciiTable table = new AsciiTable();

        table.getRenderer().setCWC(new CWC_FixedWidth()
            .add(12)
            .add(20)
            .add(25)
            .add(8));

        table.addRule();
        table.addRow("Id", "Income", "Score", "Place");
        table.addRule();

        AtomicLong incomeSum = new AtomicLong();
        AtomicLong scoreSum = new AtomicLong();

        storage.getClients().forEach((id, client) -> {
            table.addRow(
                    id,
                    formatValue(client.getInventory().getIncome()),
                    formatValue(client.getScore()),
                    client.getPlace());
            scoreSum.addAndGet(client.getScore());
            incomeSum.addAndGet(client.getInventory().getIncome());
        });

        String totalIncome = formatValue(incomeSum.get()) + " " + delta(lastIncome, incomeSum.get());
        String totalScore = formatValue(scoreSum.get()) + " " + delta(lastScore, scoreSum.get());

        table.addRule();
        table.addRow("Total",
                totalIncome,
                totalScore,
                "");
        table.addRule();
        table.addRow("Active",
                storage.size(),
                "",
                "");

        lastIncome = incomeSum.get();
        lastScore = scoreSum.get();

        table.addRule();

        System.out.println(table.render());
    }

    private String delta(long old, long cur) {
        if(old == 0 || cur == old) return "";
        long delta = cur - old;
        if(delta < 0) return "(" + formatValue(delta) + ")";
        return "(+" + formatValue(delta) + ")";
    }

    private String formatValue(long val) {
        return String.format("%.3f", val / 1000.0);
    }
}
