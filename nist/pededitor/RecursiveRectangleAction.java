/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Rectangle;
import java.util.concurrent.RecursiveAction;

/** Subclass of RecursiveAction intended for splitting parallel
    rectangle processing tasks into subtasks. */
public class RecursiveRectangleAction extends RecursiveAction {
    private static final long serialVersionUID = -2489117155798819683L;

    final public RectangleProcessor processor;
    final public Rectangle rect;
    final public double multithreadingCostThreshold;

    public static int totalJobs = 0;

    /** @param rect The region to be processed

        @param processor The RectangleProcessor to apply to rect

        @param multithreadingCostThreshold This task will be split
        into subtasks until each subtask's estimatedRunTime() does
        not exceed this threshold. */
    RecursiveRectangleAction(RectangleProcessor processor, Rectangle rect,
                         double multithreadingCostThreshold) {
        this.processor = processor;
        this.rect = rect;
        this.multithreadingCostThreshold = multithreadingCostThreshold;
        ++totalJobs;
    }

    @Override protected void compute() {
        if (rect.width >= 2
            && (processor.estimatedRunTime(rect)
                >= multithreadingCostThreshold)) {
            RecursiveRectangleAction leftHalf = new RecursiveRectangleAction
                (processor,
                 new Rectangle(rect.x, rect.y, rect.width/2, rect.height),
                 multithreadingCostThreshold);
            RecursiveRectangleAction rightHalf = new RecursiveRectangleAction
                (processor,
                 new Rectangle(rect.x + rect.width/2, rect.y,
                               (rect.width + 1)/2, rect.height),
                 multithreadingCostThreshold);
            invokeAll(leftHalf, rightHalf);
        } else {
            processor.run(rect);
        }
    }
}
