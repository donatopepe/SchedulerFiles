import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Simple test runner. No JUnit required.
 * Each test class must have a static suite() method returning List<Runnable>.
 */
public class TestRunner {

    private static java.util.concurrent.atomic.AtomicInteger passed = new java.util.concurrent.atomic.AtomicInteger();
    private static java.util.concurrent.atomic.AtomicInteger failed = new java.util.concurrent.atomic.AtomicInteger();
    private static final List<String> failures = java.util.Collections.synchronizedList(new ArrayList<>());

    public static void assertEquals(Object expected, Object actual, String msg) {
        if (!Objects.equals(expected, actual)) {
            fail("assertEquals: " + msg + " — expected <" + expected + "> but got <" + actual + ">");
        } else {
            pass(msg);
        }
    }

    public static void assertTrue(boolean condition, String msg) {
        if (!condition) {
            fail("assertTrue: " + msg);
        } else {
            pass(msg);
        }
    }

    public static void assertFalse(boolean condition, String msg) {
        if (condition) {
            fail("assertFalse: " + msg);
        } else {
            pass(msg);
        }
    }

    public static void assertThrows(Class<? extends Exception> exClass, Runnable r, String msg) {
        try {
            r.run();
            fail("assertThrows: " + msg + " — no exception thrown, expected " + exClass.getSimpleName());
        } catch (Exception e) {
            if (exClass.isInstance(e)) {
                pass(msg + " (" + e.getClass().getSimpleName() + ")");
            } else {
                fail("assertThrows: " + msg + " — expected " + exClass.getSimpleName() + " but got " + e.getClass().getSimpleName());
            }
        }
    }

    private static void pass(String msg) {
        passed.incrementAndGet();
        System.out.println("  PASS  " + msg);
    }

    private static void fail(String msg) {
        failed.incrementAndGet();
        failures.add(msg);
        System.err.println("  FAIL  " + msg);
    }

    // --- Temp test directory helpers ---

    public static Path createTempDir(String prefix) throws IOException {
        return Files.createTempDirectory(prefix);
    }

    public static void deleteRecursive(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        // Retry up to 3 times with delay (Windows file locking)
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                Files.walk(dir)
                     .sorted(Comparator.reverseOrder())
                     .forEach(p -> {
                         try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                     });
                if (!Files.exists(dir)) return;
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        // Last attempt: throw if still not deleted
        if (Files.exists(dir)) {
            try (java.util.stream.Stream<Path> s = Files.list(dir)) {
                s.forEach(p -> System.err.println("  could not delete: " + p));
            }
        }
    }

    public static Path createFile(Path parent, String name, byte[] content) throws IOException {
        Path f = parent.resolve(name);
        Files.write(f, content);
        return f;
    }

    public static Path createFile(Path parent, String name, String content) throws IOException {
        return createFile(parent, name, content.getBytes("UTF-8"));
    }

    // --- Run a test class ---

    @SuppressWarnings("unchecked")
    public static void runTestClass(Class<?> testClass) {
        System.out.println("\n=== " + testClass.getSimpleName() + " ===");
        try {
            Method suiteMethod = testClass.getMethod("suite");
            List<Runnable> tests = (List<Runnable>) suiteMethod.invoke(null);
            for (Runnable t : tests) {
                try {
                    t.run();
                } catch (Exception e) {
                    fail(testClass.getSimpleName() + " threw: " + e);
                    e.printStackTrace();
                }
            }
        } catch (NoSuchMethodException e) {
            fail(testClass.getSimpleName() + " has no static suite() method");
        } catch (IllegalAccessException | InvocationTargetException e) {
            fail(testClass.getSimpleName() + " suite() error: " + e);
        }
    }

    // --- Main ---

    public static void main(String[] args) {
        System.out.println("============================================");
        System.out.println("  SchedulerFiles — Test Suite");
        System.out.println("============================================");

        runTestClass(HashUtilTest.class);
        runTestClass(LengthFirstComparatorTest.class);
        runTestClass(TextAreaOutputStreamTest.class);
        runTestClass(MoveClassTest.class);
        runTestClass(UpdaterTest.class);

        int p = passed.get(), f = failed.get();
        System.out.println("\n============================================");
        System.out.println("  Results: " + (p + f) + " tests, "
                           + p + " passed, " + f + " failed");
        System.out.println("============================================");

        if (!failures.isEmpty()) {
            System.out.println("\nFailures:");
            for (String failMsg : failures) {
                System.out.println("  - " + failMsg);
            }
            System.exit(1);
        }
    }
}
