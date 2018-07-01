package de.dailab.jiactng.aot.auction.beans;

import java.io.Serializable;

import de.dailab.jiactng.agentcore.AbstractAgentBean;

import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.agentcore.ontology.IActionDescription;


import java.time.LocalDateTime;

/**
 * TODO Implement this class.
 */
public class BidderBean extends AbstractAgentBean {

	/*
	 * TODO
	 * add properties for e.g. the multicast message group, or the bidderID
	 * add getter methods for those properties so they can be set in the
	 * Spring configuration file
	 */
	private String bidderId;
	private String messageGroup;

	public String getBidderId() {
		return this.bidderId;
	}

	public String getMessageGroup() {
		return this.messageGroup;
	}

	public void setBidderId(String bidderId) {
		this.bidderId = bidderId;
	}

	public void setMessageGroup(String messageGroup) {
		this.messageGroup = messageGroup;
	}
	/*
	 * TODO
	 * when the agent starts, use the action ICommunicationBean.ACTION_JOIN_GROUP
	 * to join the multicast message group "de.dailab.jiactng.aot.auction"
	 * for the final competition, or a group of your choosing for testing
	 * make sure to use the same message group as the auctioneer!
	 */
	@Override
    public void doStart() throws Exception {

        IActionDescription joinAction = retrieveAction(ICommunicationBean.ACTION_JOIN_GROUP);
        this.memory.attach(new MessageObserver(), new JiacMessage());
        log( "INFO", "Agent started.");

    }

    /*
	 * TODO
	 * when the agent starts, create a message observer and attach it to the
	 * agent's memory. that message observer should then handle the different
	 * messages and send a suitable Bid in reply. see the readme and the
	 * sequence diagram for the expected order of messages.
	 */

    @SuppressWarnings("serial")
    class MessageObserver implements SpaceObserver<IFact> {

        @SuppressWarnings("rawtypes")
        @Override
        public void notify(SpaceEvent<? extends IFact> event) {
            // check the type of the event
            if (event instanceof WriteCallEvent) {
                log("INFO","New message came!");
                // we know it's a message due to the template, but we have to check the content
                JiacMessage message = (JiacMessage) ((WriteCallEvent) event).getObject();
                /*
                if (message.getPayload() instanceof OfferFact) {

                    OfferFact offerFact = (OfferFact) message.getPayload();
                    log("Receiving offer " + offerFact);

                    // compare to best offer and update if lower
                    if (bestOffer == null || offerFact.getOffer() < bestOffer.getOffer()) {
                        bestOffer = offerFact;
                        log("New best offer " + bestOffer);
                    }
                }
                */
                // once handled, the message should be removed from memory
                memory.remove(message);
            }
        }
    }


	/*
	 * TODO You will receive your initial "Wallet" from the auctioneer, but
	 * afterwards you will have to keep track of your spendings and acquisitions
	 * yourself. The Auctioneer will do so, as well.
	 */


	/*
	 * our custom  helper functions
	 */

    private void log(String type, String msg) {
        String time = LocalDateTime.now().toString();
        System.out.println("["+time+"]["+this.bidderId + "]["+type+"]: " + msg);
    }
}
