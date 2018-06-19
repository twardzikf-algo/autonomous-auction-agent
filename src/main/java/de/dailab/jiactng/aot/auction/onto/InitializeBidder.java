package de.dailab.jiactng.aot.auction.onto;

import java.util.List;

import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * Sent in response to Register message. Contains bidder's
 * initial Wallet, i.e. their starting resources, and credits.
 * Also contains initial list of items to be sold.
 */
public class InitializeBidder implements IFact {

	private static final long serialVersionUID = 1061663542225958995L;
	
	/** the ID of the bidder to whom this message is sent */
	private final String bidderId;
	
	/** the bidder's initial wallet */
	private final Wallet wallet;
	
	/** the initial list of items to be sold */
	private final List<Resource> items;
	
	
	public InitializeBidder(String bidderId, Wallet wallet, List<Resource> items) {
		this.bidderId = bidderId;
		this.wallet = wallet;
		this.items = items;
	}
	
	public String getBidderId() {
		return bidderId;
	}
	
	public Wallet getWallet() {
		return wallet;
	}
	
	public List<Resource> getItems() {
		return items;
	}
	
	@Override
	public String toString() {
		return String.format("InitializeBidder(%s, %s, %s)", bidderId, wallet, items);
	}
	
}
