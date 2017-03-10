/* Eric Boesch, NIST Materials Measurement Laboratory, 2017. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/** Stuff to help temporary storage of diagram state for do/undo. */
class EditorState {
    private static Point2D.Double clone(Point2D.Double p) {
        return (p == null) ? null : new Point2D.Double(p.x, p.y);
    }

    Diagram diagram;
    TransientState trans;
    static class TransientState {
        int selectionDecorationNum = -1;
        int selectionHandleNum = -1;
        Point2D.Double mprin = null;
        double scale = 0;

        @Override public TransientState clone() {
            TransientState res = new TransientState();
            res.selectionDecorationNum = selectionDecorationNum;
            res.selectionHandleNum = selectionHandleNum;
            res.mprin = EditorState.clone(mprin);
            res.scale = scale;
            return res;
        }
    }

    static class StringAndTransientState {
        String str;
        TransientState trans;
    }

    /**
     * After verifying that the hash codes match, copy the image bytes
     * from src to dest. This is a space saving measure, since storing
     * a multi-megabyte file with every copy would be expensive. */
    static void copyImagesBetween(Diagram src, Diagram dest) throws IOException {
        List<Decoration> srcds = src.decorations;
        List<Decoration> destds = dest.decorations;
        int srcIndex = 0;
        for (int destIndex = 0; destIndex < destds.size(); ++destIndex) {
            if (destds.get(destIndex) instanceof SourceImage) {
                SourceImage destImage = (SourceImage) destds.get(destIndex);
                for (;; ++srcIndex) {
                    if (srcIndex >= srcds.size()) {
                        throw new ImageMismatchException();
                    }
                    if (srcds.get(srcIndex) instanceof SourceImage) {
                        SourceImage srcImage = (SourceImage) srcds.get(srcIndex);
                        if (destImage.bytesHashCode != srcImage.bytesHashCode()) {
                            throw new IOException("Src/dest Images bytes hash code mismatch");
                        }
                        destImage.bytes = srcImage.bytes;
                        ++srcIndex;
                        break;
                    }
                }
            }
        }
        for (; srcIndex < srcds.size(); ++srcIndex) {
            if (srcds.get(srcIndex) instanceof SourceImage) {
                throw new ImageMismatchException();
            }
        }
    }

    void copyTo(BasicEditor target) throws IOException {
        copyImagesBetween(target, this.diagram);
        DecorationsAndHandle wrap = new DecorationsAndHandle();
        wrap.decorations = diagram.decorations;
        wrap.decorationNum = trans.selectionDecorationNum;
        wrap.handleNum = trans.selectionHandleNum;
        DecorationHandle sel = wrap.createHandle();

        // Preserve the target's undo stack and save hashes.
        int undoStackOffset = target.undoStackOffset;
        int lastSaveHashCode = target.lastSaveHashCode;
        int autoSaveHashCode = target.autoSaveHashCode;
        ArrayList<EditorState.StringAndTransientState> undoStack
            = target.undoStack;
        try {
            target.undoStack = new ArrayList<>();
            target.copyFrom(this.diagram);
            target.setSelection(sel);
            target.revalidateZoomFrame();
            target.lastSaveHashCode = lastSaveHashCode;
            target.autoSaveHashCode = autoSaveHashCode;
            // Restoring mprin and scale seems to be finicky.
            // setScale() works, but it's worse than useless if the
            // mouse is zoomed to the wrong spot.

            // target.mprin = clone(trans.mprin);
            // target.setScale( trans.scale;
            // target.centerMouse();
        } finally {
            target.undoStack = undoStack;
            target.undoStackOffset = undoStackOffset;
        }
    }

    public StringAndTransientState toStringAndTransientState()
        throws IOException {
        StringAndTransientState res = new StringAndTransientState();
        res.str = getObjectMapper().writeValueAsString(diagram);
        res.trans = trans.clone();
        return res;
    }

    /** @return this diagram as a JSON string. */
    public static StringAndTransientState toStringAndTransientState(
            BasicEditor editor) throws IOException {
        editor.resetIds();
        EditorState res = new EditorState();
        DecorationsAndHandle wrap = new DecorationsAndHandle();
        wrap.decorations = editor.decorations;
        wrap.saveHandle(editor.selection);
        res.diagram = editor;
        res.trans = new TransientState();
        res.trans.selectionDecorationNum = wrap.decorationNum;
        res.trans.selectionHandleNum = wrap.handleNum;
        res.trans.mprin = clone(editor.mprin);
        res.trans.scale = editor.scale;
        return res.toStringAndTransientState();
    }

    static EditorState loadFrom(StringAndTransientState sas) throws IOException {
        EditorState res = new EditorState();

        try {
            ObjectMapper mapper = getObjectMapper();
            res.diagram = (Diagram) mapper.readValue(sas.str, Diagram.class);
            res.trans = sas.trans.clone();
        } catch (Exception e) {
            throw new IOException("String parse error: " + e);
        }

        res.diagram.finishDeserialization();
        return res;
    }

    static void copyToEditor(BasicEditor editor, StringAndTransientState sas) throws IOException {
        String filename = editor.getFilename();
        loadFrom(sas).copyTo(editor);
        editor.setFilename(filename);
    }

    static ObjectMapper objectMapper = null;
    static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = Diagram.computeObjectMapper();
            objectMapper.configure(SerializationFeature.INDENT_OUTPUT, false);
            objectMapper.addMixIn(SourceImage.class, SourceImageHashAnnotations.class);
        }
        return objectMapper;
    }

    @SuppressWarnings("serial")
    static class ImageMismatchException extends IOException {
        ImageMismatchException() {
            super("Src/dest image count mismatch");
        }
    }
}

/** Tweak the serialization of SourceImage to substitute a hash code for the actual bytes. */
abstract class SourceImageHashAnnotations extends SourceImage {
    @Override @JsonProperty("bytesHashCode") int bytesHashCode() { return 0; }
    @Override @JsonIgnore protected byte[] getBytesUnsafe() { return null; }
    @Override @JsonIgnore protected void setBytesUnsafe(byte[] bytes) { }
}
