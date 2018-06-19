package de.dailab.jiactng.aot.auction.onto;

import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * Send by Bidder to register itself at beginning of auction.
 * Auctioneer will respond with InitializeBidder message.
 */
public class Register implements IFact {

	private static final long serialVersionUID = -8788636218469439819L;

	/** the (self-chosen) ID of the registering bidder; the same ID
	 *  has to be used in all subsequent communication */
	private final String bidderId;
	
	
	public Register(String bidderId) {
		this.bidderId = bidderId;
	}
	
	public String getBidderId() {
		return bidderId;
	}
	
	@Override
	public String toString() {
		return String.format("Register(%s)", bidderId);
	}
	
}
