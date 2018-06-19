package de.dailab.jiactng.aot.auction.onto;

import de.dailab.jiactng.agentcore.knowledge.IFact;

/**
 * Inform the seller of an item of the result, i.e. whether the item was sold,
 * and if so at what price.
 */
public class InformSell implements IFact {

	private static final long serialVersionUID = -6410327995358801993L;

	public enum SellType { SOLD, NOT_SOLD, INVALID }

	/** the outcome of the sale */
	private final SellType type;
	
	/** the ID of the call when the item was sold */
	private final Integer callId;
	
	/** the type of resource sold */
	private final Resource resource;
	
	/** the price, if sold, else null */
	private final Integer price;
	
	/** what the seller is charged (not included in price) */
	private final Integer charge;
	
	
	public InformSell(SellType type, Integer callId, Resource resource, Integer price, Integer charge) {
		this.type = type;
		this.callId = callId;
		this.resource = resource;
		this.price = price;
		this.charge = charge;
	}
	
	public Integer getCallId() {
		return callId;
	}
	
	public Resource getResource() {
		return resource;
	}
	
	public SellType getType() {
		return type;
	}
	
	public Integer getPrice() {
		return price;
	}
	
	public Integer getCharge() {
		return charge;
	}
	
	@Override
	public String toString() {
		return String.format("InformSell(%s, %d, %s, %d, %d)", type, callId, resource, price, charge);
	}
	
}
