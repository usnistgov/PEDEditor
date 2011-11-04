package gov.nist.pededitor;

/** A CropEvent is triggered when the user indicates that their crop
    selection is final. This interface is used to listen to CropEvents
    emitted by a CropFrame. */
public interface DiagramSelectionEventListener {
    public void diagramSelected(DiagramSelectionEvent e);
}
