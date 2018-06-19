package de.dailab.jiactng.aot.auction.onto;

import java.util.HashMap;
import java.util.Map;

import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * Current "wallet", or "inventory" of a bidder. Bidders have to keep track
 * of their own wallet, but the auctioneer does the same, to prevent cheating.
 */
public class Wallet implements IFact {

	private static final long serialVersionUID = -7219909278211999621L;

	/** the ID of the bidder whom this wallet belongs to */
	private String bidderId;
	
	/** remaining credits */
	private Integer credits;
	
	/** resources currently belonging to the bidder */
	private final Map<Resource, Integer> resources;
	
	
	public Wallet(String bidderId, Integer credits) {
		this.bidderId = bidderId;
		this.credits = credits;
		this.resources = new HashMap<>();
	}
	
	public String getBidderId() {
		return bidderId;
	}
	
	public Integer getCredits() {
		return credits;
	}
	
	public Integer updateCredits(int delta) {
		this.credits += delta;
		return this.credits;
	}

	public void add(Resource resource, int delta) {
		this.resources.put(resource, get(resource) + delta);
	}
	
	public Integer get(Resource resource) {
		return this.resources.computeIfAbsent(resource, r -> 0);
	}
	
	public long getValue() {
		return Resource.calculateValue(resources);
	}
	
	@Override
	public String toString() {
		return String.format("Wallet(bidderId=%s, credits=%d, resources=%s, value=%d)",
				bidderId, credits, resources, getValue());
	}
	
}
