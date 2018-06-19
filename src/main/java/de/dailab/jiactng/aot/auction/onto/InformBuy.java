package de.dailab.jiactng.aot.auction.onto;

import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * Sent by auctioneer to bidder to inform about the result of the auction.
 * Contains whether the bidder has won the item or not, or whether its bid
 * was invalid, and the price the resource was finally sold. If the bidder
 * did not win, it does not tell who won instead, though.
 */
public class InformBuy implements IFact {

	private static final long serialVersionUID = -7597674901576948939L;

	public enum BuyType { WON, LOST, INVALID }
	
	/** the outcome of the call */
	private final BuyType type;
	
	/** the ID of the call in question */
	private final Integer callId;
	
	/** the resource being sold */
	private final Resource resource;
	
	/** the price the resource was sold at, or null */
	private final Integer price;
	
	
	public InformBuy(BuyType type, Integer callId, Resource resource, Integer price) {
		this.type = type;
		this.callId = callId;
		this.resource = resource;
		this.price = price;
	}
	
	public Integer getCallId() {
		return callId;
	}
	
	public Resource getResource() {
		return resource;
	}
	
	public BuyType getType() {
		return type;
	}
	
	public Integer getPrice() {
		return price;
	}
	
	@Override
	public String toString() {
		return String.format("InformBuy(%s, %d, %s, %d)", type, callId, resource, price);
	}
	
}
