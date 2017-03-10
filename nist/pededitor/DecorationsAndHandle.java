/* Eric Boesch, NIST Materials Measurement Laboratory, 2017. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

class DecorationsAndHandle {
    @JsonProperty ArrayList<Decoration> decorations;
    /** Index of the decoration that is selected, or -1 if none. */
    @JsonProperty int decorationNum = -1;
    /** Index of the getHandles(CONTROL_POINT) that is equal to this handle. */
    @JsonProperty int handleNum = -1;

    void saveHandle(DecorationHandle hand) {
        decorationNum = -1;
        handleNum = -1;
        if (hand == null) {
            return;
        }
        if (hand instanceof Interp2DHandle2) {
            hand = ((Interp2DHandle2) hand).indexHandle();
        }
        Decoration hd = hand.getDecoration();

        int dnum = -1;
        for (Decoration d: decorations) {
            ++dnum;
            if (d != hd) {
                continue;
            }
            decorationNum = dnum;
            handleNum = 0;
            DecorationHandle[] hands = d.getHandles(
                    DecorationHandle.Type.CONTROL_POINT);
            for (int j = 0; j < hands.length; ++j) {
                if (hands[j].equals(hand)) {
                    handleNum = j;
                    break;
                }
            }
            break;
        }
    }

    DecorationHandle createHandle() {
        if (decorationNum < 0 || decorationNum >= decorations.size()) {
            return null;
        }
        Decoration d = decorations.get(decorationNum);
        DecorationHandle[] hands = d.getHandles(
                DecorationHandle.Type.CONTROL_POINT);
        if (handleNum < 0 || handleNum >= hands.length) {
            return null;
        }
        return hands[handleNum];
    }
}
