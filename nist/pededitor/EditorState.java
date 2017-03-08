/* Eric Boesch, NIST Materials Measurement Laboratory, 2017. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/** Stuff to help temporary storage of diagram state for do/undo. */
class EditorState {
    @JsonProperty Diagram diagram;

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
        int undoStackOffset = target.undoStackOffset;
        ArrayList<String> undoStack = target.undoStack;
        try {
            target.undoStack = new ArrayList<>();
            target.copyFrom(this.diagram);
        } finally {
            target.undoStack = undoStack;
            target.undoStackOffset = undoStackOffset;
        }
    }
        
    /** @return this diagram as a JSON string. */
    public String toJsonString() throws IOException {
        return getObjectMapper().writeValueAsString(this);
    }
        
    /** @return this diagram as a JSON string. */
    public static String toJsonString(BasicEditor editor) throws IOException {
        editor.resetIds();
        EditorState res = new EditorState();
        res.diagram = editor;
        return res.toJsonString();
    }

    static EditorState loadFrom(String jsonString) throws IOException {
        EditorState res;

        try {
            ObjectMapper mapper = getObjectMapper();
            res = (EditorState) mapper.readValue(jsonString, EditorState.class);
            res.diagram.saveNeeded = true; // TODO be more sophisticated.
        } catch (Exception e) {
            throw new IOException("String parse error: " + e);
        }

        res.diagram.finishDeserialization();
        return res;
    }
    
    static void copyJsonStringToEditor(BasicEditor editor, String jsonString) throws IOException {
        String filename = editor.getFilename();
        loadFrom(jsonString).copyTo(editor);
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
