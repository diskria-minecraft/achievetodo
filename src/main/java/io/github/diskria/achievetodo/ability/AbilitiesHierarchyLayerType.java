package io.github.diskria.achievetodo.ability;

public enum AbilitiesHierarchyLayerType {

    MAIN(1),
    ACTIONS(2),
    BLOCKS(2),
    UPGRADE(2),
    FOOD(4),
    TRADING(2),
    LANDMARK(2);

    private final int rowsCount;

    AbilitiesHierarchyLayerType(int rowsCount) {
        this.rowsCount = rowsCount;
    }

    public int getRowsCount() {
        return rowsCount;
    }
}
