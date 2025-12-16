public enum GravityDir {
    DOWN(0, 1),
    UP(0, -1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    private final int xSign;
    private final int ySign;

    GravityDir(int xSign, int ySign) {
        this.xSign = xSign;
        this.ySign = ySign;
    }

    public int getXSign() {
        return xSign;
    }

    public int getYSign() {
        return ySign;
    }

    public boolean isVertical() {
        return this == DOWN || this == UP;
    }

    public int gravitySign() {
        return isVertical() ? ySign : xSign;
    }
}
