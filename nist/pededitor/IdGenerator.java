/* Eric Boesch, NIST Materials Measurement Laboratory, 2017. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

/** Simple class to generate sequence (ID) numbers. */ 
public class IdGenerator {
    private int maxUsedId = 0;

    public int id() {
        return ++maxUsedId;
    }

    public void idInUse(int id) {
        if (id > maxUsedId)
            maxUsedId = id;
    }

    public int getMaxUsedId() {
        return maxUsedId;
    }

    static private IdGenerator singleton = null;
    static public IdGenerator getInstance() {
        if (singleton == null) {
            singleton = new IdGenerator();
        }
        return singleton;
    }
}
