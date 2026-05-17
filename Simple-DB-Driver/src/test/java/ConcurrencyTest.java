import FileWork.FileManager;
import FileWork.FileStorage;
import FileWork.JSON.JsonFileStorage;
import FileWork.Metadata.ColumnMetadata;
import FileWork.PathManager;
import SqlParser.QueriesStruct.WhereCondition;
import Yadro.DataStruct.Column;
import Yadro.DataStruct.DataType;
import Yadro.DataStruct.DatabaseEngine;
import Yadro.DataStruct.Row;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrencyTest {

    private static final String TEST_DIR = "test_concurrency_db";
    private DatabaseEngine engine;

    @BeforeEach
    void setUp() throws Exception {
        deleteDirectory(new File(TEST_DIR));
        PathManager.setBasePath(TEST_DIR);
        FileStorage storage = new JsonFileStorage(TEST_DIR);
        FileManager fileManager = new FileManager(storage);
        engine = new DatabaseEngine(fileManager);

        engine.createDatabase("test_db");
        engine.setCurrentDatabase("test_db");
        engine.createTable("users", List.of(
                new ColumnMetadata("id",   DataType.INTEGER, 0, new ArrayList<>(), null),
                new ColumnMetadata("name", DataType.TEXT,    0, new ArrayList<>(), null)
        ));
    }

    @AfterEach
    void tearDown() {
        PathManager.reset();
        deleteDirectory(new File(TEST_DIR));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 1. Параллельные читатели не блокируют друг друга (shared read lock)
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    void concurrentSelectsShouldNotDeadlock() throws Exception {
        engine.insert("users", List.of("id", "name"), List.of("1", "Alice"));
        engine.insert("users", List.of("id", "name"), List.of("2", "Bob"));

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGun = new CountDownLatch(1);
        List<Future<List<Row>>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                startGun.await();
                return engine.select("users", null, true,
                        null, false, null, true);
            }));
        }

        startGun.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS), "Threads should finish within 5s");

        for (Future<List<Row>> f : futures) {
            List<Row> rows = f.get();
            assertEquals(2, rows.size(), "Every reader should see both rows");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. Параллельные вставки — данные не теряются (exclusive write lock)
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    void concurrentInsertsShouldPreserveAllData() throws Exception {
        int threadCount = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGun = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 1; i <= threadCount; i++) {
            final int id = i;
            futures.add(pool.submit(() -> {
                startGun.await();
                engine.insert("users",
                        List.of("id", "name"),
                        List.of(String.valueOf(id), "user" + id));
                return null;
            }));
        }

        startGun.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        for (Future<?> f : futures) f.get(); // re-throws any exception from threads

        List<Row> rows = engine.select("users", null, true,
                (WhereCondition) null, false, null, true);
        assertEquals(threadCount, rows.size(),
                "All concurrent inserts must be persisted; expected " + threadCount + " rows");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. ThreadLocal: транзакция потока A не видна потоку B
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    void transactionContextIsIsolatedPerThread() throws Exception {
        CountDownLatch txStarted = new CountDownLatch(1);
        CountDownLatch checked   = new CountDownLatch(1);
        AtomicBoolean threadBSeesTx = new AtomicBoolean(true);

        Thread threadA = new Thread(() -> {
            engine.beginTransaction();
            txStarted.countDown();
            try { checked.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            engine.rollback();
        });

        Thread threadB = new Thread(() -> {
            try {
                txStarted.await();
                threadBSeesTx.set(engine.isTransactionActive());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                checked.countDown();
            }
        });

        threadA.start();
        threadB.start();
        threadA.join(3000);
        threadB.join(3000);

        assertFalse(threadBSeesTx.get(),
                "Thread B must not see Thread A's transaction (ThreadLocal isolation)");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. 2PL: читатель ждёт, пока транзакция-писатель не закоммитит (write lock)
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    void readerBlocksUntilTransactionalWriterCommits() throws Exception {
        engine.insert("users", List.of("id", "name"), List.of("0", "existing"));

        CountDownLatch writerHoldsLock = new CountDownLatch(1);
        AtomicLong commitTime = new AtomicLong();
        AtomicLong readTime   = new AtomicLong();
        AtomicReference<List<Row>> readerResult = new AtomicReference<>();

        Thread writer = new Thread(() -> {
            try {
                engine.beginTransaction();
                // acquireWrite → write lock held for duration of transaction
                engine.insert("users", List.of("id", "name"), List.of("1", "Alice"));
                writerHoldsLock.countDown();   // reader may now try to acquire read lock
                Thread.sleep(200);             // hold lock long enough for reader to block
                commitTime.set(System.currentTimeMillis());
                engine.commit();               // releases write lock → reader unblocks
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Thread reader = new Thread(() -> {
            try {
                writerHoldsLock.await();       // make sure writer already holds lock
                // acquireRead → blocks until writer commits
                readerResult.set(engine.select("users", null, true,
                        (WhereCondition) null, false, null, true));
                readTime.set(System.currentTimeMillis());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        writer.start();
        reader.start();
        writer.join(5000);
        reader.join(5000);

        assertNotNull(readerResult.get(), "Reader must complete after writer commits");

        // Reader must finish no earlier than the writer committed (with 30ms tolerance)
        assertTrue(readTime.get() >= commitTime.get() - 30,
                "Reader (" + readTime.get() + "ms) must not complete before writer commits (" + commitTime.get() + "ms)");

        // Reader sees committed data (both rows)
        assertEquals(2, readerResult.get().size(),
                "Reader should see committed data after write lock is released");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Вспомогательный метод
    // ──────────────────────────────────────────────────────────────────────────
    private void deleteDirectory(File directory) {
        if (!directory.exists()) return;
        try (var stream = Files.walk(directory.toPath())) {
            stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            System.err.println("Не удалось очистить папку: " + e.getMessage());
        }
    }
}
