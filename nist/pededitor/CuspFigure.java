/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

/** A class for pairing a CuspInterp2D with its color, stroke, fill,
    and/or line width. */
public class CuspFigure extends DecorationHasInterp2D
    implements CurveClosable, Fillable {
    public CuspFigure() { }
    public CuspFigure(Interp2D curve) {
        super(curve);
    }

    public CuspFigure(CuspInterp2D curve, StandardStroke stroke) {
        super(curve, stroke);
    }

    public CuspFigure(Interp2D curve,
                              StandardStroke stroke,
                              double lineWidth) {
        super(curve, stroke, lineWidth);
    }

    public CuspFigure(Interp2D curve, StandardFill fill) {
        super(curve, fill);
    }

    @Override public CuspFigure clone() {
        CuspFigure res = new CuspFigure();
        res.copyFrom(this);
        return res;
    }

    @Override public Interp2DHandle[] getHandles
        (DecorationHandle.Type type) {
        if (type == DecorationHandle.Type.SELECTION
                && getCurve().size() >= 6) {
            // If this figure has many control points,
            // only select on the pointy control points.
            ArrayList<Interp2DHandle> res = new ArrayList<>();
            for (int i: getCurve().getCusps()) {
                res.add(createHandle(i));
            }
            return res.toArray(new Interp2DHandle[0]);
        }
        return super.getHandles(type);
    }

    // TODO Using a global here is a total hack.
    static boolean removeDuplicates = false;

    @Override public Interp2DHandle move(Interp2DHandle handle,
            Point2D dest) {
        if (removeDuplicates) {
            // Moving this point onto an adjacent control point
            // deletes the control point, since adjacent control
            // points cannot be duplicates of each other. However,
            // this can cause trouble if the adjacent point was
            // also going to be moved, and if a list of
            // VertexHandles was already generated with indexes
            // that would be invalidated by removing one. So
            // checking duplicates is not always correct.
            for (int i: getCurve().adjacentVertexes(handle.index)) {
                if (dest.equals(getCurve().get(i))) {
                    return null;
                }
            }
        }
        getCurve().set(handle.index, dest);
        return handle;
    }

    @Override @JsonProperty public CuspInterp2D getCurve() {
        return (CuspInterp2D) curve;
    }

    @JsonProperty public void setCurve(CuspInterp2D curve) {
        this.curve = curve;
    }

    /** For testing purposes only; could be safely deleted. */
    public static void main(String[] args) {
        String filename = "/eb/polyline-test.json";

        Point2D[] points1 = new Point2D[]
            { new Point2D.Double(3.1, 5.7),
              new Point2D.Double(0.0, 0.1) };
        Point2D[] points2 = new Point2D[]
            { points1[0], points1[1],
              new Point2D.Double(4.5, 1.2),
              new Point2D.Double(9.1, 10.1) };

        CuspInterp2D pol = new CuspInterp2D
                (Arrays.asList(points2),
                 Arrays.asList(true, true, false, true),
                 true);
        
        CuspFigure o = new CuspFigure(pol, null, 1.3);

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
            mapper.writeValue(new File(filename), o);
            CuspFigure o2 = mapper.readValue(new File(filename),
                                               CuspFigure.class);
            System.out.println(o2);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override public void setClosed(boolean b) {
        if (!b && isFilled()) {
            throw new IllegalArgumentException("Cannot turn off closure of a filled curve");
        }
        getCurve().setClosed(b);
    }

    @Override public void draw(Graphics2D g) {
        double oldWidth = getLineWidth();
        try {
            if (isRoundedStroke() && getFill() == null && getCurve().size() == 1) {
                // A legacy rule is that dots are shown at quadruple radius.
                setLineWidth(oldWidth * 4);
            }
            super.draw(g);
        } finally {
            setLineWidth(oldWidth);
        }
    }
}
