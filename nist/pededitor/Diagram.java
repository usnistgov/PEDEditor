package gov.nist.pededitor;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.*;
import javax.swing.text.View;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Observable;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.DefaultFontMapper;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonSubTypes.Type;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.jsoup.Jsoup;

import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.dom.GenericDOMImplementation;

import org.w3c.dom.DOMImplementation;

/** Main class for Phase Equilibria Diagrams and their presentation,
    but not including GUI elements such as menus and windows. */
public class Diagram extends Observable implements Printable {
    static ObjectMapper objectMapper = null;
    protected static final DecimalFormat STANDARD_PERCENT_FORMAT
        = new DecimalFormat("##0.00%");

    /** Series of classes that implement the Decoration and
        DecorationHandle interfaces so that different types of
        decorations, such as curves and labels, can be manipulated the
        same way. */

    class VertexHandle implements BoundedParam2DHandle {
        CurveDecoration decoration;
        int vertexNo;

        @Override public BoundedParam2D getParameterization() {
            return getDecoration().getParameterization();
        }

        @Override public double getT() {
            return vertexNo;
        }

        @Override public CurveDecoration getDecoration() {
            return decoration;
        }

        VertexHandle(CuspFigure curve, int vertexNo) {
            this.decoration = new CurveDecoration(curve);
            this.vertexNo = vertexNo;
        }

        VertexHandle(CurveDecoration decoration, int vertexNo) {
            this.decoration = decoration;
            this.vertexNo = vertexNo;
        }

        public CuspFigure getItem() {
            return getDecoration().getItem();
        }

        @Override public VertexHandle remove() {
            CuspFigure path = getItem();
            int oldVertexCnt = path.size();

            if (oldVertexCnt >= 2) {
                ArrayList<Double> segments = getPathSegments(path);

                // While deleting this vertex, adjust t values that
                // reference this segment. Previous segments that
                // don't touch point are left alone; following
                // segments that don't touch point have their
                // segmentNo decremented; and the two segments that
                // touch point are combined into a single segment
                // number newSeg. What a pain in the neck!

                if (oldVertexCnt == 2) {
                    for (int i = 0; i < segments.size(); ++i) {
                        segments.set(i, (double) 0);
                    }
                } else {
                    int segCnt = path.getSegmentCnt();

                    Point2D point = path.get(vertexNo);
                    int prevSeg = (vertexNo > 0) ? (vertexNo - 1)
                        : path.isClosed() ? (segCnt-1) : -1;
                    Point2D previous = path.get((prevSeg >= 0) ? prevSeg : 0);
                    int nextSeg = vertexNo;
                    Point2D next = path.get
                        ((!path.isClosed()  && (vertexNo == oldVertexCnt - 1))
                         ? vertexNo
                         : (vertexNo + 1));
                    int newSeg = (vertexNo > 0) ? (vertexNo - 1)
                        : path.isClosed() ? (segCnt - 2)
                        : 0;
                    // T values for segments prevSeg and nextSeg should be
                    // combined into a single segment newSeg.

                    double dist1 = point.distance(previous);
                    double dist2 = point.distance(next);
                    double splitT = dist1 / (dist1 + dist2);

                    for (int i = 0; i < segments.size(); ++i) {
                        double t = segments.get(i);
                        int segment = (int) Math.floor(t);
                        double frac = t - segment;
                        if (segment == prevSeg) {
                            t = newSeg + frac * splitT;
                        } else if (segment == nextSeg) {
                            t = newSeg + splitT + frac * (1 - splitT);
                        } else if (segment > vertexNo) {
                            --t;
                        }
                        segments.set(i, t);
                    }
                }

                path.remove(vertexNo);
                setPathSegments(path, segments);
                propagateChange();
                return new VertexHandle(decoration, 
                                        (vertexNo > 0) ? (vertexNo - 1) : 0);
            } else {
                getDecoration().remove();
                return null;
            }
        }

        @Override public String toString() {
            return getClass().getSimpleName() + "[" + getItem() + ", " + vertexNo + "]";
        }

        @Override public void move(Point2D target) {
            CuspFigure path = getItem();
            path.set(vertexNo, target);
            propagateChange();
        }

        @Override public VertexHandle copy(Point2D dest) {
            Point2D.Double loc = getLocation();
            CurveDecoration dec = new CurveDecoration
                (getItem().createTransformed
                 (AffineTransform.getTranslateInstance
                  (dest.getX() - loc.x, dest.getY() - loc.y)));
            decorations.add(dec);
            propagateChange();
            return new VertexHandle(dec, vertexNo);
        }

        @Override public Point2D.Double getLocation() {
            return getItem().get(vertexNo);
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            if (getClass() != VertexHandle.class) return false;

            VertexHandle cast = (VertexHandle) other;
            return vertexNo == cast.vertexNo
                && getDecoration().equals(cast.getDecoration());
        }
    }

    
    class CurveDecoration implements Decoration, BoundedParameterizable2D {
        CuspFigure curve;

        CurveDecoration(CuspFigure curve) {
            this.curve = curve;
        }

        CurveDecoration
            (@JsonProperty("points") Point2D.Double[] points,
             @JsonProperty("smoothed") boolean[] smoothed,
             @JsonProperty("closed") boolean closed) {
            this(new CuspFigure(new CuspInterp2D(points, smoothed, closed),
                 null, 0));
        }

        @Override public void setLineWidth(double lineWidth) {
            getItem().setLineWidth(lineWidth);
            propagateChange();
        }

        @Override public double getLineWidth() {
            return getItem().getLineWidth();
        }

        @Override public void setLineStyle(StandardStroke lineStyle) {
            getItem().setStroke(lineStyle);
            propagateChange();
        }

        @Override public void setColor(Color color) {
            getItem().setColor(color);
            propagateChange();
        }

        @Override public Color getColor() {
            return getItem().getColor();
        }

        @JsonIgnore public Path2D.Double getShape() {
            return getItem().createTransformed(principalToStandardPage)
                .getPath();
        }

        @JsonIgnore public final CuspFigure getItem() {
            return curve;
        }
        @Override public CuspFigure getSerializationObject() { return getItem(); }

        @Override public DecorationHandle remove() {
            Diagram.this.remove(getItem());
            removeDecoration(this);
            return null;
        }

        @Override public void draw(Graphics2D g, double scale) {
            CuspFigure item = getItem();
            if (item.size() == 1
                && item.getStroke() != StandardStroke.INVISIBLE) {
                // Draw a dot.
                double r = item.getLineWidth() * 2 * scale;
                circleVertices(g, item, scale, true, r);
            } else {
                item.createTransformed(principalToScaledPage(scale))
                    .draw(g, scale * item.getLineWidth());
            }
        }

        @JsonIgnore @Override public BoundedParam2D getParameterization() {
            return PathParam2D.create(getShape());
        }

        @Override public String toString() {
            return getClass().getSimpleName() + "[" + getItem() + "]";
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            if (getClass() != CurveDecoration.class) return false;

            CurveDecoration cast = (CurveDecoration) other;
            return this.curve == cast.curve;
        }

        @JsonIgnore @Override public DecorationHandle[] getHandles() {
            ArrayList<DecorationHandle> output = new ArrayList<>();
            CuspFigure path = getItem();
            for (int j = 0; j < path.size(); ++j) {
                output.add(new VertexHandle(this, j));
            }
            return output.toArray(new DecorationHandle[0]);
        }

        @JsonIgnore @Override public DecorationHandle[] getMovementHandles() {
            return getHandles();
        }

        /** Return the VertexHandle closest to path(t). */
        public VertexHandle getHandle(double t) {
            BoundedParam2D c = getItem()
                .createTransformed(principalToStandardPage)
                .getParameterization();
            double ct = BoundedParam2Ds.getNearestVertex(c, t);
            return new VertexHandle(this, (int) ct);
        }
    }

    static enum LabelHandleType { CENTER, ANCHOR };

    class LabelHandle implements DecorationHandle {
        LabelDecoration decoration;
        /** A tie line may be selected from its anchor or its center. */
        LabelHandleType handle;

        @Override public  LabelDecoration getDecoration() {
            return decoration;
        }

        LabelHandle() {
            decoration = null;
            handle = null;
        }

        LabelHandle(LabelInfo info, LabelHandleType handle) {
            this.decoration = new LabelDecoration(info);
            this.handle = handle;
        }

        LabelHandle(LabelDecoration decoration, LabelHandleType handle) {
            this.decoration = decoration;
            this.handle = handle;
        }

        LabelInfo getItem() { return getDecoration().getItem(); }
        AnchoredLabel getLabel() { return getDecoration().getLabel(); }

        @Override public LabelHandle remove() {
            getDecoration().remove();
            return null;
        }

        @Override public void move(Point2D dest) {
            Point2D.Double destAnchor = getAnchorLocation(dest);
            AnchoredLabel item = getLabel();
            item.setX(destAnchor.getX());
            item.setY(destAnchor.getY());
            propagateChange();
        }

        @Override public LabelHandle copy(Point2D dest) {
            AnchoredLabel label = getLabel().clone();
            Point2D.Double destAnchor = getAnchorLocation(dest);
            label.setX(destAnchor.getX());
            label.setY(destAnchor.getY());
            LabelDecoration d = new LabelDecoration(new LabelInfo(label));
            decorations.add(d);
            propagateChange();
            return new LabelHandle(d, handle);
        }

        @Override public Point2D.Double getLocation() {
            switch (handle) {
            case ANCHOR:
                return getDecoration().getAnchorLocation();
            case CENTER:
                return getDecoration().getCenterLocation();
            }
            return null;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            if (getClass() != LabelHandle.class) {
                return false;
            }

            LabelHandle cast = (LabelHandle) other;
            return handle == cast.handle
                    && getDecoration().equals(cast.getDecoration());
        }

        public Point2D.Double getCenterLocation() {
            return getDecoration().getCenterLocation();
        }

        public Point2D.Double getAnchorLocation() {
            return getDecoration().getAnchorLocation();
        }

        /** @return the anchor location for a label whose handle is at
            dest. */
        public Point2D.Double getAnchorLocation(Point2D dest) {
            if (handle == LabelHandleType.ANCHOR) {
                return new Point2D.Double(dest.getX(), dest.getY());
            }

            // Compute the difference between the anchor
            // location and the center, and apply the same
            // difference to dest.
            Point2D.Double anchor = getAnchorLocation();
            Point2D.Double center = getCenterLocation();
            double dx = anchor.x - center.x;
            double dy = anchor.y - center.y;
            return new Point2D.Double(dest.getX() + dx, dest.getY() + dy);
        }

        @Override public String toString() {
            return getClass().getSimpleName() + "[" + getDecoration() + ", "
                + handle + "]";
        }
    }


    class LabelDecoration implements Decoration {
        LabelInfo label;

        public LabelDecoration() {
            this.label = null;
        }

        LabelDecoration(LabelInfo label) {
            this.label = label;
        }

        final LabelInfo getItem() { return label; }
        @Override public AnchoredLabel getSerializationObject() {
            return getLabel();
        }
        final AnchoredLabel getLabel() { return label.label; }

        @Override public void draw(Graphics2D g, double scale) {
            Diagram.this.draw(g, label, scale);
        }

        @Override public DecorationHandle remove() {
            for (Iterator<LabelInfo> it = new LabelInfoIterator();
                 it.hasNext();) {
                if (it.next() == label) {
                    it.remove();
                    propagateChange();
                    return null;
                }
            }
            throw new IllegalStateException("Could not locate " + label.label);
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            if (getClass() != LabelDecoration.class) return false;

            LabelDecoration cast = (LabelDecoration) other;
            return this.label == cast.label;
        }

        @JsonIgnore public Point2D.Double getCenterLocation() {
            initializeLabelInfo(label);
            Point2D.Double center = label.getCenter();
            if (center == null) {
                center = new Point2D.Double(0.5, 0.5);
                labelToScaledPage(label, 1.0).transform(center, center);
                scaledPageToPrincipal(1.0).transform(center, center);
                label.setCenter(center);
            }
            return (Point2D.Double) center.clone();
        }

        @JsonIgnore public Point2D.Double getAnchorLocation() {
            AnchoredLabel item = getLabel();
            return new Point2D.Double(item.getX(), item.getY());
        }

        @Override public void setLineWidth(double lineWidth) {
            // The capability to change box line width seems
            // unnecessary to me
        }

        @Override public double getLineWidth() {
            return 0.0;
            // What IS the line width for labels, anyhow?
        }

        @Override public void setLineStyle(StandardStroke lineStyle) {
            // The capability to change box line style seems
            // unnecessary to me
        }

        @Override public void setColor(Color color) {
            LabelInfo labelInfo = getItem();
            labelInfo.label.setColor(color);
            labelInfo.view = null;
        }

        @Override public Color getColor() { return getLabel().getColor(); }

        @JsonIgnore @Override public DecorationHandle[] getHandles() {
            AnchoredLabel label = getLabel();
            if (label.getXWeight() != 0.5 || label.getYWeight() != 0.5) {
                return new DecorationHandle[]
                    { new LabelHandle(this, LabelHandleType.ANCHOR),
                      new LabelHandle(this, LabelHandleType.CENTER) };
            } else {
                return new DecorationHandle[]
                    { new LabelHandle(this, LabelHandleType.ANCHOR) };
            }
        }

        /* Moving the anchor will move the center as well. */
        @JsonIgnore @Override public DecorationHandle[] getMovementHandles() {
            return new DecorationHandle[] { getHandles()[0] };
        }

        @Override public String toString() {
            return getClass().getSimpleName() + "[" + getLabel() + ")]";
        }
    }

    class ArrowDecoration implements Decoration, DecorationHandle {
        Arrow item;

        ArrowDecoration(Arrow item) {
            this.item = item;
        }

        ArrowDecoration(@JsonProperty("x") double x,
                        @JsonProperty("y") double y,
                        @JsonProperty("size") double size,
                        @JsonProperty("angle") double theta) {
            this(new Arrow(x, y, size, theta));
        }

        Arrow getItem() { return item; }
        @Override public Arrow getSerializationObject() { return getItem(); }

        /*
        // Nobody really cares to follow the outline of an arrow.

        public void getShape() {

            Point2D.Double p = principalToStandardPage.transform(arrow.x, arrow.y);
            return new Arrow
                (p.x, p.y, arrow.size, principalToPageAngle(arrow.theta));
        }
        */

        @Override public void draw(Graphics2D g, double scale) {
            Arrow arrow = getItem();
            Affine xform = principalToScaledPage(scale);
            Point2D.Double xpoint = xform.transform(arrow.x, arrow.y);
            Arrow arr = new Arrow
                (xpoint.x, xpoint.y, scale * arrow.size,
                 principalToPageAngle(arrow.theta));
            g.fill(arr);
        }

        @Override public ArrowDecoration remove() {
            removeDecoration(this);
            return null;
        }

        @Override public void move(Point2D dest) {
            Arrow item = getItem();
            item.x = dest.getX();
            item.y = dest.getY();
            propagateChange();
        }

        @Override public ArrowDecoration copy(Point2D dest) {
            Arrow arrow = getItem().clonus();
            arrow.x = dest.getX();
            arrow.y = dest.getY();
            ArrowDecoration res = new ArrowDecoration(arrow);
            decorations.add(res);
            propagateChange();
            return res;
        }

        @Override public void setLineWidth(double lineWidth) {
            getItem().size = lineWidth;
            propagateChange();
        }

        @Override public double getLineWidth() {
            return getItem().size;
        }

        @Override public void setLineStyle(StandardStroke lineStyle) {
            // Nothing to do here
        }

        @JsonIgnore @Override public Point2D.Double getLocation() {
            Arrow item = getItem();
            return new Point2D.Double(item.x, item.y);
        }

        @Override public void setColor(Color color) {
            propagateChange();
            getItem().setColor(color);
        }

        @Override public Color getColor() { return getItem().getColor(); }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            if (getClass() != ArrowDecoration.class) return false;

            ArrowDecoration cast = (ArrowDecoration) other;
            return item == cast.item;
        }

        @Override public ArrowDecoration getDecoration() {
            return this;
        }

        @Override public DecorationHandle[] getHandles() {
            return new DecorationHandle[] { this };
        }

        @Override public DecorationHandle[] getMovementHandles() {
            return getHandles();
        }
    }

    static enum TieLineHandleType { INNER1, INNER2, OUTER1, OUTER2 };

    class TieLineHandle implements DecorationHandle {
        TieLineDecoration decoration;
        /** A tie line is not a point object. It has up to four
         corners that can be used as handles to select it. */
        TieLineHandleType handle;

        TieLineHandle(TieLineDecoration decoration, TieLineHandleType handle) {
            this.decoration = decoration;
            this.handle = handle;
        }

        TieLineHandle(TieLine tieLine, TieLineHandleType handle) {
            this(new TieLineDecoration(tieLine), handle);
        }

        TieLine getItem() { return getDecoration().getItem(); }

        @Override public DecorationHandle remove() {
            return getDecoration().remove();
        }

        @Override public TieLineHandle copy(Point2D dest) {
            throw new UnsupportedOperationException
                ("Tie lines cannot be copied.");
        }

        @Override public Point2D.Double getLocation() {
            switch (handle) {
            case INNER1:
                return getItem().getInner1();
            case INNER2:
                return getItem().getInner2();
            case OUTER1:
                return getItem().getOuter1();
            case OUTER2:
                return getItem().getOuter2();
            }

            return null;
        }

        @Override public void move(Point2D dest) {
            // Tie line movement happens indirectly: normally,
            // everything at a key point moves at once, which means
            // that the control point that delimits the tie line moves
            // with it. No additional work is required here.
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            if (getClass() != TieLineHandle.class) return false;

            TieLineHandle cast = (TieLineHandle) other;
            return handle == cast.handle
                && getDecoration().equals(cast.getDecoration());
        }

        @Override public TieLineDecoration getDecoration() {
            return decoration;
        }
    }

    class TieLineDecoration implements Decoration {
        TieLine item;

        TieLineDecoration(TieLine item) {
            this.item = item;
        }

        TieLineDecoration() {
            this(null);
        }

        TieLine getItem() { return item; }
        @Override public TieLine getSerializationObject() { return getItem(); }

        @Override public void draw(Graphics2D g, double scale) {
            getItem().draw(g, getPrincipalToAlignedPage(), scale);
        }

        // I could return the tie line's outline, but nobody
        // cares. The inner and outer edges are already part of
        // the diagram, while the sides would be added if anyone
        // wants them.
        /*        @Override public void getShape() {

            return null;
            }
         */

        @Override public void setLineWidth(double lineWidth) {
            getItem().lineWidth = lineWidth;
            propagateChange();
        }

        @Override public double getLineWidth() {
            return getItem().lineWidth;
        }

        @Override public void setLineStyle(StandardStroke lineStyle) {
            getItem().stroke = lineStyle;
            propagateChange();
        }

        @Override public DecorationHandle remove() {
            removeDecoration(this);
            return null;
        }

        @Override public void setColor(Color color) {
            getItem().setColor(color);
            propagateChange();
        }

        @Override public Color getColor() { return getItem().getColor(); }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            if (getClass() != TieLineDecoration.class) return false;

            TieLineDecoration cast = (TieLineDecoration) other;
            return item == cast.item;
        }

        @Override public DecorationHandle[] getHandles() {
            ArrayList<TieLineHandle> output = new ArrayList<>();
            for (TieLineHandleType handle: TieLineHandleType.values()) {
                output.add(new TieLineHandle(this, handle));
            }
            return output.toArray(new TieLineHandle[0]);
        }

        /* Moving the curves these tie lines attach to will move the
           tie lines also, so return an empty array. */
        @Override public DecorationHandle[] getMovementHandles() {
            return new DecorationHandle[0];
        }
    }

    static enum RulerHandleType { START, END };

    class RulerHandle implements BoundedParam2DHandle {
        RulerDecoration decoration;
        /** Either end of the ruler may be used as a handle. */
        RulerHandleType handle; 

        RulerHandle(RulerDecoration decoration, RulerHandleType handle) {
            this.decoration = decoration;
            this.handle = handle;
        }

        RulerHandle(LinearRuler ruler, RulerHandleType handle) {
            this(new RulerDecoration(ruler), handle);
        }

        LinearRuler getItem() { return getDecoration().getItem(); }

        @Override public BoundedParam2D getParameterization() {
            return getDecoration().getParameterization();
        }

        @Override public double getT() {
            return (handle == RulerHandleType.START) ? 0 : 1;
        }

        @Override public DecorationHandle remove() {
            return getDecoration().remove();
        }

        @Override public void move(Point2D dest) {
            Point2D.Double d = new Point2D.Double(dest.getX(), dest.getY());

            switch (handle) {
            case START:
                getItem().startPoint = d;
                break;
            case END:
                getItem().endPoint = d;
                break;
            }

            propagateChange();
        }

        @Override public RulerHandle copy(Point2D dest) {
            Point2D.Double d = new Point2D.Double(dest.getX(), dest.getY());
            LinearRuler r = getItem().clone();
            double dx = r.endPoint.x - r.startPoint.x;
            double dy = r.endPoint.y - r.startPoint.y;
            
            switch (handle) {
            case START:
                r.startPoint = d;
                r.endPoint = new Point2D.Double(d.x + dx, d.y + dy);
                break;
            case END:
                r.endPoint = d;
                r.startPoint = new Point2D.Double(d.x - dx, d.y - dy);
                break;
            }

            RulerDecoration dec = new RulerDecoration(r);
            decorations.add(dec);
            propagateChange();
            return new RulerHandle(dec, handle);
        }

        @Override public Point2D.Double getLocation() {
            switch (handle) {
            case START:
                return (Point2D.Double) getItem().startPoint.clone();
            case END:
                return (Point2D.Double) getItem().endPoint.clone();
            }

            return null;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            if (getClass() != RulerHandle.class) return false;

            RulerHandle cast = (RulerHandle) other;
            return handle == cast.handle
                && getDecoration().equals(cast.getDecoration());
        }

        @Override public RulerDecoration getDecoration() {
            return decoration;
        }
    }

    class RulerDecoration implements Decoration, BoundedParameterizable2D {
        LinearRuler item;

        RulerDecoration(LinearRuler item) {
            this.item = item;
        }

        LinearRuler getItem() { return item; }
        @Override public LinearRuler getSerializationObject() { return getItem(); }

        @Override public void draw(Graphics2D g, double scale) {
            getItem().draw(g, getPrincipalToAlignedPage(), scale);
        }

        public Shape getShape() {
            LinearRuler item = getItem();
            Point2D.Double s = principalToStandardPage.transform(item.startPoint);
            Point2D.Double e = principalToStandardPage.transform(item.endPoint);
            return new Line2D.Double(s, e);
        }

        @Override public BoundedParam2D getParameterization() {
            return PathParam2D.create(getShape());
        }

        @Override public void setLineWidth(double lineWidth) {
            LinearRuler item = getItem();
            // Change fontSize too, to maintain a fixed ratio between
            // lineWidth and fontSize.
            double ratio  = lineWidth / item.lineWidth;
            item.lineWidth = lineWidth;
            item.fontSize *= ratio;
            propagateChange();
        }

        @Override public double getLineWidth() {
            return getItem().lineWidth;
        }

        @Override public void setLineStyle(StandardStroke lineStyle) {
            // Nothing to do here
        }

        @Override public DecorationHandle remove() {
            removeDecoration(this);
            return null;
        }

        @Override public void setColor(Color color) {
            propagateChange();
            getItem().setColor(color);
        }
        @Override public Color getColor() { return getItem().getColor(); }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            if (getClass() != RulerDecoration.class) return false;

            RulerDecoration cast = (RulerDecoration) other;
            return item == cast.item;
        }

        @Override public DecorationHandle[] getHandles() {
            ArrayList<RulerHandle> output = new ArrayList<>();
            for (RulerHandleType handle: RulerHandleType.values()) {
                output.add(new RulerHandle(this, handle));
            }
            return output.toArray(new DecorationHandle[0]);
        }

        @Override public DecorationHandle[] getMovementHandles() {
            return getHandles();
        }

        /** Return the VertexHandle closest to path(t). */
        public RulerHandle getHandle(double t) {
            return new RulerHandle
                (this,
                 (t <= 0.5) ? RulerHandleType.START : RulerHandleType.END);
        }
    }

    
    /** Apply the NIST MML PED standard binary diagram axis style. */
    static LinearRuler defaultBinaryRuler() {
        LinearRuler r = new LinearRuler();
        r.fontSize = normalFontSize();
        r.lineWidth = STANDARD_LINE_WIDTH;
        r.tickPadding = 3.0;
        r.drawSpine = true;
        return r;
    }


    /** Apply the NIST MML PED standard ternary diagram axis style. */
    static LinearRuler defaultTernaryRuler() {
        LinearRuler r = new LinearRuler();
        r.fontSize = normalFontSize();
        r.lineWidth = STANDARD_LINE_WIDTH;
        r.tickPadding = 3.0;
        r.multiplier = 100.0;

        r.tickType = LinearRuler.TickType.V;
        r.suppressStartTick = true;
        r.suppressEndTick = true;
        r.tickDelta = 0;

        r.drawSpine = true;
        return r;
    }

    class PathAndT {
        CuspFigure path;
        double t;

        PathAndT(CuspFigure path, double t) {
            this.path = path;
            this.t = t;
        }

        @Override public String toString() {
            return "PathAndT[" + path + ", " + t + "]";
        }
    }

    protected static final double BASE_SCALE = 615.0;

    // The label views are grid-fitted at the font size of the buttons
    // they were created from. Grid-fitting throws off the font
    // metrics, but the bigger the font size, the less effect
    // grid-fitting has, and the less error it induces. So make the
    // Views big enough that grid-fitting is largely irrelevant.
    private static final int VIEW_MAGNIFICATION = 8;
    static protected final String defaultFontName = "DejaVu LGC Sans PED";
    static protected final Map<String,String> fontFiles
        = new HashMap<String, String>() {
        private static final long serialVersionUID = -4018269657447031301L;

        {
            put("DejaVu LGC Sans PED", "DejaVuLGCSansPED.ttf");
            put("DejaVu LGC Serif PED", "DejaVuLGCSerifPED.ttf");
            put("DejaVu LGC Sans GRUMP", "DejaVuLGCSansGRUMP.ttf");
        }
    };

    // embeddedFont is initialized when needed.
    protected Font embeddedFont = null;
    protected Map<String,String> keyValues = null;
    protected Set<String> tags = new HashSet<>();

    /** Transform from original coordinates to principal coordinates.
        Original coordinates are (x,y) positions within a scanned
        image. Principal coordinates are either the natural (x,y)
        coordinates of a Cartesian graph or binary diagram (for
        example, y may equal a temperature while x equals the atomic
        fraction of the second diagram component), or the fraction
        of the right and top components respectively for a ternary
        diagram. */
    @JsonProperty protected PolygonTransform originalToPrincipal;
    protected PolygonTransform principalToOriginal;
    @JsonProperty protected AffinePolygonTransform principalToStandardPage;
    protected transient Affine standardPageToPrincipal;
    /** Bounds of the entire page in standardPage space. Use null to
        compute automatically instead. */
    protected Rectangle2D.Double pageBounds;
    protected DiagramType diagramType = null;
    protected ArrayList<Decoration> decorations;

    class LabelInfo {
        AnchoredLabel label;
        protected transient Point2D.Double center;
        protected transient View view;

        public View getView() {
            if (view == null) {
                view = toView(label);
            }
            return view;
        }

        public Point2D.Double getCenter() {
            return (center == null) ? null
                : new Point2D.Double(center.getX(), center.getY());
        }

        public void setCenter(Point2D p) {
            center = (p == null) ? null : new Point2D.Double(p.getX(), p.getY());
        }

        public LabelInfo(AnchoredLabel label) {
            this(label, null, null);
        }

        public LabelInfo(AnchoredLabel label, Point2D center,
                                 View view) {
            this.label = label;
            this.center = (center == null) ? null
                : new Point2D.Double(center.getX(), center.getY());
            this.view = view;
        }
    }

    @JsonProperty protected String[/* Side */] diagramComponents = null;

    /** All elements contained in diagram components, sorted into Hill
        order. Only trustworthy if componentElements != null. */
    protected transient String[] diagramElements = null;
    /** Diagram components expressed as vectors of element quantities.
        The element quantities are parallel to the diagramElements[]
        array. Set this to null whenever diagramComponents changes. */
    protected transient double[/* Side */][/* elementNo */]
        componentElements = null;

    protected String originalFilename;

    protected ArrayList<LinearAxis> axes = new ArrayList<>();

    protected double labelXMargin = Double.NaN;
    protected double labelYMargin = Double.NaN;

    static final double STANDARD_LINE_WIDTH = 0.0024;
    static final int STANDARD_FONT_SIZE = 15;
    protected String filename;
    private boolean usingWeightFraction = false;
    /** suppressUpdateCnt represents a count of the number of
        currently active orders to turn off the whole notification
        system. If suppressUpdateCnt > 0, changes are treated like no
        change. There are two reasons you may want to do this: 1) you
        are pushing through a whole bunch of changes and the diagram
        may temporarily be in an invalid state in the meantime, so you
        turn notifications off and then notify at the end; 2) you are
        making transient changes that will be undone later. */
    transient int suppressUpdateCnt = 0;
    transient boolean saveNeeded = false;

    /** If an UpdateSuppressor object is created, then all changes are
        treated like no change at all, until the object is closed
        again. */
    class UpdateSuppressor implements AutoCloseable {
        public UpdateSuppressor() {
            ++suppressUpdateCnt;
        }

        @Override public void close() {
            --suppressUpdateCnt;
        }
    }

    public Diagram() {
        init();
    }

    /** @return the filename that has been assigned to the PED format
        diagram output. */
    @JsonIgnore public String getFilename() {
        return filename;
    }

    private void init() {
        originalToPrincipal = null;
        originalFilename = null;
        principalToOriginal = null;
        principalToStandardPage = null;
        standardPageToPrincipal = null;
        pageBounds = null;
        decorations = new ArrayList<>();
        removeAllTags();
        removeAllVariables();
        diagramComponents = new String[Side.values().length];
        componentElements = null;
        filename = null;
        saveNeeded = false;
        embeddedFont = null;
        keyValues = new TreeMap<>();
    }

    /** Initialize/clear almost every field except diagramType. */
    void clear() {
        init();
    }

    // OBSOLESCENT
    @JsonProperty("curves") void setPaths(CuspFigure[] paths) {
        ArrayList<Decoration> figs = new ArrayList<>();
        for (CuspFigure path: paths) {
            figs.add(decorate(path));
        }
        decorations.addAll(0, figs);
    }

    // OBSOLESCENT
    @JsonProperty void setArrows(Arrow[] values) {
        for (Arrow v: values) {
            decorations.add(0, decorate(v));
        }
    }

    // OBSOLESCENT
    @JsonProperty void setLabels(AnchoredLabel[] values) {
        for (AnchoredLabel v: values) {
            decorations.add(decorate(v));
        }
    }

    CuspFigure idToCurve(int id) {
        for (CuspFigure path: paths()) {
            if (path.getJSONId() == id) {
                return path;
            }
        }
        System.err.println("No curve found with id " + id + ".");
        return null;
    }

    public String[] getTags() {
        return tags.toArray(new String[0]);
    }

    public boolean containsTag(String tag) {
        return tags.contains(tag);
    }

    public void removeAllTags() {
        while (tags.size() > 0) {
            removeTag(tags.iterator().next());
        }
    }

    public void removeAllVariables() {
        while (axes.size() > 0) {
            remove(axes.get(0));
        }
    }

    public void setTags(String[] newTags) {
        removeAllTags();
        for (String tag: newTags) {
            addTag(tag);
        }
    }

    public void addTag(String tag) {
        propagateChange();
        tags.add(tag);
    }

    public void removeTag(String tag) {
        propagateChange();
        tags.remove(tag);
    }

    public void removeVariable(String name) {
        for (LinearAxis axis: axes) {
            if (axis.name.equals(name)) {
                remove(axis);
                return;
            }
        }
        throw new IllegalStateException("Variable '" + name + "' + not found.");
    }

    @JsonIgnore public void setSaveNeeded(boolean b) {
        if (b) {
            propagateChange();
        } else {
            saveNeeded = false;
        }
    }

    @JsonIgnore public boolean getSaveNeeded() {
        return saveNeeded;
    }

    /** The returned value can be used to modify the
        internal key/value mapping. */
    @JsonProperty("keys") Map<String,String> getKeyValues() {
        return keyValues;
    }

    void setKeyValues(@JsonProperty("keys") Map<String, String> keyValues) {
        propagateChange();
        this.keyValues = keyValues;
    }

    /** Return the value associated with this key in the keyValues
        field. */
    public String get(String key) {
        return keyValues.get(key);
    }

    /** Put a key/value pair in the keyValues field. */
    public void put(String key, String value) {
        keyValues.put(key, value);
        propagateChange();
    }

    /** Remove the given key from the keyValues field, and return its
        value, or null if absent. */
    public String removeKey(String key) {
        String output = keyValues.remove(key);
        propagateChange();
        return output;
    }

    /** Return true if p1 and p2 are equal to within reasonable
        limits, where "reasonable limits" means the distance between
        their transformations to the standard page is less than
        1e-6. */
    boolean principalCoordinatesMatch(Point2D p1, Point2D p2) {
        Point2D.Double page1 = principalToStandardPage.transform(p1);
        Point2D.Double page2 = principalToStandardPage.transform(p2);
        return page1.distanceSq(page2) < 1e-12;
    }

    /** @param scale A multiple of standardPage coordinates

        @return a transform from principal coordinates to device
        coordinates. In additon to the scaling, the coordinates are
        translated to make (0,0) the top left corner. */
    Affine standardPageToDevice(double scale) {
        Affine output = Affine.getScaleInstance(scale, scale);
        output.translate(-pageBounds.x, -pageBounds.y);
        return output;
    }

    /** @param scale A multiple of standardPage coordinates

        @return a transform from principal coordinates to device
        coordinates. In additon to the scaling, the coordinates are
        translated to make (0,0) the top left corner. */
    Affine principalToScaledPage(double scale) {
        Affine output = standardPageToDevice(scale);
        output.concatenate(principalToStandardPage);
        return output;
    }

    Affine scaledPageToPrincipal(double scale) {
        try {
            return principalToScaledPage(scale).createInverse();
        } catch (NoninvertibleTransformException e) {
            System.err.println("p2sp = " + principalToStandardPage);
            System.err.println("p2scp = " + principalToScaledPage(scale));
            throw new IllegalStateException("Transform at scale " + scale
                                            + " is not invertible");
        }
    }

    /** setChanged() and then notifyObservers() */
    public void propagateChange1() {
        if (suppressUpdateCnt > 0) {
            return;
        }
        setChanged();
        notifyObservers(null);
    }

    /** Like propagateChange1(), but also set saveNeeded = true. */
    public void propagateChange() {
        if (suppressUpdateCnt > 0) {
            return;
        }
        saveNeeded = true;
        propagateChange1();
    }

    Rectangle scaledPageBounds(double scale) {
        return new Rectangle((int) 0, 0,
                             (int) Math.ceil(pageBounds.width * scale),
                             (int) Math.ceil(pageBounds.height * scale));
    }

    /** Compute the scaling factor to apply to pageBounds (and
        standardPage coordinates) in order for xform.transform(scale *
        pageBounds) to fill deviceBounds as much as possible without
        going over.
    */
    double deviceScale(AffineTransform xform, Rectangle2D deviceBounds) {
        AffineTransform itrans;
        try {
            itrans = xform.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalStateException("Transform " + xform
                                            + " is not invertible");
        }

        Point2D.Double delta = new Point2D.Double();
        itrans.deltaTransform
            (new Point2D.Double(pageBounds.width, pageBounds.height), delta);

        Rescale r = new Rescale(Math.abs(delta.x), 0.0, deviceBounds.getWidth(),
                                Math.abs(delta.y), 0.0, deviceBounds.getHeight());
        return r.t;
    }

    double deviceScale(Graphics2D g, Rectangle2D deviceBounds) {
        return deviceScale(g.getTransform(), deviceBounds);
    }

    public void paintDiagram(Graphics2D g, double scale, Color backColor) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);

        if (backColor != null && pageBounds.width > 0) {
            g.setColor(backColor);
            g.fill(scaledPageBounds(scale));
        }

        ArrayList<Decoration> decorations = getDecorations();

        for (Decoration decoration: decorations) {
            g.setColor(thisOrBlack(decoration.getColor()));
            decoration.draw(g, scale);
        }
    }

    /** Add a new vertex to path, located at point, and inserted just
        after vertex vertexNo. */
    public void add(CuspFigure path, int vertexNo,
                    Point2D.Double point, boolean smoothed) {
        if (path.size() == 0) {
            path.getCurve().add(0, point, smoothed);
            propagateChange();
            return;
        }

        ArrayList<Double> segments = getPathSegments(path);
        int segCnt = path.getSegmentCnt();

        double dist1 = (vertexNo == -1) ? 0
            : point.distance(path.get(vertexNo));
        double dist2 = (vertexNo == segCnt) ? 0
            : point.distance(path.get(vertexNo + 1));

        // For old segment vertexNo, map the t range [0, splitT] to
        // new segment vertexNo range [0,1], and map the t range
        // (splitT, 1] to new segment vertexNo+1 range [0,1]. If
        // vertexNo == segCnt then segment vertexNo never existed
        // before, so it doesn't matter what splitT value we use.
        double splitT = dist1 / (dist1 + dist2);

        for (int i = 0; i < segments.size(); ++i) {
            double t = segments.get(i);
            int segment = (int) Math.floor(t);
            double frac = t - segment;
            if (segment > vertexNo) {
                ++t;
            } else if (segment == vertexNo) {
                if (frac <= splitT) {
                    t = segment + frac / splitT;
                } else {
                    t = (segment + 1) + (frac - splitT) / (1.0 - splitT);
                }
            }
            segments.set(i, t);
        }

        path.getCurve().add(vertexNo+1, point, smoothed);
        setPathSegments(path, segments);
        propagateChange();
    }

    /** Make a list of t values that appear in this Diagram
        object and that refer to locations on the given path. */
    ArrayList<Double> getPathSegments(CuspFigure path) {
        ArrayList<Double> res = new ArrayList<>();

        // Tie Lines' limits are defined by t values.
        for (TieLine tie: tieLines()) {
            if (tie.innerEdge == path) {
                res.add(tie.it1);
                res.add(tie.it2);
            }
            if (tie.outerEdge == path) {
                res.add(tie.ot1);
                res.add(tie.ot2);
            }
        }

        return res;
    }

    /** You can change the segments returned by getPathSegments() and
        call setPathSegments() to make corresponding updates to the
        fields from which they came. */
    void setPathSegments(CuspFigure path, ArrayList<Double> segments) {
        int index = 0;
        for (TieLine tie: tieLines()) {
            if (tie.innerEdge == path) {
                tie.it1 = segments.get(index++);
                tie.it2 = segments.get(index++);
            }
            if (tie.outerEdge == path) {
                tie.ot1 = segments.get(index++);
                tie.ot2 = segments.get(index++);
            }
        }
    }

    /** Add an arrow with the given location, line width, and angle
        (expressed in principal coordinates). */
    public void addArrow(Point2D prin, double lineWidth, double theta) {
        decorations.add
            (new ArrowDecoration
             (new Arrow(prin.getX(), prin.getY(), lineWidth, theta)));
        propagateChange();
    }

    public void addDecoration(Decoration d) {
        decorations.add(d);
        propagateChange();
    }
        
    public void add(TieLine tie) {
        addDecoration(new TieLineDecoration(tie));
    }

    public void add(LinearRuler ruler) {
        addDecoration(new RulerDecoration(ruler));
    }

    public void add(LinearAxis axis) {
        String name = (String) axis.name;
        // Insert in order sorted by name.
        for (int i = 0; ; ++i) {
            if (i == axes.size()
                || name.compareTo((String) axes.get(i).name) < 0) {
                axes.add(i, axis);
                propagateChange();
                return;
            }
        }
    }

    public void rename(LinearAxis axis, String name) {
        axis.name = name;
        propagateChange();
    }

    public void remove(LinearAxis axis) {
        // Remove all rulers that depend on this axis.
        for (Iterator<LinearRuler> it = rulers().iterator(); it.hasNext();) {
            LinearRuler ruler = it.next();
            if (ruler.axis == axis) {
                it.remove();
            }
        }

        for (int i = 0; i < axes.size(); ) {
            if (axes.get(i) == axis) {
                axes.remove(i);
            } else {
                ++i;
            }
        }
        propagateChange();
    }

    public void remove(CuspFigure path) {
        // Remove all associated tie lines.
        for (int i = decorations.size() - 1; i >= 0; --i) {
            Decoration d = decorations.get(i);
            if (d instanceof TieLineDecoration) {
                TieLine tie = ((TieLineDecoration) d).getItem();
                if (tie.innerEdge == path || tie.outerEdge == path) {
                    decorations.remove(i);
                }
            }
        }
    }

    static class StringComposition {
        /** Decomposition of s into (element name, number of atoms)
            pairs. */
        Map<String,Double> c;
        /** The side (or corner, for ternary diagrams) of the diagram
            that this diagram component belongs to. */
        Side side;
    };

    /** See the documentation for the componentElements field. Returns
        a 2D array indexed by [Side][elementNo] that indicates the
        number of atoms of elementNo present in the component for the
        given Side. */
    @JsonIgnore double[][] getComponentElements() {
        if (componentElements != null || diagramType == null) {
            return componentElements;
        }

        ArrayList<StringComposition> scs = new ArrayList<>();

        for (Side side: Side.values()) {
            String dc = htmlToText(diagramComponents[side.ordinal()]);
            if (dc == null) {
                continue;
            }
            ChemicalString.Match m = ChemicalString.maybeQuotedComposition(dc);
            if (m == null) {
                System.out.println("Diagram component '" + dc + "' does not start\n"
                                   + "with a simple chemical formula.");
                continue;
            }
            StringComposition sc = new StringComposition();
            // sc.s = dc.substring(0, m.length);
            sc.c = m.composition;
            sc.side = side;
            scs.add(sc);
        }

        Map<String,Integer> elementIndexes = new HashMap<>();
        ArrayList<String> elements = new ArrayList<>();
        for (StringComposition sc: scs) {
            for (String element: sc.c.keySet()) {
                if (elementIndexes.containsKey(element)) {
                    continue;
                }
                // Don't worry about the value for now.
                elementIndexes.put(element, 0);
                elements.add(element);
            }
        }


        // Now we know which elements are present, in some order.
        // Rearrange to obtain Hill order.
        diagramElements = elements.toArray(new String[0]);
        ChemicalString.hillSort(diagramElements);
        for (int i = 0; i < diagramElements.length; ++i) {
            elementIndexes.put(diagramElements[i], i);
        }

        componentElements = new double[Side.values().length][];
        for (StringComposition sc: scs) {
            double[] compEls = new double[diagramElements.length];
            componentElements[sc.side.ordinal()] = compEls;
            for (Map.Entry<String, Double> pair: sc.c.entrySet()) {
                compEls[elementIndexes.get(pair.getKey())] = pair.getValue();
            }
        }
        return componentElements;
    }

    static class SideDouble {
        Side s;
        double d;

        SideDouble(Side s, double d) {
            this.s = s;
            this.d = d;
        }

        @Override public String toString() {
            return getClass().getSimpleName() + "[" + s + ", " + d + "]";
        }
    }

    /** Assuming that the principal coordinates are defined as mole
        percents (or in the case of binary diagrams, the X coordinate
        only is define as mole percent), return the mole fractions of
        the various diagram components at point prin, or null if the
        fractions could not be determined.
    */
    protected SideDouble[] componentFractions(Point2D prin) {
        if (prin == null || diagramType == null) {
            return null;
        }
        double x = prin.getX();
        double y = prin.getY();

        LinearAxis leftAxis = getLeftAxis();
        if (isTernary()) {
            double leftFraction = (leftAxis != null) ? leftAxis.value(x,y) :
                (1 - x - y);
            return new SideDouble[] {
                new SideDouble(Side.RIGHT, x),
                new SideDouble(Side.TOP, y),
                new SideDouble(Side.LEFT, leftFraction) };
        } else if (diagramComponents[Side.RIGHT.ordinal()] != null) {
            double leftFraction = (leftAxis != null) ? leftAxis.value(x,y) :
                (1 - x);
            return new SideDouble[] {
                new SideDouble(Side.RIGHT, x),
                new SideDouble(Side.LEFT, leftFraction) };
        } else {
            return null;
        }
    }

    protected Side[] sidesThatCanHaveComponents() {
        if (isTernary()) {
            return new Side[] { Side.LEFT, Side.RIGHT, Side.TOP };
        } else {
            return new Side[] { Side.LEFT, Side.RIGHT };
        }
    }

    protected boolean componentsSumToOne(SideDouble[] sds) {
        double sum = 0;
        for (SideDouble sd: sds) {
            sum += sd.d;
        }
        return Math.abs(1 - sum) < 1e-4;
    }

    /** Assuming that diagram's principal coordinates are mole
        fractions, return the weight fractions of the various diagram
        components at point prin, or null if the fractions could not
        be determined. */
    protected SideDouble[] componentWeightFractions(Point2D prin) {
        SideDouble[] res = componentFractions(prin);
        if (res == null || !componentsSumToOne(res)) {
            return null;
        }
        double totWeight = 0;
        for (SideDouble sd: res) {
            double cw = componentWeight(sd.s);
            if (cw == 0) {
                return null;
            }
            // Multiply the mole fraction by the compound's weight.
            // Later, dividing by the sum of all of these terms will
            // yield the weight fraction.
            cw *= sd.d;
            sd.d = cw;
            totWeight += cw;
        }
        for (SideDouble sd: res) {
            sd.d /= totWeight;
        }
        return res;
    }

    /** Assuming that diagram's principal coordinates are weight
        fractions, return the weight fractions of the various diagram
        components at point prin, or null if the fractions could not
        be determined. */
    protected SideDouble[] componentMoleFractions(Point2D prin) {
        SideDouble[] res = componentFractions(prin);
        if (res == null || !componentsSumToOne(res)) {
            return null;
        }
        double totMole = 0;
        for (SideDouble sd: res) {
            double cw = componentWeight(sd.s);
            if (cw == 0) {
                return null;
            }
            // Divide the weight fraction by the compound's weight.
            // Later, dividing by the sum of all of these terms will
            // yield the mole fraction.
            double mf = sd.d / cw;
            sd.d = mf;
            totMole += mf;
        }
        for (SideDouble sd: res) {
            sd.d /= totMole;
        }
        return res;
    }

    static class ProjectionAndOffset {
        Point2D.Double projection;
        Point2D.Double offset;
    }

    /** Conversions between mole and weight fraction produce ugly
        results when fractions exceed 100% or are less than 0%, as is
        often the case for labels outside the core diagram. To reduce
        this effect, treat such labels as the sum of the nearest point
        within the diagram (the projection) and an offset from that
        point, and perform the conversion by converting the projection
        and then adding the unchanged offset. */
    ProjectionAndOffset projectOntoDiagram(Point2D p) {
        ProjectionAndOffset res = new ProjectionAndOffset();
        res.projection = new Point2D.Double(p.getX(), p.getY());
        res.offset = new Point2D.Double(0,0);
        if (res.projection.x < 0) {
            res.offset.x = res.projection.x;
            res.projection.x = 0;
        } else if (res.projection.x > 1) {
            res.offset.x = res.projection.x - 1;
            res.projection.x = 1;
        }
        if (isTernary()) {
            if (res.projection.y < 0) {
                res.offset.y = res.projection.y;
                res.projection.y = 0;
            } else if (res.projection.y > 1) {
            res.offset.y = res.projection.y - 1;
            res.projection.y = 1;
            }
        }
        return res;
    }

    /** Convert the given point from mole fraction to weight fraction.
        If this is a binary diagram, then the Y component of the
        return value will equal the Y component of the input value. If
        the conversion cannot be performed for some reason, then
        return null. */
    public Point2D.Double moleToWeightFraction(Point2D p) {
        ProjectionAndOffset pao = projectOntoDiagram(p);
        Point2D.Double proj = pao.projection;
        Point2D.Double offs = pao.offset;
        SideDouble[] sds = componentWeightFractions(proj);
        if (sds == null) {
            return null;
        }
        if (isTernary()) {
            return new Point2D.Double(sds[0].d + offs.x, sds[1].d + offs.y);
        } else {
            return new Point2D.Double(sds[0].d + offs.x, proj.y);
        }
    }

    /** Inverse of moleToWeightFraction(). */
    public Point2D.Double weightToMoleFraction(Point2D p) {
        ProjectionAndOffset pao = projectOntoDiagram(p);
        Point2D.Double proj = pao.projection;
        Point2D.Double offs = pao.offset;
        SideDouble[] sds = componentMoleFractions(proj);
        if (sds == null) {
            return null;
        }
        if (isTernary()) {
            return new Point2D.Double(sds[0].d + offs.x, sds[1].d + offs.y);
        } else {
            return new Point2D.Double(sds[0].d + offs.x, proj.y);
        }
    }

    /** Globally convert all coordinates from mole fraction to weight
        fraction, if the information necessary to do so is available.
        Return true if the conversion was carried out.

        IMPORTANT BUGS:

        1. In ternary diagrams, the conversion from mole percent to
        weight percent should distort many straight lines into curves.
        What this routine actually (and incorrectly) does is to
        convert the endpoints only, incorrectly drawing a straight
        line between them.

        2. Angle values are not converted. This will screw up labels
        that follow isotherms, for example.
    */
    protected boolean moleToWeightFraction() {
        if (!haveComponentCompositions()) {
            return false;
        }

        for (DecorationHandle hand: movementHandles()) {
            hand.move(moleToWeightFraction(hand.getLocation()));
        }
        setUsingWeightFraction(true);

        return true;
    }

    /** Globally convert all coordinates from weight fraction to mole
        fraction, if the information necessary to do so is available.
        Return true if the conversion was carried out.

        IMPORTANT BUGS:

        1. Angle values are not converted.
    */
    protected boolean weightToMoleFraction() {
        if (!haveComponentCompositions()) {
            return false;
        }

        for (DecorationHandle hand: movementHandles()) {
            hand.move(weightToMoleFraction(hand.getLocation()));
        }
        setUsingWeightFraction(false);

        return true;
    }

    /* Return true if all sides that could have components do have
       them, those components' composiitions are known, and those
       components sum to 100%. */
    boolean haveComponentCompositions() {
        double[][] componentElements = getComponentElements();
        double a = 0;
        double b = 0;
        double c = 0;
        for (Side side: sidesThatCanHaveComponents()) {
            if (componentElements[side.ordinal()] == null) {
                return false;
            }
            LinearAxis axis = getAxis(side);
            if (axis == null) {
                return false;
            }
            a += axis.getA();
            b += axis.getB();
            c += axis.getC();
        }

        return Math.abs(a) < 1e-4 && Math.abs(b) < 1e-4 && Math.abs(c-1) < 1e-4;
    }

    static class PointAndError {
        Point2D.Double point;
        double error;
    }

    static class OrderByXY implements Comparator<Point2D.Double> {
        @Override public int compare(Point2D.Double a, Point2D.Double b) {
            double ax = a.x;
            double bx = b.x;
            return (ax < bx) ? -1 : (ax > bx) ? 1
                : (a.y < b.y) ? -1 : (a.y > b.y) ? 1 : 0;
        }
    }

    @JsonIgnore String[] getDiagramElements() {
        getComponentElements();
        return diagramElements;
    }

    class DecorationIterator<T extends Decoration> implements Iterator<T> {
        int lastIndex = -1;
        int index;
        Decoration singleton;

        public DecorationIterator(T singleton) {
            this.singleton = singleton;
            index = nextIndex(0);
        }

        @Override public final boolean hasNext() {
            return index >= 0;
        }

        @SuppressWarnings("unchecked") @Override public final T next() {
            if (index < 0) {
                throw new NoSuchElementException();
            }
            lastIndex = index;
            index = nextIndex(index+1);
            return (T) decorations.get(lastIndex);
        }

        @Override public final void remove() {
            if (lastIndex < 0) {
                throw new IllegalStateException();
            }
            if (index > 0) {
                --index;
            }
            decorations.remove(lastIndex);
        }

        private final int nextIndex(int startIndex) {
            for (int i = startIndex; i < decorations.size(); ++i) {
                Decoration d = decorations.get(i);
                if (singleton.getClass().isInstance(d)) {
                    return i;
                }
            }
            return -1;
        }
    }

    class LabelDecorationIterator extends DecorationIterator<LabelDecoration> {
        LabelDecorationIterator() {
            super(new LabelDecoration());
        }
    }

    class ArrowDecorationIterator extends DecorationIterator<ArrowDecoration> {
        ArrowDecorationIterator() {
            super(new ArrowDecoration(null));
        }
    }

    class LabelInfoIterator implements Iterator<LabelInfo> {
        DecorationIterator<LabelDecoration> it;

        public LabelInfoIterator() {
            it = new DecorationIterator<LabelDecoration>(new LabelDecoration());
        }

        @Override public final boolean hasNext() { return it.hasNext(); }
        @Override public final LabelInfo next() throws NoSuchElementException {
            return it.next().getItem();
        }
        @Override public final void remove() { it.remove(); }
    }

    class AnchoredLabelIterator implements Iterator<AnchoredLabel> {
        DecorationIterator<LabelDecoration> it;

        public AnchoredLabelIterator() {
            it = new DecorationIterator<LabelDecoration>(new LabelDecoration());
        }

        @Override public final boolean hasNext() { return it.hasNext(); }
        @Override public final AnchoredLabel next() throws NoSuchElementException {
            return it.next().getLabel();
        }
        @Override public final void remove() { it.remove(); }
    }

    class ArrowIterator implements Iterator<Arrow> {
        DecorationIterator<ArrowDecoration> it;

        public ArrowIterator() {
            it = new DecorationIterator<ArrowDecoration>(new ArrowDecoration(null));
        }

        @Override public final boolean hasNext() { return it.hasNext(); }
        @Override public final Arrow next() throws NoSuchElementException {
            return it.next().getItem();
        }
        @Override public final void remove() { it.remove(); }
    }

    class TieLineIterator implements Iterator<TieLine> {
        DecorationIterator<TieLineDecoration> it;

        public TieLineIterator() {
            it = new DecorationIterator<TieLineDecoration>(new TieLineDecoration());
        }

        @Override public final boolean hasNext() { return it.hasNext(); }
        @Override public final TieLine next() throws NoSuchElementException {
            return it.next().getItem();
        }
        @Override public final void remove() { it.remove(); }
    }

    class CuspFigureIterator implements Iterator<CuspFigure> {
        DecorationIterator<CurveDecoration> it;

        public CuspFigureIterator() {
            it = new DecorationIterator<CurveDecoration>(new CurveDecoration(null));
        }

        @Override public final boolean hasNext() { return it.hasNext(); }
        @Override public final CuspFigure next() throws NoSuchElementException {
            return it.next().getItem();
        }
        @Override public final void remove() { it.remove(); }
    }

    class LinearRulerIterator implements Iterator<LinearRuler> {
        DecorationIterator<RulerDecoration> it;

        public LinearRulerIterator() {
            it = new DecorationIterator<RulerDecoration>(new RulerDecoration(null));
        }

        @Override public final boolean hasNext() { return it.hasNext(); }
        @Override public final LinearRuler next() throws NoSuchElementException {
            return it.next().getItem();
        }
        @Override public final void remove() { it.remove(); }
    }

    class LabelInfoIterable implements Iterable<LabelInfo> {
        @Override public Iterator<LabelInfo> iterator() {
            return new LabelInfoIterator();
        }
    }

    class AnchoredLabelIterable implements Iterable<AnchoredLabel> {
        @Override public Iterator<AnchoredLabel> iterator() {
            return new AnchoredLabelIterator();
        }
    }

    class ArrowIterable implements Iterable<Arrow> {
        @Override public Iterator<Arrow> iterator() {
            return new ArrowIterator();
        }
    }

    class TieLineIterable implements Iterable<TieLine> {
        @Override public Iterator<TieLine> iterator() {
            return new TieLineIterator();
        }
    }

    class CuspFigureIterable implements Iterable<CuspFigure> {
        @Override public Iterator<CuspFigure> iterator() {
            return new CuspFigureIterator();
        }
    }

    class LinearRulerIterable implements Iterable<LinearRuler> {
        @Override public Iterator<LinearRuler> iterator() {
            return new LinearRulerIterator();
        }
    }

    public LabelInfoIterable labelInfos() {
        return new LabelInfoIterable();
    }

    public AnchoredLabelIterable labels() {
        return new AnchoredLabelIterable();
    }

    public ArrowIterable arrows() {
        return new ArrowIterable();
    }

    public TieLineIterable tieLines() {
        return new TieLineIterable();
    }

    public CuspFigureIterable paths() {
        return new CuspFigureIterable();
    }

    public LinearRulerIterable rulers() {
        return new LinearRulerIterable();
    }

    @JsonIgnore boolean isTernary() {
        return diagramType != null && diagramType.isTernary();
    }

    public void setDiagramComponent(Side side, String str) {
        componentElements = null;
        LinearAxis axis = getAxis(side);
        if (str != null && str.isEmpty()) {
            str = null;
        }

        diagramComponents[side.ordinal()] = str;

        if (axis != null) {
            if (str == null) {
                remove(axis);
            } else {
                rename(axis, str);
                axis.format = STANDARD_PERCENT_FORMAT;
            }
            return;
        }

        if (str == null) {
            // Nothing to do.
            return;
        }

        axis = defaultAxis(side);
        axis.format = STANDARD_PERCENT_FORMAT;
        axis.name = str;
        add(axis);
    }

    public static String htmlToText(String html) {
        if (html == null) {
            return null;
        }
        String text = Jsoup.parse(html).text();
        // Convert #0a (non-breaking-space) into ordinary spaces.
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            res.append((ch == '\u00a0') ? ' ' : ch);
        }
        return res.toString().trim();
    }

    /** Return the plain text of all labels, tags, key values, and
        diagram components. Duplicates are removed. */
    @JsonIgnore public String[] getAllText() {
        TreeSet<String> lines = new TreeSet<>();
        for (AnchoredLabel label: labels()) {
            lines.add(htmlToText(label.getText()));
        }
        for (String s: tags) {
            lines.add(s.trim());
        }
        for (String s: keyValues.values()) {
            lines.add(s.trim());
        }
        for (String s: diagramComponents) {
            if (s != null) {
                lines.add(s);
            }
        }
        return lines.toArray(new String[0]);
    }

    /** Return all chemical formulas converted to Hill order.
        Duplicates are removed. */
    @JsonIgnore public String[] getAllFormulas() {
        TreeSet<String> res = new TreeSet<>();
        for (String line: getAllText()) {
            for (ChemicalString.Match m: ChemicalString.embeddedFormulas(line)) {
                res.add(m.toString());
            }
        }
        return res.toArray(new String[0]);
    }

    LinearAxis getAxis(Side side) {
        switch (side) {
        case RIGHT:
            return getXAxis();
        case TOP:
            return getYAxis();
        case LEFT:
            return getLeftAxis();
        }
        return null;
    }

    /* Return true if this diagram has been marked as using weight
       percent coordinates. (That only matters for diagrams for which
       diagram components are defined.) */
    public boolean isUsingWeightFraction() {
        return usingWeightFraction;
    }

    /* Indicate whether this diagram uses weight percent coordinates.
       Note that calling setUsingWeightFraction() only claims that the
       existing coordinates are ALREADY defined using weight percents.
       If you want to convert from weight to mole percent or vice
       versa, call weightToMoleFraction or moleToWeightFraction. */
    public void setUsingWeightFraction(boolean b) {
        if (b != usingWeightFraction) {
            usingWeightFraction = b;
            propagateChange();
        }
    }

    /** Return a pretty description of the given point that is
        specified in principal coordinates. */
    String principalToPrettyString(Point2D.Double prin) {
        if (prin == null) {
            return null;
        }

        StringBuilder status = new StringBuilder();

        Point2D.Double mole = isUsingWeightFraction()
            ? weightToMoleFraction(prin) : prin;
        Point2D.Double weight = isUsingWeightFraction()
            ? prin : moleToWeightFraction(mole);
        boolean haveBoth = mole != null && weight != null;

        String compound = molePercentToCompound(mole);
        if (compound != null) {
            status.append(ChemicalString.autoSubscript(compound) + ": ");
        }

        boolean first = true;
        for (LinearAxis axis : axes) {
            if (first) {
                first = false;
            } else {
                status.append(",  ");
            }
            status.append(ChemicalString.autoSubscript(axis.name.toString()));
            status.append(" = ");

            if (haveBoth && isComponentAxis(axis)) {
                status.append(withFraction(axis, mole));
                status.append("/");
                status.append(withFraction(axis, weight));
            } else {
                status.append(withFraction(axis, prin));
            }
        }

        return status.toString();
    }

    String withFraction(LinearAxis axis, Point2D.Double p) {
        String res = axis.valueAsString(p);

        if (axisIsFractional(axis)) {
            // Express values in fractional terms if the decimal
            // value is a close approximation to a fraction.
            double d = axis.value(p);
            ContinuedFraction f = approximateFraction(d);
            if (f != null && f.numerator != 0 && f.denominator > 1) {
                res = res + " (" + f + ")";
            }
        }
        return res;
    }

    @JsonIgnore public LinearAxis getXAxis() {
        for (LinearAxis axis: axes) {
            if (isXAxis(axis)) {
                return axis;
            }
        }
        return null;
    }

    @JsonIgnore public LinearAxis getYAxis() {
        for (LinearAxis axis: axes) {
            if (isYAxis(axis)) {
                return axis;
            }
        }
        return null;
    }

    public boolean isLeftAxis(LinearAxis axis) {
        String name = (String) axis.name;
        return name.equals("Left")
            || (name != null
                && name.equals(diagramComponents[Side.LEFT.ordinal()]));
    }

    public boolean isXAxis(LinearAxis axis) {
        String name = (String) axis.name;
        return name.equals("Right") || name.equals("X")
            || (name != null
                && name.equals(diagramComponents[Side.RIGHT.ordinal()]));
    }

    public boolean isYAxis(LinearAxis axis) {
        String name = (String) axis.name;
        return name.equals("Top") || name.equals("Y")
            || (name != null
                && name.equals(diagramComponents[Side.TOP.ordinal()]));
    }

    /** Return true if this axis measures a diagram component
        concentration (in either weight or mole fraction) */
    boolean isComponentAxis(LinearAxis axis) {
        return isXAxis(axis) ? (diagramComponents[Side.RIGHT.ordinal()] != null)
            : isYAxis(axis) ? (diagramComponents[Side.TOP.ordinal()] != null)
            : isLeftAxis(axis) ? (diagramComponents[Side.LEFT.ordinal()] != null)
            : false;
    }

    @JsonIgnore public LinearAxis getLeftAxis() {
        for (LinearAxis axis: axes) {
            if (isLeftAxis(axis)) {
                return axis;
            }
        }
        
        return null;
    }

    public String getDiagramComponent(Side side) {
        return diagramComponents[side.ordinal()];
    }

    public boolean axisIsFractional(LinearAxis axis) {
        return (isXAxis(axis) && getDiagramComponent(Side.RIGHT) != null)
            || (isYAxis(axis) && getDiagramComponent(Side.TOP) != null)
            || (isLeftAxis(axis) && getDiagramComponent(Side.LEFT) != null);
    }

    public void add(AnchoredLabel label) {
        decorations.add(new LabelDecoration(new LabelInfo(label)));
        propagateChange();
    }

    /** @param xWeight Used to determine how to justify rows of text. */
    View toView(String str, double xWeight, Color textColor) {
        // System.out.println("toView('" + str + "', " + xWeight + ", " + textColor + ")");
        String style
            = "<style type=\"text/css\"><!--"
            + " body { font-size: 100 pt; } "
            + " sub { font-size: 75%; } "
            + " sup { font-size: 75%; } "
            + " --></style>";

        str = NestedSubscripts.unicodify(str);

        StringBuilder sb = new StringBuilder("<html><head");
        sb.append(style);
        sb.append("</head><body>");

        if (xWeight >= 0.67) {
            sb.append("<div align=\"right\">");
            sb.append(str);
            sb.append("</div>");
        } else if (xWeight >= 0.33) {
            sb.append("<div align=\"center\">");
            sb.append(str);
            sb.append("</div>");
        } else {
            sb.append(str);
        }
        sb.append("</body></html>");
        str = sb.toString();

        JLabel bogus = new JLabel(str);
        Font f = getFont();
        bogus.setFont(f.deriveFont((float) (f.getSize() * VIEW_MAGNIFICATION)));
        // bogus.setFont(f);
        bogus.setForeground(thisOrBlack(textColor));
        return (View) bogus.getClientProperty("html");
    }

    View toView(AnchoredLabel label) {
        return toView(label.getText(), label.getXWeight(), label.getColor());
    }

    /** Regenerate the labelViews field from the labels field. */
    /* void initializeLabelViews() {
        for (LabelInfo labelInfo: labelz()) {
            initializeLabelInfo(labelInfo);
        }
        } */

    /** Regenerate the labelViews field from the labels field. */
    void initializeLabelInfo(LabelInfo labelInfo) {
        if (labelInfo.view == null) {
            labelInfo.view = toView(labelInfo.label);
        }
    }

    public DiagramType getDiagramType() {
        return diagramType;
    }

    public void setDiagramType(DiagramType t) {
        this.diagramType = t;
        propagateChange();
    }

    double principalToPageAngle(double theta) {
        Point2D.Double p = new Point2D.Double(Math.cos(theta), Math.sin(theta));
        principalToStandardPage.deltaTransform(p, p);
        return Math.atan2(p.y, p.x);
    }

    double pageToPrincipalAngle(double theta) {
        Point2D.Double p = new Point2D.Double(Math.cos(theta), Math.sin(theta));
        standardPageToPrincipal.deltaTransform(p, p);
        return Math.atan2(p.y, p.x);
    }

    static class HandleAndDistance implements Comparable<HandleAndDistance> {
        DecorationHandle handle;
        double distance;
        public HandleAndDistance(DecorationHandle de, double di) {
            handle = de;
            distance = di;
        }
        @Override public int compareTo(HandleAndDistance other) {
            return (distance < other.distance) ? -1
                : (distance > other.distance) ? 1 : 0;
        }

        @Override public String toString() {
            return "HaD[" + handle + ", " + distance + "]";
        }
    }

    static class DecorationDistance implements Comparable<DecorationDistance> {
        Decoration decoration;
        CurveDistance distance;

        public DecorationDistance(Decoration de, CurveDistance di) {
            decoration = de;
            distance = di;
        }

        @Override public int compareTo(DecorationDistance other) {
            return (distance.distance < other.distance.distance) ? -1
                : (distance.distance > other.distance.distance) ? 1 : 0;
        }

        @Override public String toString() {
            return getClass().getSimpleName() + "[" + decoration + ", "
                + distance + "]";
        }
    }

    void removeDecoration(Decoration d) {
        for (Iterator<Decoration> it = decorations.iterator(); it.hasNext();) {
            if (it.next() == d) {
                it.remove();
                propagateChange();
                return;
            }
        }

        throw new IllegalStateException("Could not find " + d);
    }

    Decoration decorate(Decorated item) {
        if (item instanceof CuspFigure)
            return new CurveDecoration((CuspFigure) item);
        if (item instanceof AnchoredLabel)
            return new LabelDecoration(new LabelInfo((AnchoredLabel) item));
        if (item instanceof Arrow)
            return new ArrowDecoration((Arrow) item);
        if (item instanceof LinearRuler)
            return new RulerDecoration((LinearRuler) item);
        if (item instanceof TieLine)
            return new TieLineDecoration((TieLine) item);
        throw new IllegalStateException("decorate(): unrecognized item " + item);
    }

    /** @return a list of DecorationHandles such that moving all of
        those handles will move every decoration in the diagram. */
    ArrayList<DecorationHandle> movementHandles() {
        ArrayList<DecorationHandle> res = new ArrayList<>();
        for (Decoration d: getDecorations()) {
            for (DecorationHandle h: d.getMovementHandles()) {
                res.add(h);
            }
        }
        return res;
    }

    /** @return a list of DecorationHandles sorted by distance in page
        coordinates from point p (expressed in principal coordinates).
        Generally, only the closest DecorationHandle for each
        Decoration is included, though perhaps an exception should be
        made for VertexHandles. */
    ArrayList<DecorationHandle> nearestHandles(Point2D.Double p) {
        Point2D.Double pagePoint = principalToStandardPage.transform(p);

        ArrayList<HandleAndDistance> hads = new ArrayList<>();
        for (Decoration d: getDecorations()) {
            double minDistSq = 0;
            DecorationHandle nearestHandle = null;
            for (DecorationHandle h: d.getHandles()) {
                Point2D.Double p2 = h.getLocation();
                p2 = principalToStandardPage.transform(p2);
                double distSq = pagePoint.distanceSq(p2);
                if (nearestHandle == null || distSq < minDistSq) {
                    nearestHandle = h;
                    minDistSq = distSq;
                }
            }
            if (nearestHandle != null) {
                hads.add(new HandleAndDistance(nearestHandle, minDistSq));
            }
        }

        Collections.sort(hads);

        ArrayList<DecorationHandle> res = new ArrayList<>();
        for (HandleAndDistance h: hads) {
            res.add(h.handle);
        }

        return res;
    }

    /** Like nearestHandles(p), but return only handles of the given type. */
    @SuppressWarnings("unchecked")
        <T> ArrayList<T> nearestHandles(Point2D.Double p, T handleType) {
        Point2D.Double pagePoint = principalToStandardPage.transform(p);

        ArrayList<HandleAndDistance> hads = new ArrayList<>();
        Class<? extends Object> c = handleType.getClass();
        for (Decoration d: getDecorations()) {
            double minDistSq = 0;
            DecorationHandle nearestHandle = null;
            for (DecorationHandle h: d.getHandles()) {
                if (!c.isInstance(h)) {
                    continue;
                }
                Point2D.Double p2 = h.getLocation();
                p2 = principalToStandardPage.transform(p2);
                double distSq = pagePoint.distanceSq(p2);
                if (nearestHandle == null || distSq < minDistSq) {
                    nearestHandle = h;
                    minDistSq = distSq;
                }
            }
            if (nearestHandle != null) {
                hads.add(new HandleAndDistance(nearestHandle, minDistSq));
            }
        }

        Collections.sort(hads);

        ArrayList<T> res = new ArrayList<>();
        for (HandleAndDistance h: hads) {
            res.add((T) h.handle);
        }

        return res;
    }

    /** @return a list of all key points in the diagram. Some
        duplication is likely. */
    public ArrayList<Point2D.Double> keyPoints() {
        ArrayList<Point2D.Double> res = intersections();

        for (Point2D.Double p: principalToStandardPage.getInputVertices()) {
            res.add(p);
        }

        for (DecorationHandle m: getDecorationHandles()) {
            res.add(m.getLocation());
        }

        // Add all segment midpoints.
        for (Line2D.Double s: getAllLineSegments()) {
            res.add(new Point2D.Double((s.getX1() + s.getX2()) / 2,
                                       (s.getY1() + s.getY2()) / 2));
        }

        return res;
    }

    /** @return a list of all possible selections. */
    ArrayList<Decoration> getDecorations() {
        return decorations;
    }

    /** @return the underlying objects behind all decorations. */
    @JsonProperty("decorations") ArrayList<Decorated> getJSONDecorations() {
        ArrayList<Decorated> res = new ArrayList<>();
        for (Decoration d: getDecorations()) {
            res.add(d.getSerializationObject());
        }
        return res;
    }

    @JsonProperty("decorations") void setJSONDecorations(Decorated[] objects) {
        decorations = new ArrayList<>();
        for (Decorated o: objects) {
            decorations.add(decorate(o));
        }
        for (LinearRuler r: rulers()) {
            String name = r.axisName;
            for (LinearAxis axis: axes) {
                if (axis.name.equals(name)) {
                    r.axis = axis;
                    r.axisName = null;
                    break;
                }
            }
            if (r.axisName != null) {
                throw new IllegalStateException
                    ("Unknown axis name '" + r.getAxisName() + "'");
            }
        }
    }

    /** @return all decoration handles. */
    @JsonIgnore ArrayList<DecorationHandle> getDecorationHandles() {
        ArrayList<DecorationHandle> res = new ArrayList<>();

        for (Decoration selectable: getDecorations()) {
            res.addAll(Arrays.asList(selectable.getHandles()));
        }
        return res;
    }


    /** @return all point intersections involves curves and/or line
        segments. */
    ArrayList<Point2D.Double> intersections() {
        ArrayList<Point2D.Double> res = new ArrayList<>();
        Line2D.Double[] segments = getAllLineSegments();

        // Spline curves are defined in the page coordinates, and the
        // transformation of a spline fit in principal coordinates
        // does not equal the spline fit of the transformation, so
        // convert the segments to page coordinates to match the
        // splines.
        Line2D.Double[] pageSegments = new Line2D.Double[segments.length];
        for (int i = 0; i < segments.length; ++i) {
            pageSegments[i] = new Line2D.Double
                (principalToStandardPage.transform(segments[i].getP1()),
                 principalToStandardPage.transform(segments[i].getP2()));
        }

        ArrayList<BoundedParam2D> curves = new ArrayList<>();
        for (CuspFigure path: paths()) {
            CuspFigure pagePath = path.createTransformed
                (principalToStandardPage);

            Param2DBounder b = (Param2DBounder) pagePath.getParameterization();
            PathParam2D pp = (PathParam2D) b.getUnboundedCurve();
            for (BoundedParam2D curve: pp.curvedSegments()) {
                curves.add(curve);
            }


            for (Line2D segment: pageSegments) {
                for (Point2D.Double point:
                         pagePath.segIntersections(segment)) {
                    standardPageToPrincipal.transform(point, point);
                    res.add(point);
                }
            }
        }

        int cs = curves.size();
        for (int i = 0; i < cs; ++i) {
            for (int j = i+1; j < cs; ++j) {
                try {
                    for (Point2D.Double p: BoundedParam2Ds.intersections
                             (curves.get(i), curves.get(j), 1e-9, 80)) {
                        standardPageToPrincipal.transform(p, p);
                        res.add(p);
                    }
                } catch (FailedToConvergeException x) {
                    System.err.println(x);
                    // That's OK.
                }
            }
        }

        return res;
    }

    /* Return the DecorationDistance for the curve or ruler whose
       outline comes closest to pagePoint. This routine operates
       entirely in standard page space, both internally and in terms
       of the input and output values. */
    DecorationDistance nearestCurve(Point2D pagePoint) {
        ArrayList<Decoration> decs = new ArrayList<>();
        ArrayList<BoundedParam2D> params = new ArrayList<>();
        for (Decoration dec: getDecorations()) {
            if (dec instanceof BoundedParameterizable2D) {
                BoundedParam2D param
                    = ((BoundedParameterizable2D) dec).getParameterization();
                if (param.getMinT() == param.getMaxT()) {
                    // That's a point, not a curve. If the user wanted
                    // a point, they would have pressed 'p' instead.
                    continue;
                }
                decs.add(dec);
                params.add(param);
            }
        }

        if (params.size() == 0) {
            return null;
        }

        OffsetParam2D.DistanceIndex di
            = OffsetParam2D.distance(params, pagePoint, 1e-6, 2000);
        return new DecorationDistance(decs.get(di.index), di.distance);
    }

    /** Toggle the closed/open status of curve #pathNo. Throws
        IllegalArgumentException if that curve is filled, since you
        can't turn off closure for a filled curve. */
    public void toggleCurveClosure(CuspFigure path) throws IllegalArgumentException {
        if (path.isFilled() && path.isClosed()) {
            throw new IllegalArgumentException("Cannot turn off closure of a filled curve");
        }
        path.setClosed(!path.isClosed());
        propagateChange();
    }

    /** principalToStandardPage shifted to put the pageBounds corner
        at (0,0). */
    Affine getPrincipalToAlignedPage() {
        Affine xform = new Affine
            (AffineTransform.getTranslateInstance(-pageBounds.x, -pageBounds.y));
        xform.concatenate(principalToStandardPage);
        return xform;
    }

    void draw(Graphics2D g, CuspFigure path, double scale) {
        path.createTransformed(principalToScaledPage(scale))
            .draw(g, scale * path.getLineWidth());
    }

    void draw(Graphics2D g, TieLine tie, double scale) {
        tie.draw(g, getPrincipalToAlignedPage(), scale);
    }

    /** @return the name of the image file that this diagram was
        digitized from, or null if this diagram is not known to be
        digitized from a file. */
    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setTitle(String title) {
        put("title", title);
        propagateChange();
    }

    @JsonIgnore public String getTitle() {
        return get("title");
    }

    /** Like getTitle, but make up a title if no official title has been assigned. */
    @JsonIgnore public String getProvisionalTitle() {
        String titleStr = getTitle();
        if (titleStr != null) {
            return titleStr;
        }

        StringBuilder titleBuf = new StringBuilder();
        if (diagramType != null) {
            titleBuf.append(diagramType);
        }

        String str = systemName();
        if (str != null) {
            if (titleBuf.length() > 0) {
                titleBuf.append(" ");
            }
            titleBuf.append(str);
        }

        str = getOriginalFilename();
        if (str == null) {
            str = getFilename();
        }
        if (str != null) {
            if (titleBuf.length() > 0) {
                titleBuf.append(" ");
            }
            titleBuf.append(str);
        }

        return titleBuf.length() > 0 ? titleBuf.toString() : "Phase Equilibria Diagram Editor";
    }

    /** @return the system name if known, with components sorted into
        alphabetical order, or null otherwise.

        This might not be an actual system name if the diagram
        components are not principal components, but whatever. */
    public String systemName() {
        Side[] sides = null;
        if (diagramType == null) {
            return null;
        }

        if (isTernary()) {
            sides = new Side[] {Side.LEFT, Side.RIGHT, Side.TOP};
        } else {
            sides = new Side[] {Side.LEFT, Side.RIGHT};
        }

        ArrayList<String> comps = new ArrayList<String>();
        for (Side side: sides) {
            String str = getDiagramComponent(side);
            if (str == null) {
                return null;
            } else {
                comps.add(str);
            }
        }

        if (comps != null) {
            Collections.sort(comps);
        }

        StringBuilder str = null;
        for (String comp: comps) {
            if (str == null) {
                str = new StringBuilder();
            } else {
                str.append("-");
            }
            str.append(comp);
        }

        return str.toString();
    }

    public void setOriginalFilename(String filename) {
        if ((filename == null && originalFilename == null)
            || (filename != null && filename.equals(originalFilename))) {
            return;
        }

        originalFilename = filename;
        propagateChange();
    }

    protected static double normalFontSize() {
        return STANDARD_FONT_SIZE / BASE_SCALE;
    }

    LinearAxis createLeftAxis() {
        LinearAxis axis = new LinearAxis
                (STANDARD_PERCENT_FORMAT,
                 -1.0,
                 isTernary() ? -1.0 : 0.0,
                 1.0);
        String name = diagramComponents[Side.LEFT.ordinal()];
        axis.name = (name == null) ? "Left" : name;
        return axis;
    }

    LinearAxis defaultAxis(Side side) {
        if (isTernary()) {
            NumberFormat format = STANDARD_PERCENT_FORMAT;
            LinearAxis axis;

            switch (side) {
            case RIGHT:
                axis = LinearAxis.createXAxis(format);
                axis.name = "Right";
                return axis;
            case LEFT:
                return createLeftAxis();
            case TOP:
                axis = LinearAxis.createYAxis(format);
                axis.name = "Top";
                return axis;
            default:
                return null;
            }
        } else {
            NumberFormat format = new DecimalFormat("0.0000");
            switch (side) {
            case LEFT:
                return createLeftAxis();
            case RIGHT:
                return LinearAxis.createXAxis(format);
            case TOP:
                return LinearAxis.createYAxis(format);
            default:
                return null;
            }
        }
    }

    protected void initializeDiagram() {
        try {
            standardPageToPrincipal = principalToStandardPage.createInverse();
        } catch (NoninvertibleTransformException e) {
            System.err.println("This transform is not invertible");
            System.exit(2);
        }

        if (getOriginalFilename() != null) {
            try {
                principalToOriginal = (PolygonTransform)
                    originalToPrincipal.createInverse();
            } catch (NoninvertibleTransformException e) {
                System.err.println("This transform is not invertible");
                System.exit(2);
            }
        }

        setOriginalFilename(getOriginalFilename());
    }

    /** Invoked from the EditFrame menu */
    public void setAspectRatio(double aspectRatio) {
        Rectangle2D.Double bounds = principalToStandardPage.outputBounds();

        double oldValue = ((double) bounds.width) / bounds.height;
        double stretchFactor = aspectRatio / oldValue;
        ((RectangleTransform) principalToStandardPage).scaleOutput
            (stretchFactor, 1.0);
        try {
            standardPageToPrincipal = principalToStandardPage.createInverse();
        } catch (NoninvertibleTransformException e) {
            System.err.println("This transform is not invertible");
            System.exit(2);
        }

        pageBounds.x *= stretchFactor;
        pageBounds.width *= stretchFactor;
        propagateChange();
    }

    public double getMargin(Side side) {
        Rectangle2D.Double bounds = principalToStandardPage.outputBounds();

        switch (side) {
        case LEFT:
            return -pageBounds.x;
        case RIGHT:
            return pageBounds.getMaxX() - bounds.getMaxX();
        case TOP:
            return -pageBounds.y;
        case BOTTOM:
            return pageBounds.getMaxY() - bounds.getMaxY();
        }

        return 0;
    }


    /** Invoked from the EditFrame menu */
    public void setMargin(Side side, double margin) {
        double delta = margin - getMargin(side);

        switch (side) {
        case LEFT:
            pageBounds.x = -margin;
            // Fall through
        case RIGHT:
            pageBounds.width += delta;
            break;
        case TOP:
            pageBounds.y = -margin;
            // Fall through
        case BOTTOM:
            pageBounds.height += delta;
            break;
        }

        setPageBounds(pageBounds);
    }

    static Diagram loadFrom(File file) throws IOException {
        Diagram res;

        try {
            ObjectMapper mapper = getObjectMapper();
            res = (Diagram) mapper.readValue(file, Diagram.class);
        } catch (Exception e) {
            throw new IOException("File load error: " + e);
        }

        res.filename = file.getAbsolutePath();
        try {
            res.standardPageToPrincipal = res.principalToStandardPage.createInverse();
        } catch (NoninvertibleTransformException e) {
            System.err.println("This transform is not invertible");
            System.exit(2);
        }
        
        for (TieLine tie: res.tieLines()) {
            tie.innerEdge = res.idToCurve(tie.innerId);
            tie.outerEdge = res.idToCurve(tie.outerId);
        }
        if (res.pageBounds == null) {
            res.computeMargins();
        }
        res.setSaveNeeded(false);
        return res;
    }

    /** Invoked from the EditFrame menu */
    public void openDiagram(File file) throws IOException {
        setSaveNeeded(false);
        try (UpdateSuppressor us = new UpdateSuppressor()) {
                clear();
                cannibalize(loadFrom(file));
            }
        propagateChange1();
    }

    public Rectangle2D.Double getPageBounds() {
        if (pageBounds == null) {
            return null;
        }
        return (Rectangle2D.Double) pageBounds.clone();
    }

    public void setPageBounds(Rectangle2D rect) {
        pageBounds = Duh.createRectangle2DDouble(rect);
        propagateChange();
    }

    /** Set the margins just slightly wider than necessary to fit the
        entire diagram in. */
    public void computeMargins() {
        if (pageBounds == null) {
            setPageBounds(new Rectangle2D.Double(0, 0, 1, 1));
        }
        MeteredGraphics mg = new MeteredGraphics();
        double mscale = 10000;
        paintDiagram(mg, mscale, null);
        Rectangle2D.Double bounds = mg.getBounds();
        if (bounds == null) {
            return;
        }
        bounds.x /= mscale;
        bounds.y /= mscale;
        bounds.width /= mscale;
        bounds.height /= mscale;
        bounds.x += pageBounds.x;
        bounds.y += pageBounds.y;
        double margin = (bounds.width + bounds.height) / 200;
        bounds.width += margin * 2;
        bounds.height += margin * 2;
        bounds.x -= margin;
        bounds.y -= margin;
        setPageBounds(bounds);
    }

    /** Return the bounds of d on the aligned page. */
    public Rectangle2D.Double bounds(Decoration d) {
        if (pageBounds == null) {
            setPageBounds(new Rectangle2D.Double(0, 0, 1, 1));
        }
        MeteredGraphics mg = new MeteredGraphics();
        double mscale = 10000;
        d.draw(mg, mscale);
        Rectangle2D.Double bounds = mg.getBounds();
        if (bounds == null) {
            return null;
        }
        bounds.x /= mscale;
        bounds.y /= mscale;
        bounds.width /= mscale;
        bounds.height /= mscale;
        bounds.x += pageBounds.x;
        bounds.y += pageBounds.y;
        double margin = (bounds.width + bounds.height) / 200;
        bounds.width += margin * 2;
        bounds.height += margin * 2;
        bounds.x -= margin;
        bounds.y -= margin;
        return bounds;
    }

    /** Copy non-transient data fields from other. Afterwards, it is
        unsafe to modify other, because the modifications may affect
        this as well. In other words, this is a shallow copy that
        destroys other. */
    void cannibalize(Diagram other) {
        try (UpdateSuppressor us = new UpdateSuppressor()) {
                diagramType = other.diagramType;
                diagramComponents = other.diagramComponents;
                originalToPrincipal = other.originalToPrincipal;
                principalToStandardPage = other.principalToStandardPage;
                pageBounds = other.pageBounds;
                originalFilename = other.originalFilename;
                filename = other.filename;

                boolean haveBounds = (pageBounds != null);
                if (!haveBounds) {
                    pageBounds = new Rectangle2D.Double(0,0,1,1);
                }
                initializeDiagram();
                decorations = other.decorations;
                setFontName(other.getFontName());
                axes = other.axes;
                componentElements = null;
                setTags(other.getTags());
                setKeyValues(other.getKeyValues());
                if (!haveBounds) {
                    pageBounds = null;
                }
                setUsingWeightFraction(other.isUsingWeightFraction());
            }
        propagateChange1();
    }

    @JsonProperty("axes") ArrayList<LinearAxis> getAxes() {
        return axes;
    }

    /** Populate the Diagram object's "rulers" field from the
        individual axes' "rulers" fields, and set the individual axes'
        "rulers" fields to null. */
    @JsonProperty("axes") void setAxes(ArrayList<LinearAxis> axes) {
        for (LinearAxis axis: axes) {
            add(axis);
            // OBSOLESCENT
            if (axis.rulers != null) {
                for (LinearRuler r: axis.rulers) {
                    decorations.add(0, decorate(r));
                }
                axis.rulers = null;
            }
        }
    }

    // OBSOLESCENT
    @JsonProperty("tieLines")
    void setTieLines(ArrayList<TieLine> tieLines) {
        for (TieLine tie: tieLines) {
            decorations.add(0, decorate(tie));
        }
    }

    public void saveAsPDF(File file) {
        Document doc = new Document(PageSize.LETTER);
        PdfWriter writer = null;
        try {
            writer = PdfWriter.getInstance(doc, new FileOutputStream(file));
        } catch (Exception e) {
            System.err.println(e);
            return;
        }

        doc.open();
        appendToPDF(doc, writer);
        doc.close();
    }

    public byte[] toPDFByteArray() {
        Document doc = new Document(PageSize.LETTER);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer;
        try {
            writer = PdfWriter.getInstance(doc, baos);
        } catch (DocumentException e) {
            e.printStackTrace();
            return null;
        }

        doc.open();
        appendToPDF(doc, writer);
        doc.close();
        return baos.toByteArray();
    }

    public void appendToPDF(Document doc, PdfWriter writer) {
        try {
            doc.add(new Paragraph(getProvisionalTitle()));
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        float topMargin = (float) (72 * 0.5);
        Rectangle2D.Double bounds = new Rectangle2D.Double
            (doc.left(), doc.bottom(), doc.right() - doc.left(),
             doc.top() - doc.bottom() - topMargin);

        PdfContentByte cb = writer.getDirectContent();
        PdfTemplate tp = cb.createTemplate((float) bounds.width, (float) bounds.height);
        
        @SuppressWarnings("unused")
            Graphics2D g2 = false
            ? tp.createGraphics((float) bounds.width, (float) bounds.height,
                                new DefaultFontMapper())
            : tp.createGraphicsShapes((float) bounds.width, (float) bounds.height);

        g2.setFont(getFont());
        paintDiagram(g2, deviceScale(g2, bounds), null);
        g2.dispose();
        cb.addTemplate(tp, doc.left(), doc.bottom());
    }

    /** Invoked from the EditFrame menu */
    public void saveAsSVG(File file) throws IOException {
        DOMImplementation imp = GenericDOMImplementation.getDOMImplementation();
        org.w3c.dom.Document doc = imp.createDocument("http://www.w3.org/2000/svg", "svg", null);
        SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(doc);
        ctx.setComment("PED Document exported using Batik SVG Generator");
        ctx.setEmbeddedFontsOn(true);
        SVGGraphics2D svgGen = new SVGGraphics2D(ctx, true);
        paintDiagram(svgGen, BASE_SCALE, null);
        Writer wr = new FileWriter(file);
        svgGen.stream(wr);
        wr.close();
    }

    public void saveAsPED(Path path) throws IOException {
        try (PrintWriter writer = new PrintWriter
             (Files.newBufferedWriter(path, StandardCharsets.UTF_8))) {
                writer.print(Tabify.tabify(getObjectMapper().writeValueAsString(this)));
                setSaveNeeded(false);
            }
    }

    /** Remove every decoration that has at least one handle for which
        principalToStandardPage.transform(handle.getLocation()) lies
        outside r. Return true if at least one decoration was
        removed. */
    public boolean crop(Rectangle2D r) {
        boolean res = false;
        boolean finished = false;
        while (!finished) {
            finished = true;
            for (DecorationHandle hand: movementHandles()) {
                Point2D page = principalToStandardPage.transform
                    (hand.getLocation());
                if (Duh.distanceSq(page, r) > 1e-12) {
                    System.err.println("Removing handle " + hand
                                       + " at " + Duh.toString(page)
                                       + " outside  " + Duh.toString(r) + ")");
                    hand.remove();
                    finished = false;
                    res = true;
                    break;
                }
            }
        }
        return res;
    }

    /** Look at labels within the diagram in an attempt to guess what
     * the name of the diagram component is. For ternary diagrams,
     * look for compounds close to the appropriate corner (left,
     * right, or top). For binary diagrams, look for compound names
     * close to the lower left or lower right. */
    public String guessComponent(Side side) {
        boolean ternary = isTernary();

        Point2D.Double left = null;
        Point2D.Double right = null;
        Point2D.Double top = null;
        double rd = -1e50;
        double td = -1e50;
        double ld = -1e50;

        for (Point2D.Double p: principalToStandardPage.getOutputVertices()) {
            double trd;
            double ttd;
            double tld;

            if (ternary) {
                trd = p.x;
                ttd = -p.y;
                tld = 1 - p.x + p.y;
            } else {
                trd = p.x + p.y;
                tld = -p.x + p.y;
                ttd = 0;
            }

            if (trd > rd) {
                rd = trd;
                right = p;
            }
            if (tld > ld) {
                ld = tld;
                left = p;
            }
            if (ttd > td) {
                td = ttd;
                top = p;
            }
        }

        Point2D.Double cornerPage = (side == Side.LEFT) ? left
            : (side == Side.RIGHT) ? right : top;

        if (ternary && side == Side.TOP && keyValues != null) {
            // The location of the top corner may be stored in the
            // x3,y3 coordinates, which in the case of bottom
            // ternaries may differ from the top corner of the diagram
            // itself.

            String x3 = get("x3");
            String y3 = get("y3");
            if (x3 != null && y3 != null) {
                try {
                    Point2D.Double p = new Point2D.Double
                        (ContinuedFraction.parseDouble(x3),
                         ContinuedFraction.parseDouble(y3));
                    cornerPage = principalToStandardPage.transform(p);
                } catch (NumberFormatException e) { }
            }
        }

        double maxDistance = (pageBounds.width + pageBounds.height) / 8;
        double maxDistanceSq = maxDistance * maxDistance;
        Point2D.Double corner = standardPageToPrincipal.transform(cornerPage);

        for (LabelHandle hand: nearestHandles(corner, new LabelHandle())) {
            Point2D.Double page = principalToStandardPage.transform
                (hand.getLocation());
            if (page.distanceSq(cornerPage) > maxDistanceSq) {
                return null;
            }
            String text = htmlToText(hand.getLabel().getText());
            ChemicalString.Match match = ChemicalString.maybeQuotedComposition(text);
            if (match != null) {
                return match.within(text);
            }
        }

        return null;
    }

    /** Fill in all undefined diagram components with best guesses.
        Return false if it appears that some components still need to
        be added, or true otherwise. */
    public boolean guessComponents() {
        String code;
        if (diagramType == DiagramType.OTHER
            || (((code = get("diagram code")) != null)
                && (code.equals("Q") || code.equals("P")))) {
            // Types OTHER, QUATERNARY, and PRESSURE/TEMPERATURE do
            // not have diagram components. Note that types Q and P
            // relate only to an obsolete version of this editor, so
            // you won't find an explanation of these items anywhere
            // in this editor's source code.
            return true;
        }

        boolean res = true;
        Side[] sides = isTernary()
            ? new Side[] { Side.LEFT, Side. RIGHT, Side.TOP }
        : new Side [] { Side.LEFT, Side. RIGHT };

        for (Side side: sides) {
            int i = side.ordinal();
            String oldName = diagramComponents[i];
            if (oldName == null
                || oldName.toLowerCase().equals(side.toString().toLowerCase())) {
                String c = guessComponent(side);
                if (c != null) {
                    setDiagramComponent(side, c);
                    System.out.println("Assigned " + side + " = " + c);
                } else {
                    res = false;
                    System.out.println
                        ("Could not determine " + side
                         + " diagram component in " + getFilename());
                }
            }
        }
        return res;
    }

    public void setFill(CuspFigure path, StandardFill fill) {
        path.setFill(fill);
        propagateChange();
    }

    /** Invoked from the EditFrame menu */
    public void print(PrinterJob job) throws PrinterException {
        PrintRequestAttributeSet aset 
            = new HashPrintRequestAttributeSet();
        aset.add
            ((pageBounds.width > pageBounds.height)
             ? OrientationRequested.LANDSCAPE
             : OrientationRequested.PORTRAIT);
        job.print(aset);
    }

    @Override public int print(Graphics g0, PageFormat pf, int pageIndex)
         throws PrinterException {
        if (pageIndex != 0 || principalToStandardPage == null) {
            return Printable.NO_SUCH_PAGE;
        }
        Graphics2D g = (Graphics2D) g0;

        AffineTransform oldTransform = g.getTransform();

        Rectangle2D.Double bounds
            = new Rectangle2D.Double
            (pf.getImageableX(), pf.getImageableY(),
             pf.getImageableWidth(), pf.getImageableHeight());

        String title = getTitle();
        double deltaY = 0;
        double titleY = 0;
        if (title != null) {
            Rectangle2D tbox = g.getFontMetrics().getStringBounds(title, g);
            deltaY = tbox.getHeight();
            titleY = -tbox.getY();
        }

        g.translate(bounds.getX(), bounds.getY() + deltaY);
        if (title != null) {
            g.drawString(title, 0, (int) Math.round(titleY - deltaY));
        }
        g.setFont(getFont());
        double scale = Math.min((bounds.height - deltaY) / pageBounds.height,
                                bounds.width / pageBounds.width);
        paintDiagram(g, scale, null);
        g.setTransform(oldTransform);

        return Printable.PAGE_EXISTS;
    }

    /** @param segment A line on the standard page

        Return one of the vectors (which, inconsistently, is defined in
        principal coordinates) that passes through segment.getP1() and
        that is roughly parallel to segment, or null if no such line
        is close enough to parallel. A grid line is a line of zero
        change for a defined axis (from the "axes" variable). */
    Line2D.Double nearestGridLine(Line2D.Double segment,
                                  List<Point2D.Double> vectors) {
        Point2D source = segment.getP1();
        Point2D dest = segment.getP2();
        Point2D.Double pageDelta = Duh.aMinusB(dest, source);
        double deltaLength = Duh.length(pageDelta);

        // Tolerance is the maximum ratio of the distance between dest
        // and the projection to deltaLength. TODO A smarter approach
        // might allow for both absolute and relative errors.
        double tolerance = 0.06;

        double maxDist = deltaLength * tolerance;
        double maxDistSq = maxDist * maxDist;
        Line2D.Double res = null;
        
        for (Point2D.Double v: vectors) {
            principalToStandardPage.deltaTransform(v, v);
            Point2D.Double projection = Duh.nearestPointOnLine
                (pageDelta, new Point2D.Double(0,0), v);
            double distSq = pageDelta.distanceSq(projection);
            if (distSq < maxDistSq) {
                projection.x += source.getX();
                projection.y += source.getY();
                maxDistSq = distSq;
                res = new Line2D.Double(source, projection);
            }
        }

        if (res == null) {
            return null;
        }

        // Extend res to the limits of pageBounds.
        Rectangle2D b = pageBounds;

        Point2D.Double[] vertexes =
            { new Point2D.Double(b.getMinX(), b.getMinY()),
              new Point2D.Double(b.getMaxX(), b.getMinY()),
              new Point2D.Double(b.getMaxX(), b.getMaxY()),
              new Point2D.Double(b.getMinX(), b.getMaxY()) };
        double minT = Double.NaN;
        double maxT = Double.NaN;

        for (int i = 0; i < 4; ++i) {
            double t = Duh.lineSegmentIntersectionT
                (res.getP1(), res.getP2(),
                 vertexes[i], vertexes[(i+1) % 4]);
            if (Double.isNaN(t)) {
                continue;
            }
            if (Double.isNaN(minT)) {
                minT = t;
                maxT = t;
            } else {
                minT = Math.min(minT, t);
                maxT = Math.max(maxT, t);
            }
        }

        if (Double.isNaN(minT)) {
            // Apparently the line of the segment doesn't even
            // intersect the page. Weird.
            return null;
        }

        double x1 = res.getX1();
        double y1 = res.getY1();
        double dx = res.getX2() - x1;
        double dy = res.getY2() - y1;

        // Midpoints of segments are key points, but the midpoint of
        // the grid line is not really interesting. Double the
        // difference of minT and maxT to put the grid line's midpoint
        // on the edge of the diagram.

        double pastMax = maxT + (maxT - minT);

        return new Line2D.Double
            (x1 + minT * dx, y1 + minT * dy,
             x1 + pastMax * dx, y1 + pastMax * dy);
    }

    /** Apply the given transform to all curve vertices, all label
        locations, all arrow locations, and all ruler start- and
        endpoints. */
    public void transformPrincipalCoordinates(AffineTransform trans) {
        for (CuspFigure path: paths()) {
            path.setCurve(path.getCurve().createTransformed(trans));
        }

        Point2D.Double tmp = new Point2D.Double();

        for (AnchoredLabel label: labels()) {
            tmp.x = label.x;
            tmp.y = label.y;
            trans.transform(tmp, tmp);
            label.x = tmp.x;
            label.y = tmp.y;
        }

        for (Arrow arrow: arrows()) {
            tmp.x = arrow.x;
            tmp.y = arrow.y;
            trans.transform(tmp, tmp);
            arrow.x = tmp.x;
            arrow.y = tmp.y;
        }

        for (LinearRuler ruler: rulers()) {
            trans.transform(ruler.startPoint, ruler.startPoint);
            trans.transform(ruler.endPoint, ruler.endPoint);
        }

        propagateChange();
    }

    /** Apply the given transform to all coordinates defined in
        principal coordinates, but apply corresponding and inverse
        transformations to all transforms to and from principal
        coordinates, with one exception: leave the x- and y-axes
        alone. So the diagram looks the same as before except for (1)
        principal component axis ticks and (2) principal coordinate
        values as indicated in the status bar. For example, one might
        use this method to convert a binary diagram's y-axis from one
        temperature scale to another, or from the default range 0-1 to
        the range you really want. */
    public void invisiblyTransformPrincipalCoordinates(AffineTransform trans) {
        transformPrincipalCoordinates(trans);

        Affine atrans = new Affine(trans);
        Affine itrans;
        try {
            itrans = atrans.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalStateException("Transform " + trans
                                            + " is not invertible");
        }

        if (originalToPrincipal != null) {
            originalToPrincipal.preConcatenate(atrans);
            principalToOriginal.concatenate(itrans);
        }

        // Convert all angles from principal to page coordinates.
        for (Arrow item: arrows()) {
            item.setAngle(principalToPageAngle(item.getAngle()));
        }
        for (AnchoredLabel item: labels()) {
            item.setAngle(principalToPageAngle(item.getAngle()));
        }

        principalToStandardPage.concatenate(itrans);
        standardPageToPrincipal.preConcatenate(atrans);

        // Convert all angles from page to the new principal coordinates.
        for (LinearAxis axis: axes) {
            if (isXAxis(axis) || isYAxis(axis)) {
                continue;
            }
            axis.concatenate(itrans);
        }

        for (Arrow item: arrows()) {
            item.setAngle(pageToPrincipalAngle(item.getAngle()));
        }
        for (AnchoredLabel item: labels()) {
            item.setAngle(pageToPrincipalAngle(item.getAngle()));
        }



    }

    ContinuedFraction approximateFraction(double d) {
        // Digitization isn't precise enough to justify guessing
        // ratios. Only show fractions that are almost exact.
        ContinuedFraction f = ContinuedFraction.create(d, 0.000001, 0, 90);

        if (f != null) {
            return f;
        }

        return f;
    }

    /** If the diagram has a full set of diagram components defined as
        compounds with integer subscripts (that is, the components sum
        to 1), and point "prin" nearly equals a round fraction, then
        return the compound that "prin" represents. */
    public String molePercentToCompound(Point2D.Double prin) {
        if (prin == null) {
            return null;
        }
        double[][] componentElements = getComponentElements();
        if (componentElements == null) {
            return null;
        }

        String[] diagramElements = getDiagramElements();

        SideDouble[] sds = componentFractions(prin);
        if (sds == null || sds.length == 0 || !componentsSumToOne(sds)) {
            return null;
        }
        for (SideDouble sd: sds) {
            if (componentElements[sd.s.ordinal()] == null) {
                // Can't do it without a complete set of diagram
                // components that can be parsed to compounds.
                return null;
            }
        }

        int eltCnt = diagramElements.length;
        double[] quantities = new double[eltCnt];
        for (SideDouble sd: sds) {
            // Vector of element quantities for this component
            double[] compel = componentElements[sd.s.ordinal()];
            // Quantity of this component
            double d = sd.d;
            // If d is slightly out of bounds, move it in-bounds. If d
            // is very small, then reduce it to zero.

            if (d > 1 - 1e-4) {
                if (d > 1 + 1e-4) {
                    return null;
                } else {
                    d = 1;
                }
            } else if (d < 1e-4) {
                if (d < -1e-4) {
                    return null;
                } else {
                    d = 0;
                }
            }

            for (int i = 0; i < eltCnt; ++i) {
                quantities[i] += d * compel[i];
            }
        }

        ArrayList<ContinuedFraction> fracs = new ArrayList<>(eltCnt);

        long lcd = 1;
        for (double d: quantities) {
            ContinuedFraction f = approximateFraction(d);
            if (f == null) {
                return null;
            }
            fracs.add(f);
            try {
                lcd = ContinuedFraction.lcm(lcd, f.denominator);
            } catch (OverflowException e) {
                return null;
            }
        }

        StringBuilder res = new StringBuilder();
        for (int i = 0; i < eltCnt; ++i) {
            ContinuedFraction f = fracs.get(i);
            long num = f.numerator * (lcd / f.denominator);
            if (num == 0) {
                continue;
            }
            res.append(diagramElements[i]);
            if (num > 1) {
                res.append(num);
            }
        }
        return res.toString();
    }

    /** @return an array of all straight line segments defined for
        this diagram. 2-point SplinePols are actually straight, so
        they are included. */
    @JsonIgnore public Line2D.Double[] getAllLineSegments() {
        ArrayList<Line2D.Double> res = new ArrayList<>();
         
        for (CuspFigure path : paths()) {
            Param2DBounder b = (Param2DBounder) path.getParameterization();
            PathParam2D pp = (PathParam2D) b.getUnboundedCurve();
            for (Line2D.Double line: pp.lineSegments()) {
                res.add(line);
            }
        }

        for (LinearRuler ruler: rulers()) {
            BoundedParam2D p = ruler.getParameterization();
            res.add(new Line2D.Double(p.getStart(), p.getEnd()));
        }

        return res.toArray(new Line2D.Double[0]);
    }

    /** Draw a circle around each point in path. */
    void circleVertices(Graphics2D g, CuspFigure path, double scale,
                        boolean fill, double r) {
        Point2D.Double xpoint = new Point2D.Double();
        Affine p2d = principalToScaledPage(scale);

        Stroke oldStroke = g.getStroke();
        if (!fill) {
            g.setStroke(new BasicStroke((float) (r / 4)));
        }

        for (Point2D.Double point: path.getPoints()) {
            p2d.transform(point, xpoint);
            Shape shape = new Ellipse2D.Double
                (xpoint.x - r, xpoint.y - r, r * 2, r * 2);
            if (fill) {
                g.fill(shape);
            } else {
                g.draw(shape);
            }
        }

        if (!fill) {
            g.setStroke(oldStroke);
        }
    }

    /** @return a transformation that maps the unit square to the
        outline of label labelNo in scaled page space. */
    AffineTransform labelToScaledPage(LabelInfo labelInfo, double scale) {
        initializeLabelInfo(labelInfo);
        AnchoredLabel label = labelInfo.label;
        View view = labelInfo.view;
        Affine toPage = getPrincipalToAlignedPage();
        Point2D.Double point = toPage.transform(label.getX(), label.getY());
        double angle = principalToPageAngle(label.getAngle());

        return labelToScaledPage
            (view, scale * label.getFontSize(), angle,
             point.x * scale, point.y * scale,
             label.getXWeight(), label.getYWeight(),
             label.getBaselineXOffset(), label.getBaselineYOffset());
    }

    /** Take all labels that have nonzero baseline X/Y offsets
        defined, set those offsets to zero, and transform the labels'
        X and Y locations so that the label's drawn position remains
        unchanged. Baseline offsets in their current state are little
        more than a hack used during the conversion of GRUMP files, so
        zeroing them out is a good thing. */
    void zeroBaselineOffsets() {
        for (AnchoredLabel label: labels()) {
            double bxo = label.getBaselineXOffset();
            double byo = label.getBaselineYOffset();
            if (bxo == 0 && byo == 0) {
                continue;
            }
            double angle = principalToPageAngle(label.getAngle());
            AffineTransform baselineToPage = AffineTransform.getRotateInstance(angle);
            double textScale = label.getFontSize() / VIEW_MAGNIFICATION
                / BASE_SCALE;
            baselineToPage.scale(textScale, textScale);
            Point2D.Double offset = new Point2D.Double(bxo, byo);
            baselineToPage.deltaTransform(offset, offset);
            // Offset is now defined in standard page coordinates.
            standardPageToPrincipal.deltaTransform(offset, offset);
            // Offset is now defined in principal coordinates.
            label.setX(label.getX() + offset.x);
            label.setY(label.getY() + offset.y);
            label.setBaselineXOffset(0);
            label.setBaselineYOffset(0);
        }
    }

    public void setLayer(Decoration d, int layer) {
        int oldLayer = getLayer(d);
        int cnt = decorations.size();
        if (oldLayer != -1) {
            decorations.remove(oldLayer);
        }
        if (layer < 0) {
            layer = 0;
        } else if (layer >= cnt) {
            layer = cnt - 1;
        }
        decorations.add(layer, d);
        propagateChange();
    }

    public int getLayer(Decoration d) {
        int i = -1;
        for (Decoration thisd: decorations) {
            ++i;
            if (d == thisd) {
                return i;
            }
        }
        return i;
    }


    void draw(Graphics g0, LabelInfo labelInfo, double scale) {
        AnchoredLabel label = labelInfo.label;
        if (label.getFontSize() == 0) {
            return;
        }

        Graphics2D g = (Graphics2D) g0;
        AffineTransform l2s = labelToScaledPage(labelInfo, scale);
        Path2D.Double box = htmlBox(l2s);

        if (label.isOpaque()) {
            Color oldColor = g.getColor();
            g.setColor(Color.WHITE);
            g.fill(box);
            g.setColor(oldColor);
        }

        if (label.isBoxed()) {
            g.draw(box);
        }

        // TODO: Exploit the labelToScaledPage transformation in htmlDraw.
        View view = labelInfo.view;
        Affine toPage = getPrincipalToAlignedPage();
        Point2D.Double point = toPage.transform(label.getX(), label.getY());
        double angle = principalToPageAngle(label.getAngle());
        htmlDraw(g, view, scale * label.getFontSize(),
                 angle, point.x * scale, point.y * scale,
                 label.getXWeight(), label.getYWeight(),
                 label.getBaselineXOffset(), label.getBaselineYOffset());
    }

    void initializeLabelMargins() {
        if (Double.isNaN(labelXMargin)) {
            View em = toView("n", 0, Color.BLACK);
            labelXMargin = em.getPreferredSpan(View.X_AXIS) / 3.0;
            // labelYMargin = em.getPreferredSpan(View.Y_AXIS) / 5.0;
            labelYMargin = 0;
        }
    }

    /**
       @param view The view.paint() method is used to perform the
       drawing (the decorated text to be encoded is implicitly
       included in this parameter)

       @param scale The text is magnified by this factor before being
       painted.

       @param angle The printing angle. 0 = running left-to-right (for
       English); rotated 90 degrees clockwise (running downwards)

       @param ax The X position of the anchor point

       @param ay The Y position of the anchor point

       @param xWeight 0.0 = The anchor point lies along the left edge
       of the text block in baseline coordinates (if the text is
       rotated, then this edge may not be on the left in physical
       coordinates; for example, if the text is rotated by an angle of
       PI/2, then this will be the top edge in physical coordinates);
       0.5 = the anchor point lies along the vertical line (in
       baseline coordinates) that bisects the text block; 1.0 = the
       anchor point lies along the right edge (in baseline
       coordinates) of the text block

       @param yWeight 0.0 = The anchor point lies along the top edge
       of the text block in baseline coordinates (if the text is
       rotated, then this edge may not be on top in physical
       coordinates; for example, if the text is rotated by an angle of
       PI/2, then this will be the right edge in physical
       coordinates); 0.5 = the anchor point lies along the horizontal
       line (in baseline coordinates) that bisects the text block; 1.0
       = the anchor point lies along the bottom edge (in baseline
       coordinates) of the text block
    */
    void htmlDraw(Graphics g, View view, double scale, double angle,
                  double ax, double ay,
                  double xWeight, double yWeight,
                  double baselineXOffset, double baselineYOffset) {
        scale /= VIEW_MAGNIFICATION;
        double baseWidth = view.getPreferredSpan(View.X_AXIS);
        double baseHeight = view.getPreferredSpan(View.Y_AXIS);
        initializeLabelMargins();
        double width = baseWidth + labelXMargin * 2;
        double height = baseHeight + labelYMargin * 2;

        Graphics2D g2d = (Graphics2D) g;
        double textScale = scale / BASE_SCALE;

        AffineTransform baselineToPage = AffineTransform.getRotateInstance(angle);
        baselineToPage.scale(textScale, textScale);
        Point2D.Double xpoint = new Point2D.Double();
        baselineToPage.transform
            (new Point2D.Double(width * xWeight, height * yWeight), xpoint);

        ax -= xpoint.x;
        ay -= xpoint.y;

        Point2D.Double baselineOffset = new Point2D.Double
            (baselineXOffset, baselineYOffset);
        baselineToPage.transform
            (baselineOffset, baselineOffset);

        ax += baselineOffset.x;
        ay += baselineOffset.y;

        // Now (ax, ay) represents the (in baseline coordinates) upper
        // left corner of the text block expanded by the x- and
        // y-margins.

        {
            // Displace (ax,ay) by (labelXMargin, labelYMargin) (again, in baseline
            // coordinates) in order to obtain the true upper left corner
            // of the text block.

            baselineToPage.transform
                (new Point2D.Double(labelXMargin, labelYMargin), xpoint);
            ax += xpoint.x;
            ay += xpoint.y;
        }

        // Paint the view after creating a transform in which (0,0)
        // maps to (ax,ay)

        AffineTransform oldxform = g2d.getTransform();
        g2d.translate(ax, ay);
        g2d.transform(baselineToPage);

        // Don't pass a rectangle that is larger than necessary to
        // view.paint(), or else view.paint() will attempt to center
        // the label on its own, but it won't recenter the way we
        // want.
        Rectangle r = new Rectangle
            (0, 0, (int) Math.ceil(baseWidth), (int) Math.ceil(baseHeight));

        // The views seem to require non-null clip bounds for some
        // dumb reason, and the SVG uses no clip region, so... TODO
        // Still needed?!
        if (g.getClipBounds() == null) {
            g.setClip(-1000000, -1000000, 2000000, 2000000);
        }

        try {
            view.paint(g, r);
        } catch (NullPointerException e) {
            System.out.println("Clip = " + g2d.getClipBounds());
            System.out.println("R = " + r);
            throw(e);
        }
        g2d.setTransform(oldxform);
    }

    /** @return a transformation that maps the unit square to the
        outline of this label in scaled page space. */
    AffineTransform labelToScaledPage
        (View view, double scale, double angle,
         double ax, double ay, double xWeight, double yWeight,
         double baselineXOffset, double baselineYOffset) {
        initializeLabelMargins();
        double width = view.getPreferredSpan(View.X_AXIS) + labelXMargin * 2;
        double height = view.getPreferredSpan(View.Y_AXIS) + labelYMargin * 2;
        double textScale = scale / BASE_SCALE / VIEW_MAGNIFICATION;

        AffineTransform res = AffineTransform.getTranslateInstance(ax, ay);
        res.rotate(angle);
        res.scale(textScale, textScale);
        res.translate(baselineXOffset, baselineYOffset);
        res.scale(width, height);
        res.translate(-xWeight, -yWeight);
        return res;
    }


    /** Create a box in the space that the transform of the unit
        square would enclose. */
    Path2D.Double htmlBox(AffineTransform xform) {
        Point2D.Double[] corners =
            { new Point2D.Double(0,0),
              new Point2D.Double(1,0),
              new Point2D.Double(1,1),
              new Point2D.Double(0,1) };
        xform.transform(corners, 0, corners, 0, corners.length);
        Path2D.Double res = new Path2D.Double();
        res.moveTo(corners[0].x, corners[0].y);
        for (int i = 1; i < corners.length; ++i) {
            res.lineTo(corners[i].x, corners[i].y);
        }
        res.closePath();
        return res;
    }

    static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationConfig.Feature.INDENT_OUTPUT,
                                   true);
            objectMapper.setSerializationInclusion(Inclusion.NON_NULL);
            SerializationConfig ser = objectMapper.getSerializationConfig();
            DeserializationConfig des = objectMapper.getDeserializationConfig();

            ser.addMixInAnnotations(Point2D.class, Point2DAnnotations.class);
            des.addMixInAnnotations(Point2D.class, Point2DAnnotations.class);

            ser.addMixInAnnotations(Rectangle2D.class,
                                    Rectangle2DAnnotations.class);
            des.addMixInAnnotations(Rectangle2D.class,
                                    Rectangle2DAnnotations.class);

            ser.addMixInAnnotations(Rectangle2D.Double.class,
                                    Rectangle2DDoubleAnnotations.class);
            des.addMixInAnnotations(Rectangle2D.Double.class,
                                    Rectangle2DDoubleAnnotations.class);

            ser.addMixInAnnotations(BasicStroke.class,
                                    BasicStrokeAnnotations.class);
            des.addMixInAnnotations(BasicStroke.class,
                                    BasicStrokeAnnotations.class);

            ser.addMixInAnnotations(DecimalFormat.class,
                                    DecimalFormatAnnotations.class);
            des.addMixInAnnotations(DecimalFormat.class,
                                    DecimalFormatAnnotations.class);

            ser.addMixInAnnotations(NumberFormat.class,
                                    NumberFormatAnnotations.class);
            des.addMixInAnnotations(NumberFormat.class,
                                    NumberFormatAnnotations.class);

            ser.addMixInAnnotations(Color.class,
                                    ColorAnnotations.class);
            des.addMixInAnnotations(Color.class,
                                    ColorAnnotations.class);

            ser.addMixInAnnotations(Decorated.class,
                                    DecoratedAnnotations.class);
            // OBSOLESCENT
            /*
            des.addMixInAnnotations(Decorated.class,
                                    DecoratedAnnotations.class);
            */
        }

        return objectMapper;
    }

    void drawArrow(Graphics2D g, double scale, Arrow ai) {
        Affine xform = principalToScaledPage(scale);
        Point2D.Double xpoint = xform.transform(ai.x, ai.y);
        Arrow arr = new Arrow
            (xpoint.x, xpoint.y, scale * ai.size,
             principalToPageAngle(ai.theta));
        g.fill(arr);
    }

    void addTernaryBottomRuler(double start /* Z */, double end /* Z */) {
        LinearRuler r = defaultTernaryRuler();
        r.textAngle = 0;
        r.tickLeft = true;
        r.labelAnchor = LinearRuler.LabelAnchor.RIGHT;

        r.startPoint = new Point2D.Double(start, 0.0);
        r.endPoint = new Point2D.Double(end, 0);
        r.startArrow = Math.abs(start) > 1e-8;
        r.endArrow = (Math.abs(end - 1) > 1e-4);
        r.suppressStartTick = (diagramType != DiagramType.TERNARY_RIGHT)
            || (start < 1e-6);
        r.suppressEndTick = (diagramType != DiagramType.TERNARY_LEFT)
            || (end > 1 - 1e-6);
        r.axis = getXAxis();
        add(r);
    }

    void addTernaryLeftRuler(double start /* Y */, double end /* Y */) {
        LinearRuler r = defaultTernaryRuler();
        r.textAngle = Math.PI / 3;
        r.tickRight = true;
        r.labelAnchor = LinearRuler.LabelAnchor.LEFT;

        // Usual PED Data Center style leaves out the tick labels on
        // the left unless this is a top or left partial ternary
        // diagram.
        boolean showLabels = diagramType == DiagramType.TERNARY_LEFT
            || diagramType == DiagramType.TERNARY_TOP;
        if (showLabels) {
            r.labelAnchor = LinearRuler.LabelAnchor.LEFT;
            r.suppressEndLabel = diagramType != DiagramType.TERNARY_RIGHT;
        } else {
            r.labelAnchor = LinearRuler.LabelAnchor.NONE;
        }

        r.startPoint = new Point2D.Double(0.0, start);
        r.endPoint = new Point2D.Double(0.0, end);
        r.startArrow = Math.abs(start) > 1e-8;
        r.endArrow = (Math.abs(end - 1) > 1e-4);
        // The tick label for the bottom of the left ruler is
        // redundant with the tick label for the left end of the
        // bottom ruler unless this is a top partial ternary
        // diagram.
        r.suppressStartLabel = (diagramType != DiagramType.TERNARY_TOP);
        r.suppressStartTick = (diagramType != DiagramType.TERNARY_TOP)
            || (start < 1e-6);
        r.suppressEndLabel = (diagramType != DiagramType.TERNARY_TOP);
        r.suppressEndTick = (diagramType != DiagramType.TERNARY_LEFT)
            || (end > 1 - 1e-6);
        r.axis = getYAxis();
        add(r);
    }

    void addTernaryRightRuler(double start /* Y */, double end /* Y */) {
        LinearRuler r = defaultTernaryRuler();
        r.textAngle = Math.PI * 2 / 3;
        r.tickLeft = true;

        // The tick labels for the right ruler are redundant with the
        // tick labels for the left ruler unless this is a top or right
        // partial ternary diagram.
        boolean showLabels = diagramType == DiagramType.TERNARY_RIGHT
            || diagramType == DiagramType.TERNARY_TOP;
        if (showLabels) {
            r.labelAnchor = LinearRuler.LabelAnchor.RIGHT;
            r.suppressEndLabel = diagramType != DiagramType.TERNARY_RIGHT;
        } else {
            r.labelAnchor = LinearRuler.LabelAnchor.NONE;
        }
        r.suppressStartTick = diagramType != DiagramType.TERNARY_TOP
            || (start < 1e-6);
        r.suppressEndTick = diagramType != DiagramType.TERNARY_RIGHT
            || (start > 1-1e-6);

        r.startPoint = new Point2D.Double(1 - start, start);
        r.endPoint = new Point2D.Double(1 - end, end);
        r.startArrow = Math.abs(start) > 1e-8;
        r.endArrow = (Math.abs(end - 1) > 1e-4);
        r.axis = getYAxis();
        add(r);
    }

    void addBinaryBottomRuler() {
        LinearRuler r = defaultBinaryRuler();
        r.textAngle = 0;
        r.tickLeft = true;
        r.labelAnchor = LinearRuler.LabelAnchor.RIGHT;
        r.startPoint = new Point2D.Double(0.0, 0.0);
        r.endPoint = new Point2D.Double(1.0, 0.0);
        r.axis = getXAxis();
        add(r);
    }

    void addBinaryTopRuler() {
        LinearRuler r = defaultBinaryRuler();
        r.textAngle = 0;
        r.tickRight = true;
        r.labelAnchor = LinearRuler.LabelAnchor.NONE;

        r.startPoint = new Point2D.Double(0.0, 1.0);
        r.endPoint = new Point2D.Double(1.0, 1.0);
        r.axis = getXAxis();
        add(r);
    }

    void addBinaryLeftRuler() {
        LinearRuler r = defaultBinaryRuler();
        r.textAngle = Math.PI / 2;
        r.tickRight = true;
        r.labelAnchor = LinearRuler.LabelAnchor.LEFT;

        r.startPoint = new Point2D.Double(0.0, 0.0);
        r.endPoint = new Point2D.Double(0.0, 1.0);
        r.axis = getYAxis();
        add(r);
    }

    void addBinaryRightRuler() {
        LinearRuler r = defaultBinaryRuler();
        r.textAngle = Math.PI / 2;
        r.tickLeft = true;
        r.labelAnchor = LinearRuler.LabelAnchor.NONE;

        r.startPoint = new Point2D.Double(1.0, 0.0);
        r.endPoint = new Point2D.Double(1.0, 1.0);
        r.axis = getYAxis();
        add(r);
    }

    /** Return the weight of the given component computed as a product
        of the quantities and standard weights of its individual
        elements, or 0 if the weight could not be computed, either
        because the component is not known, it could not be converted
        to a compound, or the compound includes elements for which no
        standard weight is defined. */
    public double componentWeight(Side side) {
        double[][] componentElements = getComponentElements();
        double[] ces = componentElements[side.ordinal()];
        if (ces == null) {
            return 0;
        }
        String[] elements = getDiagramElements();
        double total = 0;
        for (int i = 0; i < elements.length; ++i) {
            double q = ces[i];
            if (q == 0) {
                continue;
            }
            double w = ChemicalString.elementWeight(elements[i]);
            if (w == 0) {
                return 0;
            }
            total += q * w;
        }
        return total;
    }

    static Color thisOrBlack(Color c) {
        return (c == null) ? Color.BLACK : c;
    }

    @JsonIgnore public Rectangle2D.Double getPrincipalBounds() {
        return principalToStandardPage.inputBounds();
    }

    public String getFontName() {
        return getFont().getFontName();
    }

    /** Returns false if there is no effect because it's the same font
        as before and it was already loaded. */
    public boolean setFontName(String s) {
        if (embeddedFont != null && s.equals(getFontName())) {
            return false; // No change
        }

        String filename = fontFiles.get(s);
        if (filename == null) {
            throw new IllegalArgumentException("Unrecognized font name '" + s + "'");
        }
        embeddedFont = loadFont(filename, STANDARD_FONT_SIZE);
        for (LabelInfo labelInfo: labelInfos()) {
            labelInfo.view = null;
            labelInfo.setCenter(null);
        }
        propagateChange();
        return true;
    }

    public Font loadFont(String filename, float size) {
        InputStream is = getClass().getResourceAsStream(filename);
        if (is == null) {
            throw new IllegalStateException
                ("Could not locate font '" + filename + "'");
        }
        try {
            Font f = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
            f = f.deriveFont(size);
            return f;
        } catch (IOException e) {
            throw new IllegalStateException
                ("Could not process font '" + filename
                 + "': " + e);
        } catch (FontFormatException e) {
            throw new IllegalStateException
                ("Could not process font '" + filename
                 + "': " + e);
        }
    }

    @JsonIgnore public Font getFont() {
        if (embeddedFont == null) {
            setFontName(defaultFontName);
        }
        return embeddedFont;
    }
}

// Annotations that are serialization hints for the Jackson JSON
// encoder

@JsonDeserialize(as=Point2D.Double.class)
abstract class Point2DAnnotations {
}

@JsonDeserialize(as=Rectangle2D.Double.class)
abstract class Rectangle2DAnnotations {
}

@SuppressWarnings("serial")
abstract class Rectangle2DDoubleAnnotations
    extends Rectangle2D.Double {
    @Override @JsonIgnore abstract public Rectangle getBounds();
    @Override @JsonIgnore abstract public Rectangle2D getBounds2D();
    @Override @JsonIgnore abstract public boolean isEmpty();
    @Override @JsonIgnore abstract public double getMinX();
    @Override @JsonIgnore abstract public double getMaxX();
    @Override @JsonIgnore abstract public double getMinY();
    @Override @JsonIgnore abstract public double getMaxY();
    @Override @JsonIgnore abstract public Rectangle2D getFrame();
    @Override @JsonIgnore abstract public double getCenterX();
    @Override @JsonIgnore abstract public double getCenterY();
}

class BasicStrokeAnnotations {
    BasicStrokeAnnotations
        (@JsonProperty("lineWidth") float lineWidth,
         @JsonProperty("endCap") int endCap,
         @JsonProperty("lineJoin") int lineJoin,
         @JsonProperty("miterLimit") float miterLimit,
         @JsonProperty("dashArray") float[] dashArray,
         @JsonProperty("dashPhase") float dashPhase) {}
}

@SuppressWarnings("serial")
class DecimalFormatAnnotations extends DecimalFormat {
    @Override @JsonProperty("pattern") public String toPattern() {
        return null;
    }
    DecimalFormatAnnotations(@JsonProperty("pattern") String pattern) {}
}

@SuppressWarnings("serial")
@JsonTypeInfo(
              use = JsonTypeInfo.Id.NAME,
              include = JsonTypeInfo.As.PROPERTY,
              property = "type")
@JsonSubTypes({
        @Type(value=DecimalFormat.class, name = "DecimalFormat") })
@JsonIgnoreProperties
    ({"groupingUsed", "parseIntegerOnly",
      "maximumIntegerDigits",
      "minimumIntegerDigits",
      "maximumFractionDigits",
      "minimumFractionDigits",
      "positivePrefix",
      "positiveSuffix",
      "negativePrefix",
      "negativeSuffix",
      "multiplier",
      "groupingSize",
      "decimalSeparatorAlwaysShown",
      "parseBigDecimal",
      "roundingMode",
      "decimalFormatSymbols",
      "currency"})
abstract class NumberFormatAnnotations extends NumberFormat {
}

@SuppressWarnings("serial")
@JsonIgnoreProperties
    ({"alpha", "red", "green", "blue",
            "colorSpace", "transparency"})
abstract class ColorAnnotations extends Color {
    ColorAnnotations(@JsonProperty("rgb") int rgb) {
        super(rgb);
    }
}

@JsonTypeInfo(
              use = JsonTypeInfo.Id.NAME,
              include = JsonTypeInfo.As.PROPERTY,
              property = "decoration")
@JsonSubTypes({
        @Type(value=CuspFigure.class, name = "curve"),
        @Type(value=AnchoredLabel.class, name = "label"),
        @Type(value=LinearRuler.class, name = "ruler"),
        @Type(value=Arrow.class, name = "arrow"),
        @Type(value=TieLine.class, name = "tie line")
            })
interface DecoratedAnnotations extends Decorated {
}
