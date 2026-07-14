import java.util.*;

public class LengthFirstComparatorTest {

    public static List<Runnable> suite() {
        return Arrays.asList(
            LengthFirstComparatorTest::testShorterFirst,
            LengthFirstComparatorTest::testEqualLengthsConsistent,
            LengthFirstComparatorTest::testEqualStrings,
            LengthFirstComparatorTest::testSortTreeSet
        );
    }

    static void testShorterFirst() {
        LengthFirstComparator cmp = new LengthFirstComparator();
        int r = cmp.compare("abc", "abcdef");
        TestRunner.assertTrue(r < 0,
            "shorter string comes first: 'abc' length " + "abc".length()
            + " < 'abcdef' length " + "abcdef".length() + " (got " + r + ")");
    }

    static void testEqualLengthsConsistent() {
        LengthFirstComparator cmp = new LengthFirstComparator();
        // compare(a,b) and compare(b,a) must have opposite signs
        int ab = cmp.compare("alpha", "beta");
        int ba = cmp.compare("beta", "alpha");
        TestRunner.assertTrue(ab != 0 && ba != 0 && (ab > 0) != (ba > 0),
            "consistent ordering: compare(alpha,beta)=" + ab + " compare(beta,alpha)=" + ba);
    }

    static void testEqualStrings() {
        LengthFirstComparator cmp = new LengthFirstComparator();
        TestRunner.assertEquals(0, cmp.compare("same", "same"), "equal strings → 0");
    }

    static void testSortTreeSet() {
        TreeSet<String> set = new TreeSet<>(new LengthFirstComparator());
        set.add("longer-path/file.txt");
        set.add("a.txt");
        set.add("bb.txt");
        set.add("mid/file.dat");
        List<String> ordered = new ArrayList<>(set);
        TestRunner.assertEquals("a.txt", ordered.get(0), "first is shortest: a.txt");
        TestRunner.assertEquals("bb.txt", ordered.get(1), "second: bb.txt");
        TestRunner.assertTrue(ordered.get(2).length() >= ordered.get(1).length(),
            "non-decreasing length order: " + ordered.get(1) + " len=" + ordered.get(1).length()
            + " ≤ " + ordered.get(2) + " len=" + ordered.get(2).length());
    }
}
