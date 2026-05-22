import FileWork.Binary.BinaryFileStorage;
import FileWork.FileManager;
import FileWork.PathManager;
import SqlParser.Antlr.SQLProcessor;
import SqlParser.QueriesStruct.QueryInterface;
import Yadro.DataStruct.DatabaseEngine;
import com.simpledb.server.PostgresServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Main {

    public static void main(String[] args) throws Exception {
        int    port     = 5433;
        String dbName   = "testdb";
        String dataDir  = "./db-data";
        String initFile = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port"     -> port    = Integer.parseInt(args[++i]);
                case "--dbname"   -> dbName  = args[++i];
                case "--data-dir" -> dataDir = args[++i];
                case "--init"     -> initFile = args[++i];
            }
        }

        PathManager.setBasePath(dataDir);

        BinaryFileStorage storage  = new BinaryFileStorage();
        FileManager       manager  = new FileManager(storage);
        DatabaseEngine    engine   = new DatabaseEngine(manager);

        try { engine.createDatabase(dbName); } catch (Exception ignored) {}
        engine.setCurrentDatabase(dbName);

        if (initFile != null) {
            System.out.println("Loading schema from: " + initFile);
            loadSql(initFile, engine);
            System.out.println("Schema loaded.");
        }

        new PostgresServer(port, engine).start();
    }

    private static void loadSql(String path, DatabaseEngine engine) throws Exception {
        String content = Files.readString(Path.of(path));
        List<String> stmts = splitStatements(content);

        for (String stmt : stmts) {
            String upper = stmt.trim().toUpperCase(Locale.ROOT);
            if (upper.startsWith("INSERT")) continue;
            try {
                QueryInterface q = SQLProcessor.getQuery(stmt);
                if (q != null) q.execute(engine);
            } catch (Exception e) {
                System.err.println("Init DDL warning [" + abbrev(stmt) + "]: " + e.getMessage());
            }
        }

        List<String> inserts = stmts.stream()
                .filter(s -> s.trim().toUpperCase(Locale.ROOT).startsWith("INSERT"))
                .toList();

        if (!inserts.isEmpty()) {
            System.out.println("Bulk-loading " + inserts.size() + " rows...");
            engine.beginTransaction();
            try {
                for (String stmt : inserts) {
                    try {
                        QueryInterface q = SQLProcessor.getQuery(stmt);
                        if (q != null) q.execute(engine);
                    } catch (Exception e) {
                        System.err.println("Init INSERT warning [" + abbrev(stmt) + "]: " + e.getMessage());
                    }
                }
                engine.commit();
                System.out.println("Bulk load committed.");
            } catch (Exception e) {
                try { engine.rollback(); } catch (Exception ignored) {}
                throw e;
            }
        }
    }

    private static List<String> splitStatements(String sql) {
        List<String>  result  = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean       inStr   = false;
        char          strChar = 0;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            if (inStr) {
                current.append(c);
                if (c == strChar && (i == 0 || sql.charAt(i - 1) != '\\')) inStr = false;
            } else if (c == '\'' || c == '"') {
                inStr   = true;
                strChar = c;
                current.append(c);
            } else if (c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                while (i < sql.length() && sql.charAt(i) != '\n') i++;
            } else if (c == ';') {
                String s = current.toString().trim();
                if (!s.isEmpty()) result.add(s);
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        String last = current.toString().trim();
        if (!last.isEmpty()) result.add(last);
        return result;
    }

    private static String abbrev(String s) {
        return s.length() > 60 ? s.substring(0, 60) + "…" : s;
    }
}
