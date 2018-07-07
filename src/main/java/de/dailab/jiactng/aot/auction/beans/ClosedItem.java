package de.dailab.jiactng.aot.auction.beans;

import de.dailab.jiactng.aot.auction.onto.Resource;

/*************************************************************
 * CLOSEDITEM:
 * - single Element of "history" of calls
 * - stores Resource, Price that our agent bidded on him
 *   price, for which was it sold and if we won this resource
 * - also provides standard getters/setters
 *************************************************************/

public class ClosedItem {

    private Resource resource;
    private Integer myPrice;
    private Integer soldPrice;
    private Boolean won;

    public ClosedItem(Resource resource, Integer myPrice, Integer soldPrice, Boolean won) {
        this.resource = resource;
        this.myPrice = myPrice;
        this.soldPrice = soldPrice;
        this.won = won;
    }

    public Integer getMyPrice() {
        return myPrice;
    }

    public void setMyPrice(Integer myPrice) {
        this.myPrice = myPrice;
    }

    public Integer getSoldPrice() {
        return soldPrice;
    }

    public void setSoldPrice(Integer soldPrice) {
        this.soldPrice = soldPrice;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Boolean getWon() {
        return won;
    }

    public void setWon(Boolean won) {
        this.won = won;
    }

    public String toString() {
        return "ClosedItem(resource=" + resource + ";myPrice=" + myPrice + ";soldPrice=" + soldPrice + ";won=" + won + ")";
    }
}
