package de.dailab.jiactng.aot.auction.onto;

import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * Message containing a Bid by a bidder for buying the Resource
 * in the current call.
 */
public class Bid implements IFact {

	private static final long serialVersionUID = -123356410215917626L;

	/** ID of the bidder issuing this bid */
	private final String bidderId;
	
	/** the ID of the call-for-bids */
	private final Integer callId;
	
	/** the price offered for the resource */
	private final Integer offer;
	
	
	public Bid(String bidderId, Integer callId, Integer offer) {
		this.bidderId = bidderId;
		this.callId = callId;
		this.offer = offer;
	}

	public String getBidderId() {
		return bidderId;
	}
	
	public Integer getCallId() {
		return callId;
	}
	
	public Integer getOffer() {
		return offer;
	}
	
	public String toString() {
		return String.format("Bid(%s, %d, %d)", bidderId, callId, offer);
	}

}
