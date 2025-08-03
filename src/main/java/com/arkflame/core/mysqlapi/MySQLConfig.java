package com.arkflame.core.mysqlapi;

/**
 * A final class to hold MySQL database connection configuration.
 * This is compatible with Java 8.
 */
public final class MySQLConfig {
    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;

    public MySQLConfig(String host, int port, String database, String user, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getDatabase() { return database; }
    public String getUser() { return user; }
    public String getPassword() { return password; }
}