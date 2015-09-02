/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.Box;
import javax.swing.JCheckBox;

/** Dialog to ask for the length of two axes. It can also have a
    checkbox to ask whether to show the original image. */
public class ImageDimensionDialog extends StringArrayDialog {
    private static final long serialVersionUID = -8426815422655006923L;
    JCheckBox showOriginalImage = new
        JCheckBox("Show original image");
    JCheckBox transparent = new JCheckBox("Transparent");

    ImageDimensionDialog(Frame owner) {
        super(owner,
              new String[] {"Width", "Height"},
              new String[] {"800", "600"},
              Stuff.htmlify("Enter maximum values for the width "
                            + "and height of the image in pixels."));
        setTitle("Image size");
        GridBagUtil gb = new GridBagUtil(panelBeforeOK);
        gb.endRowWith(Box.createVerticalStrut(6 /* pixels */));
        gb.addEast(transparent);
        gb.endRowWith(showOriginalImage);
    }

    /** Select whether to display the "Show original image"
        checkbox. */
    public void setShowOriginalImageVisible(boolean b) {
        showOriginalImage.setVisible(b);
    }

    public void setShowOriginalImage(boolean b) {
        showOriginalImage.setSelected(b);
    }

    public boolean isShowOriginalImage() {
        return showOriginalImage.isSelected();
    }

    /** Select whether to display the "Show original image"
        checkbox. */
    public void setTransparentVisible(boolean b) {
        panelBeforeOK.setVisible(b);
    }

    public void setTransparent(boolean b) {
        transparent.setSelected(b);
    }

    public boolean isTransparent() {
        return transparent.isSelected();
    }

    public void setDimension(Dimension d) {
        setTextAt(0, Integer.toString(d.width));
        setTextAt(1, Integer.toString(d.height));
    }

    public Dimension getDimension() throws NumberFormatException {
        int w, h;
        String str = null;
        try {
            str = getTextAt(0);
            w = Integer.parseInt(str);
        } catch (NumberFormatException x) {
            throw new NumberFormatException("Illegal width value '" + str + "'");
        }
        try {
            str = getTextAt(1);
            h = Integer.parseInt(str);
        } catch (NumberFormatException x) {
            throw new NumberFormatException("Illegal height value '" + str + "'");
        }
        return new Dimension(w,h);
    }

    public Dimension showModalDimension() throws NumberFormatException {
        String[] strs = showModalStrings();
        if (strs != null) {
            return getDimension();
        } else {
            return null;
        }
    }

    public static void main(String[] args) {
        ImageDimensionDialog dog = new ImageDimensionDialog(null);
        Dimension d = dog.showModalDimension();
        System.out.println(d);
    }
   
}
