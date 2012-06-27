package gov.nist.pededitor;

public enum Side {
    TOP (false, "Top"), BOTTOM (false, "Bottom"), LEFT (true, "Left"),
    RIGHT (true, "Right");

    private final boolean mIsX;
    private final String name;

    Side(boolean isX, String name) {
        mIsX = isX;
        this.name = name;
    }

    public boolean isX() {
        return mIsX;
    }

    public String dimensionName() {
        return isX() ? "width" : "height";
    }

    @Override
	public String toString() {
        return name;
    }
 };
