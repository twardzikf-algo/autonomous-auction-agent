package de.dailab.jiactng.aot.auction.beans;

import de.dailab.jiactng.aot.auction.onto.Resource;

import java.util.List;
import java.util.stream.Collectors;

/**************************************************************
 * OPENSTACK:
 * - stores resources that are yet to be bidded on in a list
 * - allows getting particular Elements from the list,
 *   removing from the top (sold Resources)
 * - also allows basic filtering functions
 **************************************************************/
public class OpenStack {
    private List<Resource> items;

    public OpenStack(List<Resource> items) {
        this.items = items;
    }

    public void remove() {
        if(!items.isEmpty()) items.remove(0);
    }

    public Resource get() {
        return items.get(0);
    }

    public Resource get(int i) {
        return items.get(i);
    }

    public List<Resource> getAll() {
        return this.items;
    }

    public int countAll() {
        return this.items.size();
    }

    public List<Resource> getByResource(Resource resource) {
        return items.stream().filter(p -> p == resource).collect(Collectors.toList());
    }

    public int countByResource(Resource resource) {
        return items.stream().filter(p -> p == resource).collect(Collectors.toList()).size();
    }

    public String toString() {
        return "OpenStack(items=" + items + ")";
    }
}