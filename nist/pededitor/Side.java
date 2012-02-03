package gov.nist.pededitor;

public enum Side {
    TOP (false), BOTTOM (false), LEFT (true), RIGHT (true);

    private final boolean mIsX;

    Side(boolean isX) {
        mIsX = isX;
    }

    public boolean isX() {
        return mIsX;
    }

    public String dimensionName() {
        return isX() ? "width" : "height";
    }
 };
