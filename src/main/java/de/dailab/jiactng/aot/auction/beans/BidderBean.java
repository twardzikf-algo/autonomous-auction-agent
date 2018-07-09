package de.dailab.jiactng.aot.auction.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.stream.IntStream;

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

import static de.dailab.jiactng.aot.auction.onto.Resource.*;
import static de.dailab.jiactng.aot.auction.onto.Resource.Q;
import static de.dailab.jiactng.aot.auction.onto.Resource.Z;


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
 *  - rebuild the design of Calculator, including HIstory and BuyFact classes,
 *    there is a lot of redundancy there and
 *
 * IN PROGRESS:
 *  - implementing max end and max profit strategies
 *
 *
 * TODO:
 *  - implement specific tactics
 *  - think, where should the resaling functionality be integrated
 *    and when/what should we resell (sounds like part of each specific tactic)
 *
 *************************************************************/

public class BidderBean extends AbstractAgentBean {

    /***************
     * ATTRIBUTES
     ***************/
    private Boolean auctionStarted = Boolean.FALSE;
    private String bidderId;
    private String messageGroup;
    private ICommunicationAddress auctioneer;
    public Brain brain;

    /*********************
     * GETTERS & SETTERS
     *********************/
    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }
    public void setMessageGroup(String messageGroup) {
        this.messageGroup = messageGroup;
    }

    /*********************
     * INITIALIZE AGENT
     *********************/

    @Override
    public void doStart() throws Exception {
        IGroupAddress group = CommunicationAddressFactory.createGroupAddress(this.messageGroup);
        log.info("MESSAGE GROUP:"+this.messageGroup);
        IActionDescription joinAction = retrieveAction(ICommunicationBean.ACTION_JOIN_GROUP);
        invoke(joinAction, new Serializable[]{group});
        this.memory.attach(new MessageObserver(), new JiacMessage());
        log.info("Agent " + bidderId + " started.");
    }

    /*********************
     * MESSAGE OBSERVER
     *********************/

    @SuppressWarnings("serial")
    class MessageObserver implements SpaceObserver<IFact> {
        @Override
        public void notify(SpaceEvent<? extends IFact> event) {

            if (event instanceof WriteCallEvent) {

                JiacMessage message = (JiacMessage) ((WriteCallEvent) event).getObject();
                IFact payload = message.getPayload();

                log.info(payload.toString());

                if (payload instanceof StartAuction) handleStartAuction(message);
                if (payload instanceof CallForBids) handleCallForBids(message);
                if (payload instanceof InitializeBidder) handleInitializeBidder(message);
                if (payload instanceof InformBuy) handleInformBuy(message);
                if (payload instanceof InformSell) handleInformSell(message);
                if (payload instanceof EndAuction) handleEndAuction(message);

                if (brain != null) log.info(brain.getWallet().toString());

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
        if( !auctionStarted ){
            auctioneer = message.getSender();
            send(new Register(bidderId), auctioneer);
            auctionStarted = Boolean.TRUE;
        }

    }

    private void handleInitializeBidder(JiacMessage message) {
        Wallet wallet = ((InitializeBidder) message.getPayload()).getWallet();
        List<Resource> stack = ((InitializeBidder) message.getPayload()).getItems();
        brain = new Brain(bidderId, wallet, stack);
    }

    private void handleCallForBids(JiacMessage message) {
        CallForBids payload = ((CallForBids) message.getPayload());
        brain.newCall(payload.getCallId(), payload.getResource(), payload.getMinOffer());
        if (message.getSender().getName() != this.bidderId) {
            int offer = brain.bid();
            brain.addClosed(payload.getResource(), offer);
            send(new Bid(bidderId, payload.getCallId(), offer), auctioneer);
        }
    }

    private void handleInformBuy(JiacMessage message) {
        InformBuy payload = (InformBuy) message.getPayload();
        brain.removeOpen();
        brain.updateClosed(payload.getPrice(), (payload.getType() == InformBuy.BuyType.WON));
        if (payload.getType() == InformBuy.BuyType.WON) {
            brain.updateWallet(-payload.getPrice());
            brain.updateWallet(payload.getResource(), 1);
        }
        Resource[] reses = new Resource[]{C, D, E, J, K, M, N, W, X, Y, Z, Q};
        for (int i = 0; i < 12; i++) {
            if (brain.sellCalls[i] > 0) {
                log.info("Ziel:" + brain.sellCalls.toString());
                offerResale(reses[i], brain.sellCalls[i]);
            }
        }
    }

    private void handleInformSell(JiacMessage message) {
        InformSell payload = (InformSell) message.getPayload();
        if (payload.getType() == InformSell.SellType.SOLD) {
            brain.updateWallet(payload.getResource(), -1);
            brain.updateWallet(payload.getPrice() - payload.getCharge());
        } else if (payload.getType() == InformSell.SellType.NOT_SOLD) {
            brain.updateWallet(-payload.getCharge());
        } else log.error("Resale offer was invalid!");
    }

    private void handleEndAuction(JiacMessage message) {
        EndAuction payload = (EndAuction) message.getPayload();
        auctionStarted = Boolean.FALSE;
        if (payload.getWinner().equals(bidderId)) log.info("I WON THE AUCTION!!!!!");
        else log.info("I LOST THE AUCTION!!!!!");
    }

    /******************************
     * HANDLER FOR SENDING AN OFFER
     ******************************/
    private void offerResale(Resource resource, Integer price) {
        Offer offer = new Offer(bidderId, resource, price);
        log.info(offer.toString());
        send(offer, auctioneer);
    }
}













