import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CliTest {

    public static List<Runnable> suite() {
        return Arrays.asList(
            CliTest::testHelpFlag,
            CliTest::testVersionFlag,
            CliTest::testNoArgsShowsHelp,
            CliTest::testMissingSourceShowsError,
            CliTest::testMissingDestShowsError,
            CliTest::testCreatesDestinationDir,
            CliTest::testSourceEqualsDestError,
            CliTest::testCliOptionsParse,
            CliTest::testSuppressLogger
        );
    }

    // Capture stdout/stderr while running CLI
    static Capture runCli(String... args) {
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(outBytes, true, "UTF-8"));
            System.setErr(new PrintStream(errBytes, true, "UTF-8"));
            int exit = Cli.run(args);
            return new Capture(exit, outBytes.toString("UTF-8"), errBytes.toString("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } finally {
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
    }

    static class Capture {
        final int exit;
        final String out;
        final String err;
        Capture(int exit, String out, String err) {
            this.exit = exit; this.out = out; this.err = err;
        }
    }

    // ===== Tests =====

    static void testHelpFlag() {
        Capture c = runCli("--help");
        TestRunner.assertEquals(Cli.EXIT_OK, c.exit, "--help exit code = OK");
        TestRunner.assertTrue(c.out.contains("Usage:"), "--help shows Usage");
        TestRunner.assertTrue(c.out.contains("SchedulerFiles"), "--help shows app name");
    }

    static void testVersionFlag() {
        Capture c = runCli("--version");
        TestRunner.assertEquals(Cli.EXIT_OK, c.exit, "--version exit code = OK");
        TestRunner.assertTrue(c.out.contains("v"), "--version contains version: " + c.out);
        TestRunner.assertTrue(c.out.contains("SchedulerFiles"), "--version shows app name");
    }

    static void testNoArgsShowsHelp() {
        Capture c = runCli();
        TestRunner.assertEquals(Cli.EXIT_ERROR, c.exit, "no args exit code = ERROR");
        TestRunner.assertTrue(c.out.contains("Usage:"), "no args shows Usage");
    }

    static void testMissingSourceShowsError() {
        Capture c = runCli("-d", "/tmp");
        TestRunner.assertEquals(Cli.EXIT_ERROR, c.exit, "missing --source exit = ERROR");
        TestRunner.assertTrue(c.err.contains("--source"), "error mentions --source");
    }

    static void testMissingDestShowsError() {
        Capture c = runCli("-s", "/tmp");
        TestRunner.assertEquals(Cli.EXIT_ERROR, c.exit, "missing --dest exit = ERROR");
        TestRunner.assertTrue(c.err.contains("--dest"), "error mentions --dest");
    }

    static void testCreatesDestinationDir() {
        try {
            Path tmp = TestRunner.createTempDir("cliCreate_");
            Path nonExistentDest = tmp.resolve("new_subdir");
            TestRunner.assertFalse(Files.exists(nonExistentDest),
                "destination does not exist yet");

            // Run CLI with valid source but non-existent dest
            Capture c = runCli("-s", tmp.toString(), "-d", nonExistentDest.toString());
            TestRunner.assertTrue(c.out.contains("Created"),
                "CLI reports created directory");
            TestRunner.assertTrue(Files.exists(nonExistentDest),
                "destination directory was created");
            TestRunner.deleteRecursive(tmp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testSourceEqualsDestError() {
        try {
            Path tmp = TestRunner.createTempDir("cliSame_");
            Capture c = runCli("-s", tmp.toString(), "-d", tmp.toString());
            TestRunner.assertEquals(Cli.EXIT_ERROR, c.exit, "same source+dest = ERROR");
            TestRunner.assertTrue(c.err.contains("different"),
                "error mentions 'different'");
            TestRunner.deleteRecursive(tmp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testCliOptionsParse() {
        // Test parsing with source+dest (required for non-help mode)
        Cli.CliOptions opts = new Cli.CliOptions();
        TestRunner.assertTrue(
            opts.parse(new String[]{"--copy", "--verify-hash", "-s", "/tmp", "-d", "/tmp/out"}),
            "parse known flags returns true");
        TestRunner.assertTrue(opts.verifyHash, "verifyHash flag parsed");
        TestRunner.assertTrue(opts.copyMode, "copyMode=true (default)");

        // Test without source+dest returns false
        Cli.CliOptions optsNoPath = new Cli.CliOptions();
        TestRunner.assertFalse(optsNoPath.parse(new String[]{"--copy"}),
            "parse without source/dest returns false");

        // Test unknown flag
        Cli.CliOptions optsBad = new Cli.CliOptions();
        TestRunner.assertFalse(optsBad.parse(new String[]{"--unknown-flag"}),
            "unknown flag returns false");

        // Test help flag (no source/dest needed)
        Cli.CliOptions optsHelp = new Cli.CliOptions();
        TestRunner.assertTrue(optsHelp.parse(new String[]{"--help"}),
            "help flag returns true");
        TestRunner.assertTrue(optsHelp.help, "help flag parsed");
    }

    static void testSuppressLogger() {
        // Before setting, logger uses parent handlers
        java.util.logging.Logger log = java.util.logging.Logger.getLogger(MoveClass.class.getName());
        boolean hadParent = log.getUseParentHandlers();
        MoveClass.setSuppressLogger(true);
        TestRunner.assertFalse(log.getUseParentHandlers(),
            "suppressLogger disables parent handlers");
        MoveClass.setSuppressLogger(false);
    }
}
