package com.darkkeks.vcoin.bot.stock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Properties;

public class Database {

    private static final Logger logger = LoggerFactory.getLogger(Database.class);

    private static final String NEW_TRANSACTION = "INSERT INTO transaction_log (\"from\", \"to\", amount) VALUES (?, ?, ?);";
    private static final String UPDATE_SCORE = "INSERT INTO user_scores (user_id, score) VALUES (?, ?) " +
            "ON CONFLICT (user_id) DO UPDATE SET score = user_scores.score + excluded.score";

    private Connection connection;

    private PreparedStatement new_transaction;
    private PreparedStatement update_score;


    public Database(String url, String username, String password) {
        try {
            Class.forName("org.postgresql.Driver");
            Properties properties = new Properties();
            properties.setProperty("user", username);
            properties.setProperty("password", password);
            properties.setProperty("ssl", "true");
            properties.setProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory");

            connection = DriverManager.getConnection(url, properties);
            logger.info("Connected to database");

            new_transaction = connection.prepareStatement(NEW_TRANSACTION);
            update_score = connection.prepareStatement(UPDATE_SCORE);
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean createTransaction(int from, int to, long amount) {
        try {
            new_transaction.setInt(1, from);
            new_transaction.setInt(2, to);
            new_transaction.setLong(3, amount);
            new_transaction.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public synchronized boolean addScore(int from, long delta) {
        try {
            update_score.setInt(1, from);
            update_score.setLong(2, delta);
            update_score.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
