package de.dailab.jiactng.aot.auction.onto;

import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * Wrapper for an item to be sold, including the type of the item,
 * the seller, if any, and the reservation price.
 */
public class Item implements IFact {

	private static final long serialVersionUID = -537290120804212290L;

	/** the ID of this item */
	private final Integer callId;
	
	/** the resource being sold */
	private final Resource type;
	
	/** the ID of the bidder selling the item, or null */
	private final String seller;
	
	/** the reservation price for this item */
	private final Integer reservationPrice;
	
	
	public Item(Integer callId, Resource type, String seller, Integer resPrice) {
		this.callId = callId;
		this.type = type;
		this.seller = seller;
		this.reservationPrice = resPrice;
	}

	public Integer getCallId() {
		return callId;
	}
	
	public Resource getType() {
		return type;
	}
	
	public String getSeller() {
		return seller;
	}
	
	public Integer getReservationPrice() {
		return reservationPrice;
	}
	
	@Override
	public String toString() {
		return String.format("Item(%d, %s, %s, %d)", callId, type, seller, reservationPrice);
	}
}
