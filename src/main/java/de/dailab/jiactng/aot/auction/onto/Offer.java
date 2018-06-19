package de.dailab.jiactng.aot.auction.onto;

import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * Sent by the Bidder to offer one of its own resources for sale.
 * If reservation price is not met, the Bidder gets the resource back.
 */
public class Offer implements IFact {

	private static final long serialVersionUID = 3456389482058944999L;

	/** the ID of the bidder making the offer */
	private final String bidderId;
	
	/** the type of resource to sell */
	private final Resource resource;
	
	/** the minimum price expected by bids */
	private final Integer reservationPrice;
	
	
	public Offer(String bidderId, Resource resource, Integer price) {
		this.bidderId = bidderId;
		this.resource = resource;
		this.reservationPrice = price;
	}

	public String getBidderId() {
		return bidderId;
	}
	
	public Resource getResource() {
		return resource;
	}

	public Integer getReservationPrice() {
		return reservationPrice;
	}
	
	@Override
	public String toString() {
		return String.format("Offer(%s, %s, %d)", bidderId, resource, reservationPrice);
	}
	
}
