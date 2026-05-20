package com.simpledb.server;

import Yadro.DataStruct.DatabaseEngine;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PostgresServer {
    private final int port;
    private final DatabaseEngine engine;

    public PostgresServer(int port, DatabaseEngine engine) {
        this.port = port;
        this.engine = engine;
    }

    public void start() throws IOException {
        ExecutorService pool = Executors.newCachedThreadPool();
        try (ServerSocket srv = new ServerSocket(port)) {
            System.out.println("PostgreSQL-compatible server listening on port " + port);
            while (true) {
                Socket client = srv.accept();
                pool.submit(new ClientHandler(client, engine));
            }
        }
    }
}
