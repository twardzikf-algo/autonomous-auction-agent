package de.dailab.jiactng.aot.auction.beans;

import de.dailab.jiactng.aot.auction.onto.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/*******************************************************
 * CLOSEDSTACK:
 * - stores items that are already sold in a list
 *   as ClosedItem objects
 * - allows getting particular Elements from the list,
 *   adding to the top (newly sold Resources)
 * - also allows basic filtering functions
 *******************************************************/

public class ClosedStack {
    private List<ClosedItem> items;

    public ClosedStack() {
        this.items = new ArrayList<>();
    }

    public ClosedStack(List<ClosedItem> items) {
        this.items = items;
    }

    public void add(Resource resource, Integer myPrice, Integer soldPrice, Boolean won) {
        items.add(0, new ClosedItem(resource, myPrice, soldPrice, won));
    }

    public ClosedItem get() {
        return items.get(0);
    }

    public ClosedItem get(int i) {
        return items.get(i);
    }

    public List<ClosedItem> getAll() {
        return this.items;
    }

    public int countAll() {
        return this.items.size();
    }

    public ClosedStack getByResource(Resource resource) {
        return new ClosedStack(items.stream().filter(p -> p.getResource() == resource).collect(Collectors.toList()));
    }

    public ClosedStack getByWon(Boolean won) {
        return new ClosedStack(items.stream().filter(p -> p.getWon() == won).collect(Collectors.toList()));
    }

    public String toString() {
        return items.stream().map(Object::toString).collect(Collectors.joining(",\n"));
    }
}
