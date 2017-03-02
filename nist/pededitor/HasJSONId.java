/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import com.fasterxml.jackson.annotation.JsonInclude;

public interface HasJSONId {
    @JsonInclude(JsonInclude.Include.ALWAYS) int getJsonId();

    void setJsonId(int id);

    default void clearJsonId() { setJsonId(-1); }
}
