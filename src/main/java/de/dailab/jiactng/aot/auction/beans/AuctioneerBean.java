package de.dailab.jiactng.aot.auction.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.comm.CommunicationAddressFactory;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.agentcore.ontology.IActionDescription;
import de.dailab.jiactng.aot.auction.onto.Bid;
import de.dailab.jiactng.aot.auction.onto.CallForBids;
import de.dailab.jiactng.aot.auction.onto.EndAuction;
import de.dailab.jiactng.aot.auction.onto.InformBuy;
import de.dailab.jiactng.aot.auction.onto.InformBuy.BuyType;
import de.dailab.jiactng.aot.auction.onto.InformSell;
import de.dailab.jiactng.aot.auction.onto.InformSell.SellType;
import de.dailab.jiactng.aot.auction.onto.InitializeBidder;
import de.dailab.jiactng.aot.auction.onto.Item;
import de.dailab.jiactng.aot.auction.onto.Offer;
import de.dailab.jiactng.aot.auction.onto.Register;
import de.dailab.jiactng.aot.auction.onto.Resource;
import de.dailab.jiactng.aot.auction.onto.StartAuction;
import de.dailab.jiactng.aot.auction.onto.Wallet;

/**
 * This bean defines the auctioneer role. You are invited to inspect the code
 * and to understand what it does, and you can use the auctioneer agent to
 * test your bidder agent's behavior, but you should not modify this code.
 * (If you do, your code will most likely not work in the final competition.)
 * 
 * This will send call-for-bids to the agents on the multicast-message-channel
 * and handle the bids that come back, determine the winner, check the balances,
 * etc. Please see the included sequence diagram for an overview of the protocol.
 */
public class AuctioneerBean extends AbstractAgentBean {

	/** the current state of the auction (not to be confused with the lifecycle state of the agent) */
	enum Phase { STARTING, REGISTRATION, BIDDING, EVALUATION, END }
	
	/*
	 * CONFIGURATION
	 * those can be set in the Spring configuration file
	 */
	
	/** initial balance for each bidder */
	private Integer initialBalance;
	
	/** initial number of J or K resources to give to each bidder */
	private Integer initialJKcount;
	
	/** random seed for generating items */
	private Long randomSeed;
	
	/** number of items to generate */
	private Integer numItems;
	
	/** minimum offer; also selling price in case of just one bid */
	private Integer minOffer;
	
	/** message group where to send multicast messages to */
	private String messageGroup;
	
	/*
	 * STATE
	 */
	
	/** the items to be sold */
	private LinkedList<Item> items;
	
	/** keeping track of the wallets of the different bidders */
	private Map<String, Wallet> wallets;
	
	/** communication addresses of the bidders */
	private Map<String, ICommunicationAddress> messageBoxes;
	
	/** the current item */
	private Item current;
	
	/** provider for unique call IDs */
	private AtomicInteger callIdProvider;
	
	/** the current phase */
	private Phase phase;
	
	/*
	 * LIFECYCLE METHODS
	 */

	@Override
	public void doStart() throws Exception {
		// initialize items as a stack; items offered by bidders are pushed to the top
		callIdProvider = new AtomicInteger();
		items = new LinkedList<>();
		for (Resource res : Resource.generateRandomResources(numItems, randomSeed)) {
			items.add(new Item(callIdProvider.incrementAndGet(), res, null, minOffer));
		}
		
		// attach memory observer for handling messages
		this.memory.attach(new MessageObserver(), new JiacMessage());
		
		current = null;
		phase = Phase.STARTING;
	}
	
	@Override
	public void execute() {
		log.info("Current Phase: " + phase);
		log.info("Messages in Memory: " + memory.readAll(new JiacMessage()).size());
		
		switch (phase) {
		case STARTING:
			// send initial start auction message
			sendGroup(new StartAuction());
			phase = Phase.REGISTRATION;
			break;
		
		case REGISTRATION:
			// initialize maps
			this.wallets = new HashMap<>();
			this.messageBoxes = new HashMap<>();
			
			// get all messages from memory, inspect registrations
			for (JiacMessage message : memory.readAll(new JiacMessage())) {
				if (! (message.getPayload() instanceof Register)) continue;
				Register register = (Register) message.getPayload();
				String bidder = register.getBidderId();
				messageBoxes.put(bidder, message.getSender());
				
				// initialize wallet
				Wallet wallet = new Wallet(bidder, initialBalance);
				wallet.add(Math.random() < 0.5 ? Resource.J : Resource.K, initialJKcount);
				wallets.put(bidder, wallet);

				// send initialization message
				List<Resource> rawItems = items.stream().map(Item::getType).collect(Collectors.toList());
				send(new InitializeBidder(bidder, wallet, rawItems), bidder);
				
				// once handled, remove message from memory
				memory.remove(message);
			}

			// next phase: bidding
			phase = Phase.BIDDING;
			break;
			
		case BIDDING:
			// get next item to sell from stack
			current = items.pop();
			
			if (current.getSeller() != null) {
				// check whether seller actually has the advertised item
				Wallet wallet = wallets.get(current.getSeller());
				if (wallet.get(current.getType()) == 0) {
					// offer is invalid, but seller is still charged
					log.warn("Invalid Offer: Bidder does not have Resource");
					Integer charge = calcCharge(current.getReservationPrice());
					wallet.updateCredits(- charge);
					send(new InformSell(SellType.INVALID, current.getCallId(), current.getType(), null, charge), current.getSeller());
					// do not send bids or change phase, instead skip to the next item
					break;
				}
			}
			
			// start new round, send request to message group
			sendGroup(new CallForBids(current.getCallId(), current.getType(), current.getReservationPrice()));
			
			// next phase: evaluation
			phase = Phase.EVALUATION;
			break;

		case EVALUATION:
			Bid first = null;
			Bid secnd = null;
			Set<Bid> invalid = new HashSet<>();
			Collection<Bid> bids = new ArrayList<>(); 
			
			// get all messages from memory, inspect bids
			for (JiacMessage message : memory.readAll(new JiacMessage())) {
				if (! (message.getPayload() instanceof Bid)) continue;
				Bid bid = (Bid) message.getPayload();

				// if bidder ID does not match sender, discard Bid
				if (checkMessage(message, bid.getBidderId())) {
					bids.add(bid);
					
					// get wallet for bidder
					Wallet wallet = wallets.get(bid.getBidderId());
	
					// check offers, memorize best two
					if (current.getCallId().equals(bid.getCallId()) && 
							bid.getOffer() >= current.getReservationPrice() &&
							bid.getOffer() <= wallet.getCredits()) {
						// offer is valid... but is it a new first or second best offer?
						if (first == null || first.getOffer() < bid.getOffer()) {
							secnd = first;
							first = bid;
						} else if (secnd == null || secnd.getOffer() < bid.getOffer()) {
							secnd = bid;
						}
					} else {
						log.warn("Invalid Bid: Bidder does not have enough money");
						invalid.add(bid);
					}
				} else {
					log.warn("Discarding Bid with mismatched bidderID");
				}
				memory.remove(message);
			}
			
			log.info("Evaluating bids");
			wallets.entrySet().forEach(log::info);
			bids.forEach(log::info);
			
			// send out inform messages
			// if >1 bid then 2nd price, else if 1 bid then res price, else null
			Integer price = secnd != null ? secnd.getOffer() : (first != null ? current.getReservationPrice() : null);
			for (Bid bid : bids) {
				BuyType type = null;
				if (bid == first) {
					// update wallet
					Wallet w = wallets.get(bid.getBidderId());
					w.add(current.getType(), 1);
					w.updateCredits(- price);

					type = BuyType.WON;
				} else if (invalid.contains(bid)) {
					type = BuyType.INVALID;
				} else {
					type = BuyType.LOST;
				}
				// send inform
				send(new InformBuy(type, current.getCallId(), current.getType(), price), bid.getBidderId());
			}

			if (current.getSeller() != null) {
				// update seller's wallet
				Wallet wallet = wallets.get(current.getSeller());
				Integer charge = calcCharge(current.getReservationPrice());
				if (first != null) {
					wallet.updateCredits(price);
					wallet.add(current.getType(), -1);
				}
				wallet.updateCredits(-charge);
				
				// inform seller
				send(new InformSell(first != null ? SellType.SOLD : SellType.NOT_SOLD,
						current.getCallId(), current.getType(), price, charge), current.getSeller());
			}

			// next phase: bidding or end
			phase = items.isEmpty() ? Phase.END : Phase.BIDDING;
			break;
	
		case END:
			// send information about winner and winner's final wallet to all bidders
			Wallet winner = wallets.values().stream().max(Comparator.comparing(Wallet::getValue)).get();
			sendGroup(new EndAuction(winner.getBidderId(), winner));
			
			// stop regular execution
			setExecutionInterval(0);
			break;
		}
	}

	/*
	 * MESSAGE HANDLING
	 */
	
	/**
	 * This memory observer will be triggered whenever a JIAC message arrives.
	 * This receives Offers, which can be sent at any time (not bound to the
	 * current phase of the auction). Offers will be pushed to the item stack.
	 */
	@SuppressWarnings("serial")
	class MessageObserver implements SpaceObserver<IFact> {

		@SuppressWarnings("rawtypes")
		@Override
		public void notify(SpaceEvent<? extends IFact> event) {
			// check the type of the event
			if (event instanceof WriteCallEvent) {
				// we know it's a message due to the template, but we have to check the content
				JiacMessage message = (JiacMessage) ((WriteCallEvent) event).getObject();
				log.info("Received " + message.getPayload());
				
				if (message.getPayload() instanceof Offer) {
					Offer offer = (Offer) message.getPayload();
				
					if (checkMessage(message, offer.getBidderId())) {
						items.push(new Item(callIdProvider.incrementAndGet(), offer.getResource(),
								offer.getBidderId(), offer.getReservationPrice()));
					} else {
						log.warn("Discarding Offer with mismatched bidderID");
					}
					
					// once handled, the message should be removed from memory
					memory.remove(message);
				}
				// other messages remain in memory, those are handled in execute()
			}
		}		
	}
	
	/*
	 * HELPER METHODS
	 * some small utility functions for commonly used procedures
	 */

	/**
	 * send message to bidder with given bidder ID
	 */
	private void send(IFact payload, String bidderId) {
		log.info(String.format("Sending %s to %s", payload, bidderId));
		send(payload, messageBoxes.get(bidderId));
	}

	/**
	 * send message to multicast communication group
	 */
	private void sendGroup(IFact payload) {
		log.info(String.format("Sending %s to group", payload));
		send(payload, CommunicationAddressFactory.createGroupAddress(messageGroup));
	}
	
	/**
	 * send message to given communication address
	 */
	private void send(IFact payload, ICommunicationAddress address) {
		JiacMessage message = new JiacMessage(payload);
		IActionDescription sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		invoke(sendAction, new Serializable[] {message, address});
	}
	
	/**
	 * Calculate fee charged to the seller as 10% of reservation price or 1 credit
	 */
	private int calcCharge(int reservationPrice) {
		return Math.max(1, (int) Math.ceil(reservationPrice * 0.1));
	}
	
	/**
	 * Fraud prevention
	 */
	private boolean checkMessage(JiacMessage msg, String bidderId) {
		return msg.getSender().equals(messageBoxes.get(bidderId));
	}

	
	/*
	 * GETTERS AND SETTERS
	 * needed for setting properties via Spring configuration file
	 */

	public void setInitialBalance(Integer initialBalance) {
		this.initialBalance = initialBalance;
	}
	
	public void setInitialJKcount(Integer initialJKcount) {
		this.initialJKcount = initialJKcount;
	}
	
	public void setNumItems(Integer numItems) {
		this.numItems = numItems;
	}
	
	public void setRandomSeed(Long randomSeed) {
		this.randomSeed = randomSeed;
	}
	
	public void setMinOffer(Integer minOffer) {
		this.minOffer = minOffer;
	}
	
	public void setMessageGroup(String messageGroup) {
		this.messageGroup = messageGroup;
	}
}
