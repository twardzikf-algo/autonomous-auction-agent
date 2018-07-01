package de.dailab.jiactng.aot.auction.beans;

import java.io.Serializable;

import de.dailab.jiactng.agentcore.AbstractAgentBean;

import de.dailab.jiactng.agentcore.comm.CommunicationAddressFactory;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.IGroupAddress;
import de.dailab.jiactng.aot.auction.onto.*;
import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.agentcore.ontology.IActionDescription;

import java.util.List;

/**
 * TODO Implement this class.
 */
public class BidderBean extends AbstractAgentBean {

    /***********************
     * ATTRIBUTES
     ***********************/
	private String bidderId;
	private String messageGroup;

	private Wallet wallet;
    private List<Resource> stack;

    /***********************
     * GETTERS & SETTERS
     ***********************/

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

    /***********************
     * INITIALIZE AGENT
     ***********************/

	@Override
    public void doStart() throws Exception {
        IGroupAddress group = CommunicationAddressFactory.createGroupAddress("test-group");
        IActionDescription joinAction = retrieveAction(ICommunicationBean.ACTION_JOIN_GROUP);
        invoke( joinAction, new Serializable[] {group});
        this.memory.attach(new MessageObserver(), new JiacMessage());
        log.info( "Agent "+bidderId+" started.");
    }

    /***********************
     * MESSAGE OBSERVER
     ***********************/

    @SuppressWarnings("serial")
    class MessageObserver implements SpaceObserver<IFact> {
        @SuppressWarnings("rawtypes")
        @Override
        public void notify(SpaceEvent<? extends IFact> event) {

            if (event instanceof WriteCallEvent) {

                JiacMessage message = (JiacMessage) ((WriteCallEvent) event).getObject();
                IFact payload = message.getPayload();

                log.info(payload.toString());

                if( payload instanceof StartAuction) handleStartAuction(message);
                if( payload instanceof CallForBids ) handleCallForBids(message);
                if( payload instanceof InitializeBidder ) handleInitializeBidder(message);
                if( payload instanceof InformBuy ) handleInformBuy(message);
                if( payload instanceof InformSell ) handleInformSell(message);
                if( payload instanceof EndAuction ) handleEndAuction(message);

                log.info(wallet.toString());

                memory.remove(message);
            }
        }
    }

    /****************************
     * SEND MESSAGE TO AUCTIONEER
     ****************************/

    private void send(IFact payload, ICommunicationAddress address) {
        JiacMessage message = new JiacMessage(payload);
        IActionDescription sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
        invoke(sendAction, new Serializable[]{message, address});
    }

    /****************************
     * MESSAGE-SPECIFIC HANDLERS
     ****************************/

    private void handleStartAuction(JiacMessage message) {
        send( new Register(bidderId), message.getSender() );
    }

    private void handleInitializeBidder(JiacMessage message) {
        this.wallet = ((InitializeBidder) message.getPayload()).getWallet();
        this.stack = ((InitializeBidder) message.getPayload()).getItems();
    }

    private void handleCallForBids(JiacMessage message) {
        Resource resource = ((CallForBids) message.getPayload()).getResource();
        Integer minOffer = ((CallForBids) message.getPayload()).getMinOffer();
        Calculator calculator = new Calculator(bidderId, resource, minOffer, stack);
        Integer myOffer = calculator.estimateOffer();
        send( new Bid( bidderId, ((CallForBids) message.getPayload()).getCallId(), myOffer),  message.getSender() );
    }

    private void handleInformBuy(JiacMessage message) {
        InformBuy payload = (InformBuy) message.getPayload();
        if(payload.getType() == InformBuy.BuyType.WON) {
            log.info("Won "+payload.getResource().toString()+" for "+payload.getPrice());
            wallet.updateCredits(-payload.getPrice());
            wallet.add(payload.getResource(), 1);
        }
    }

    private void handleInformSell(JiacMessage message) {

    }

    private void handleEndAuction(JiacMessage message) {
        EndAuction payload = (EndAuction) message.getPayload();
        if(payload.getWinner()==bidderId) log.info("I WON THE AUCTION!!!!!");
    }

    /****************************
     * CLASS FOR BID CALCULATIONS
     ****************************/

    private class  Calculator {

        private String bidderId;
        private Integer minOffer;
        Resource resource;
        private List<Resource> stack;

        public Calculator(String bidderId, Resource resource , Integer minOffer, List<Resource> stack) {
            this.bidderId = bidderId;
            this.resource = resource;
            this.minOffer = minOffer;
            this.stack = stack;
        }

        /************************************************
         * CHOOSE METHOD BASED ON AGENT NAME (bidder.xml)
         ************************************************/

        public Integer estimateOffer() {
            switch(bidderId) {
                case "20_uniform": return uniformDistribution();
                case "20_avgBid": return avgBid();
                case "20_maxProfit": return maxProfit();
                case "20_maxEnd": return maxEnd();
                default: return  uniformDistribution();
            }
        }

        /************************************
         * SPECIFICAL BID CALCULATING METHODS
         ************************************/

        private Integer uniformDistribution() {
            //TODO
            return 1;
        }

        private Integer avgBid() {
            //TODO
            return 2;
        }

        private Integer maxProfit() {
            //TODO
            return 3;
        }

        private Integer maxEnd() {
            //TODO
            return 4;
        }

    }


}
