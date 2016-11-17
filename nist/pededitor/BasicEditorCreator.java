/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

class BasicEditorCreator {
    public BasicEditor run() {
        return new BasicEditor();
    }

    public String getProgramTitle() {
        return BasicEditor.PROGRAM_TITLE;
    }
}
