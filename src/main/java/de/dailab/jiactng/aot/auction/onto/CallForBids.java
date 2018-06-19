package de.dailab.jiactng.aot.auction.onto;

import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * Sent by the auctioneer to advertise a new call.
 */
public class CallForBids implements IFact {

	private static final long serialVersionUID = -2459640599311665642L;

	/** the id of the call-for-bids */
	private final Integer callId;
	
	/** the type of resource */
	private final Resource resource;
	
	/** the minimum offer */
	private final Integer minOffer;
	
	
	public CallForBids(Integer id, Resource resource, Integer minOffer) {
		this.callId = id;
		this.resource = resource;
		this.minOffer = minOffer;
	}

	public Integer getCallId() {
		return callId;
	}
	
	public Resource getResource() {
		return resource;
	}
	
	public Integer getMinOffer() {
		return minOffer;
	}
	
	@Override
	public String toString() {
		return String.format("CallForBids(%d, %s, %d)", callId, resource, minOffer);
	}
	
}
