package gov.nist.pededitor;

public class PedPalette extends StringPalette {
    public PedPalette() {
        super
            (new Object[]
                { 
                    // Diacriticals
                    new Object[][] {
                        {"\u0300", "o\u0300"},
                        {"\u0301", "o\u0301"},
                        {"\u0302", "o\u0302"},
                        {"\u0303", "o\u0303"},
                        {"\u0304", "o\u0304"},
                        {"\u0307", "o\u0307"},
                        null,
                    },

                    // Plotting symbols
                    "\u25cb" /* white circle */, "\u25cf" /* black circle */,
                    "\u25a1" /* white square */, "\u25a0" /* black square */, 
                    "\u25b3" /* white triangle !! */, "\u25b2" /* black triangle */,
                    "\u2715" /* multiplication x? */,
                    null,

                    // Miscellaneous
                    "\u221A" /* square root */, "\u2207" /* nabla / gradient symbol */,
                    "\u221E" /* infinity */, "\u222b" /* integral */, "\u222e" /* contour integral */,
                    "\u2192" /* right arrow */, "\u2190" /* left arrow */,
                    "\u21cc" /* right-over-left harpoon */,
                    "\u00b1" /* plus-minus */, "\u2264" /* <= */, "\u2265" /* >= */,
                    null,

                    // Greek lowercase
                    "\u03b1", "\u03b2", "\u03b3",  "\u03b4", "\u03b5", 
                    "\u03b6", "\u03b7", "\u03b8",  "\u03b9", "\u03ba", 
                    "\u03bb", "\u03bc", "\u03bd",  "\u03be", "\u03bf", 
                    "\u03c0", "\u03c1", "\u03c3", "\u03c4", 
                    "\u03c5", "\u03c6", "\u03c7",  "\u03c8", "\u03c9", 
                    null,

                    // Greek uppercase
                    "\u0391", "\u0392", "\u0393",  "\u0394", "\u0395", 
                    "\u0396", "\u0397", "\u0398",  "\u0399", "\u039a", 
                    "\u039b", "\u039c", "\u039d",  "\u039e", "\u039f", 
                    "\u03a0", "\u03a1",  "\u03a3", "\u03a4", 
                    "\u03a5", "\u03a6", "\u03a7",  "\u03a8", "\u03a9", 
                    null,
                });
    }
}