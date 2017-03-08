/* Eric Boesch, NIST Materials Measurement Laboratory, 2017. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/** Stuff to help temporary storage of diagram state for do/undo. */
class EditorState {
    @JsonProperty Diagram diagram;

    // TODO be more efficient about the image decorations.
    // WeakReference<SourceImage> ...?

    void copyTo(BasicEditor target) throws IOException {
        target.copyFrom(this.diagram);
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
        SourceImage img = editor.firstImage();
        // Retain the current firstImage() if there is one.
        int layer = (img == null) ? -1 : editor.getLayer(img);
        loadFrom(jsonString).copyTo(editor);
        if (img != null) {
            editor.addDecoration(layer, img);
        }
        editor.setFilename(filename);
    }

    static ObjectMapper objectMapper = null;
    static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = Diagram.computeObjectMapper();
            objectMapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        }
        return objectMapper;
    }
}

