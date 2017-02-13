/* Eric Boesch, NIST Materials Measurement Laboratory, 2017. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

/** Simple class to generate sequence (ID) numbers. */ 
public class IdGenerator {
    static private int maxUsedId = 0;

    public static int id() {
        return ++maxUsedId;
    }

    public static void idInUse(int id) {
        if (id > maxUsedId)
            maxUsedId = id;
    }

    public static int getMaxUsedId() {
        return maxUsedId;
    }
}
