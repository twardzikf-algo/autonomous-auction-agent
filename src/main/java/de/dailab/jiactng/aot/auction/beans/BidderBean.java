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

/******************* PRODUCT BACKLOG *************************
 *
 * DONE:
 *  - requested Initialization. MessageObserver
 *  - Message handling (rather fully functional)
 *  - keeping track of resources and credits in the wallet
 *  - functionality to send an offer of resale of a resource
 *    and correctly handle a response for such request
 *  - Calculator class encapsuling the actual bid calculating functionality
 *  - impementation of specific tactics/techniques happens in prepared method stubs
 *  - Technique/Tactic will be chosen based on bidderId,
 *    e. g. uniform distribution will be used for bidder id 20_uniform
 *
 * TODO:
 *  - implement specific tactics
 *  - think, where should the resaling functionality be integrated
 *    and when/what should we resell (sounds like part of each specific tactic)
 *  - think, if we may need to extract and store some additional facts/knowledge for
 *    further use by specific tactics, like e.g. what did we lost and how high was the bid
 *    or which offers of resale were bought with high price, which not
 *    Dunno, if it is needed/important, just had a random thought about it
 *
 *************************************************************/

public class BidderBean extends AbstractAgentBean {

    /***********************
     * ATTRIBUTES
     ***********************/
	private String bidderId;
	private String messageGroup;

	private Wallet wallet;
    private List<Resource> stack;

    private ICommunicationAddress auctioneer;

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

                if(wallet!=null) log.info(wallet.toString());

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
        auctioneer = message.getSender();
        send( new Register(bidderId), auctioneer );
    }

    private void handleInitializeBidder(JiacMessage message) {
        this.wallet = ((InitializeBidder) message.getPayload()).getWallet();
        this.stack = ((InitializeBidder) message.getPayload()).getItems();
    }

    private void handleCallForBids(JiacMessage message) {
        Resource resource = ((CallForBids) message.getPayload()).getResource();
        Integer minOffer = ((CallForBids) message.getPayload()).getMinOffer();
        Calculator calculator = new Calculator(bidderId, resource, minOffer, stack);
        calculator.printStack();
        Integer myOffer = calculator.estimateOffer();
        if(myOffer <= wallet.getCredits()) {
            send(new Bid(bidderId, ((CallForBids) message.getPayload()).getCallId(), myOffer), auctioneer);
        }
    }

    private void handleInformBuy(JiacMessage message) {
        InformBuy payload = (InformBuy) message.getPayload();
        if(payload.getType() == InformBuy.BuyType.WON) {
            log.info("Won "+payload.getResource().toString()+" for "+payload.getPrice());
            wallet.updateCredits(-payload.getPrice());
            wallet.add(payload.getResource(), 1);
        }
        stack.remove(0);
    }

    private void handleInformSell(JiacMessage message) {
        InformSell payload = (InformSell) message.getPayload();
        if(payload.getType()== InformSell.SellType.SOLD) {
            wallet.add(payload.getResource(), -1);
            wallet.updateCredits(payload.getPrice()-payload.getCharge());
        }
        else if(payload.getType()==InformSell.SellType.NOT_SOLD) {
            wallet.updateCredits(-payload.getCharge());
        }
        else {
            log.error("Resale offer was invalid!");
        }
    }

    private void handleEndAuction(JiacMessage message) {
        EndAuction payload = (EndAuction) message.getPayload();
        if(payload.getWinner()==bidderId) log.info("I WON THE AUCTION!!!!!");
    }

    /******************************
     * HANDLER FOR SENDING AN OFFER
     ******************************/
    private void offerResale(Resource resource, Integer price) {
        Offer offer = new Offer(bidderId, resource, price);
        log.info(offer.toString());
        send(offer, auctioneer );
    }

    /******************************
     * CLASS FOR BID CALCULATIONS
     ******************************/

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

        private void printStack() {
            for( Resource s : stack) System.out.print(s+" ");
            System.out.println();
        }

    }


}
