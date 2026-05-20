package com.simpledb.server;

import SqlParser.Antlr.SQLProcessor;
import SqlParser.QueriesStruct.ExecutionResult;
import SqlParser.QueriesStruct.QueryInterface;
import Yadro.DataStruct.DatabaseEngine;
import Yadro.DataStruct.Row;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ClientHandler implements Runnable {

    private static final int OID_INT4 = 23;
    private static final int OID_TEXT = 25;

    private final Socket socket;
    private final DatabaseEngine engine;

    public ClientHandler(Socket socket, DatabaseEngine engine) {
        this.socket = socket;
        this.engine = engine;
    }

    @Override
    public void run() {
        try (socket) {
            DataInputStream  in  = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            if (doStartup(in, out)) {
                messageLoop(in, out);
            }
        } catch (Exception ignored) {}
    }

    // ── startup ──────────────────────────────────────────────────────────────

    private boolean doStartup(DataInputStream in, DataOutputStream out) throws IOException {
        while (true) {
            int len      = in.readInt();
            int protocol = in.readInt();
            byte[] rest  = new byte[Math.max(0, len - 8)];
            in.readFully(rest);

            if (protocol == 80877103) {   // SSLRequest — reject without closing
                out.write('N');
                out.flush();
                continue;
            }
            break;
        }

        writeAuthOk(out);
        writeParamStatus(out, "server_version",    "14.0");
        writeParamStatus(out, "server_encoding",   "UTF8");
        writeParamStatus(out, "client_encoding",   "UTF8");
        writeParamStatus(out, "DateStyle",         "ISO, MDY");
        writeParamStatus(out, "TimeZone",          "UTC");
        writeParamStatus(out, "integer_datetimes", "on");
        writeBackendKeyData(out);
        writeReadyForQuery(out);
        out.flush();
        return true;
    }

    // ── message loop ─────────────────────────────────────────────────────────

    private void messageLoop(DataInputStream in, DataOutputStream out) throws IOException {
        while (true) {
            int type = in.read();
            if (type == -1) break;

            int    len  = in.readInt();
            byte[] body = new byte[Math.max(0, len - 4)];
            in.readFully(body);

            switch ((char) type) {
                case 'Q' -> handleSimpleQuery(body, out);
                case 'X' -> { return; }
                // Extended query protocol stubs (psycopg2 may use these)
                case 'P' -> { writeMsg(out, '1', new byte[0]); out.flush(); }  // ParseComplete
                case 'B' -> { writeMsg(out, '2', new byte[0]); out.flush(); }  // BindComplete
                case 'D' -> { /* Describe – skip */ }
                case 'E' -> { writeCommandComplete(out, ""); writeReadyForQuery(out); out.flush(); }
                case 'S' -> { writeReadyForQuery(out); out.flush(); }          // Sync
                default  -> { /* ignore unknown */ }
            }
        }
    }

    // ── simple query ─────────────────────────────────────────────────────────

    private void handleSimpleQuery(byte[] body, DataOutputStream out) throws IOException {
        String sql = stripNull(body).trim();

        if (sql.isEmpty()) {
            writeEmptyQueryResponse(out);
            writeReadyForQuery(out);
            out.flush();
            return;
        }

        String upper = sql.toUpperCase(Locale.ROOT);
        if (upper.startsWith("SET ") || upper.startsWith("SHOW ") || upper.startsWith("RESET ")) {
            writeCommandComplete(out, "SET");
            writeReadyForQuery(out);
            out.flush();
            return;
        }

        // Pass through BEGIN/COMMIT/ROLLBACK without touching engine transaction state
        if (upper.equals("BEGIN") || upper.startsWith("BEGIN ")
                || upper.startsWith("BEGIN;")
                || upper.equals("COMMIT") || upper.startsWith("COMMIT;")
                || upper.equals("ROLLBACK") || upper.startsWith("ROLLBACK;")) {
            String tag = upper.startsWith("COMMIT") ? "COMMIT"
                       : upper.startsWith("ROLLBACK") ? "ROLLBACK" : "BEGIN";
            writeCommandComplete(out, tag);
            writeReadyForQuery(out);
            out.flush();
            return;
        }

        try {
            ExecutionResult result;
            synchronized (engine) {
                QueryInterface q = SQLProcessor.getQuery(sql);
                if (q == null) throw new IllegalArgumentException("Parse error: " + sql);
                result = q.execute(engine);
            }

            if (!result.isSuccess()) {
                writeError(out, result.getMessage() != null ? result.getMessage() : "execution failed");
            } else {
                List<Row> rows = result.getRows();
                if (rows != null) {
                    sendSelectResult(out, rows);
                } else {
                    writeCommandComplete(out, buildTag(sql));
                }
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            writeError(out, msg != null ? msg : e.getClass().getSimpleName());
        }

        writeReadyForQuery(out);
        out.flush();
    }

    // ── result serialisation ─────────────────────────────────────────────────

    private void sendSelectResult(DataOutputStream out, List<Row> rows) throws IOException {
        if (rows.isEmpty()) {
            writeEmptyRowDescription(out);
            writeCommandComplete(out, "SELECT 0");
            return;
        }

        List<String> cols = new ArrayList<>(rows.get(0).getValuesMap().keySet());
        int[]        oids = inferOids(cols, rows);

        writeRowDescription(out, cols, oids);
        for (Row row : rows) writeDataRow(out, cols, row);
        writeCommandComplete(out, "SELECT " + rows.size());
    }

    private int[] inferOids(List<String> cols, List<Row> rows) {
        int[] oids = new int[cols.size()];
        for (int i = 0; i < cols.size(); i++) {
            String  col    = cols.get(i);
            boolean allInt = true;
            boolean seen   = false;
            for (Row r : rows) {
                String v = r.get(col);
                if (v == null) continue;
                seen = true;
                try {
                    long n = Long.parseLong(v);
                    // Only use OID_INT4 for values that fit in a 32-bit signed integer
                    if (n < Integer.MIN_VALUE || n > Integer.MAX_VALUE) { allInt = false; break; }
                } catch (NumberFormatException e) {
                    allInt = false; break;
                }
            }
            oids[i] = (seen && allInt) ? OID_INT4 : OID_TEXT;
        }
        return oids;
    }

    // ── low-level writers ────────────────────────────────────────────────────

    private void writeAuthOk(DataOutputStream out) throws IOException {
        out.write('R'); out.writeInt(8); out.writeInt(0);
    }

    private void writeParamStatus(DataOutputStream out, String name, String value) throws IOException {
        byte[] n = nts(name);
        byte[] v = nts(value);
        out.write('S');
        out.writeInt(4 + n.length + v.length);
        out.write(n);
        out.write(v);
    }

    private void writeBackendKeyData(DataOutputStream out) throws IOException {
        out.write('K'); out.writeInt(12); out.writeInt(1); out.writeInt(0);
    }

    private void writeReadyForQuery(DataOutputStream out) throws IOException {
        out.write('Z'); out.writeInt(5); out.write('I');
    }

    private void writeEmptyQueryResponse(DataOutputStream out) throws IOException {
        out.write('I'); out.writeInt(4);
    }

    private void writeEmptyRowDescription(DataOutputStream out) throws IOException {
        out.write('T'); out.writeInt(6); out.writeShort(0);
    }

    private void writeRowDescription(DataOutputStream out, List<String> cols, int[] oids) throws IOException {
        List<byte[]> names = new ArrayList<>();
        int size = 4 + 2;
        for (String c : cols) {
            byte[] b = nts(c);
            names.add(b);
            size += b.length + 4 + 2 + 4 + 2 + 4 + 2;
        }
        out.write('T');
        out.writeInt(size);
        out.writeShort(cols.size());
        for (int i = 0; i < cols.size(); i++) {
            out.write(names.get(i));
            out.writeInt(0);                                    // table OID
            out.writeShort(0);                                  // attribute number
            out.writeInt(oids[i]);                              // data-type OID
            out.writeShort(oids[i] == OID_INT4 ? 4 : -1);     // type size
            out.writeInt(-1);                                   // type modifier
            out.writeShort(0);                                  // format (text)
        }
    }

    private void writeDataRow(DataOutputStream out, List<String> cols, Row row) throws IOException {
        List<byte[]> vals = new ArrayList<>();
        int size = 4 + 2;
        for (String c : cols) {
            String v = row.get(c);
            byte[] b = (v != null) ? v.getBytes(StandardCharsets.UTF_8) : null;
            vals.add(b);
            size += 4 + (b != null ? b.length : 0);
        }
        out.write('D');
        out.writeInt(size);
        out.writeShort(cols.size());
        for (byte[] b : vals) {
            if (b == null) out.writeInt(-1);
            else           { out.writeInt(b.length); out.write(b); }
        }
    }

    private void writeCommandComplete(DataOutputStream out, String tag) throws IOException {
        byte[] t = nts(tag);
        out.write('C'); out.writeInt(4 + t.length); out.write(t);
    }

    private void writeError(DataOutputStream out, String message) throws IOException {
        byte[] sev  = "SERROR\0".getBytes(StandardCharsets.UTF_8);   // field 'S'
        byte[] code = "C42000\0".getBytes(StandardCharsets.UTF_8);   // field 'C' SQLSTATE
        byte[] msg  = ("M" + message + "\0").getBytes(StandardCharsets.UTF_8);
        int    len  = 4 + sev.length + code.length + msg.length + 1;
        out.write('E'); out.writeInt(len);
        out.write(sev); out.write(code); out.write(msg); out.write(0);
    }

    private void writeMsg(DataOutputStream out, char type, byte[] body) throws IOException {
        out.write(type); out.writeInt(4 + body.length); out.write(body);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Null-terminated UTF-8 bytes for a string. */
    private static byte[] nts(String s) {
        return (s + '\0').getBytes(StandardCharsets.UTF_8);
    }

    /** Strip trailing null byte from a query body. */
    private static String stripNull(byte[] body) {
        int len = body.length;
        if (len > 0 && body[len - 1] == 0) len--;
        return new String(body, 0, len, StandardCharsets.UTF_8);
    }

    private static String buildTag(String sql) {
        String u = sql.trim().toUpperCase(Locale.ROOT);
        if (u.startsWith("INSERT"))   return "INSERT 0 1";
        if (u.startsWith("UPDATE"))   return "UPDATE 0";
        if (u.startsWith("DELETE"))   return "DELETE 0";
        if (u.startsWith("CREATE"))   return "CREATE TABLE";
        if (u.startsWith("DROP"))     return "DROP TABLE";
        if (u.startsWith("ALTER"))    return "ALTER TABLE";
        if (u.startsWith("BEGIN"))    return "BEGIN";
        if (u.startsWith("COMMIT"))   return "COMMIT";
        if (u.startsWith("ROLLBACK")) return "ROLLBACK";
        return "OK";
    }
}
