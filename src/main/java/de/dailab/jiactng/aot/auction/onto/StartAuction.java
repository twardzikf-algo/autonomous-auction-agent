package de.dailab.jiactng.aot.auction.onto;

import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * This message is sent to all Bidders at the very beginning
 * of the auction. Bidders should reply with Register.
 * 
 * This message does not carry any content; it just indicates the start
 * of a new auction, and to make the auctioneer known to the bidders.
 */
public class StartAuction implements IFact {

	private static final long serialVersionUID = -3738971099847743500L;

	@Override
	public String toString() {
		return String.format("StartAuction()");
	}
	
}
