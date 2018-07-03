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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;

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
    private ICommunicationAddress auctioneer;

	private Wallet wallet;
    private List<Resource> currentStack;
    private List<Resource> initialStack;
    private History history = new History();

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
        this.currentStack = ((InitializeBidder) message.getPayload()).getItems();
        this.initialStack = ((InitializeBidder) message.getPayload()).getItems();
    }

    private void handleCallForBids(JiacMessage message) {
        Resource resource = ((CallForBids) message.getPayload()).getResource();
        Integer minOffer = ((CallForBids) message.getPayload()).getMinOffer();
        Integer callId = ((CallForBids) message.getPayload()).getCallId();
        Calculator calculator = new Calculator(bidderId, callId, resource, minOffer, currentStack, initialStack, history);
        //calculator.printStack();
        Integer myPrice = calculator.estimateOffer();

        if(myPrice <= wallet.getCredits()) {
            history.add(resource, myPrice, null, null);
            send(new Bid(bidderId, ((CallForBids) message.getPayload()).getCallId(), myPrice), auctioneer);
        }
    }

    private void handleInformBuy(JiacMessage message) {
        InformBuy payload = (InformBuy) message.getPayload();
        currentStack.remove(0);
        history.get().setSoldPrice(payload.getPrice());
        if(payload.getType() == InformBuy.BuyType.WON) {
            history.get().setWon(Boolean.TRUE);
            log.info("Won "+payload.getResource().toString()+" for "+payload.getPrice());
            wallet.updateCredits(-payload.getPrice());
            wallet.add(payload.getResource(), 1);
        }
        else history.get().setWon(Boolean.FALSE);
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
        if(payload.getWinner().equals(bidderId)) log.info("I WON THE AUCTION!!!!!");
        else log.info("I LOST THE AUCTION!!!!!");
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

        /**************************************************************************************
         * ATTENTION!
         * it works, but design is of rather poor quality in my opinion, i should use wallet for parts of information isntead of
         * filtering stacks over and over, will try to think of something better and rebuild it
         *************************************************************************************/
        private String bidderId; // to choose proper method based on agent's name
        private Integer callId; // which round is it - looks like curently not needed too
        private Integer minOffer; // reservation price, in our case it should be always 0,so probably not needed
        Resource resource; //resource being bidded
        private List<Resource> currentStack; // gets smaller each time an item is sold
        private List<Resource> initialStack; // unmodified stack from the beginning of auction
        private History history;

        public Calculator(String bidderId, Integer callId, Resource resource , Integer minOffer, List<Resource> currentStack,  List<Resource> initialStack, History history) {
            this.bidderId = bidderId;
            this.resource = resource;
            this.minOffer = minOffer;
            this.currentStack = currentStack;
            this.initialStack = initialStack;
            this.history = history;
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
         * SPECIFIC BID CALCULATING METHODS
         ************************************/

        private Integer uniformDistribution() {
            //TODO
            return 25;
        }

        private Integer avgBid() {
            //TODO
            return 25;
        }

        private Integer maxProfit() {
            switch(resource) {
                case C: //DONE
                    if( history.filterByResource(Resource.C).filterByWon(Boolean.TRUE).size()==0 ) return 28;
                    else return 4;
                case D: //DONE
                    int allD = initialStack.stream().filter(p -> p == Resource.D).collect(Collectors.toList()).size();
                    return  fib(allD)/2 +1;
                case E: //DONE
                    return 0;
                case J: //DONE
                case K:
                    HashMap<Resource, Integer> map = new HashMap<>();
                    map.put(Resource.J, history.filterByResource(Resource.J).filterByWon(Boolean.TRUE).size());
                    map.put(Resource.K, history.filterByResource(Resource.K).filterByWon(Boolean.TRUE).size());
                    int min = Math.min(map.get(Resource.J),map.get(Resource.K));
                    if(map.get(resource)==min) return 20;
                    else return 0;
                case M: //DONE
                    return 13;
                case N:
                    int currentM = history.filterByResource(Resource.M).filterByWon(Boolean.TRUE).size();
                    int currentN = history.filterByResource(Resource.N).filterByWon(Boolean.TRUE).size();
                    if(currentM>currentN) return 7;
                    else return 0;
                case W: //DONE
                case X:
                case Y:
                case Z:
                    map = new HashMap<>();
                    map.put(Resource.W, history.filterByResource(Resource.W).filterByWon(Boolean.TRUE).size());
                    map.put(Resource.X, history.filterByResource(Resource.X).filterByWon(Boolean.TRUE).size());
                    map.put(Resource.Y, history.filterByResource(Resource.Y).filterByWon(Boolean.TRUE).size());
                    map.put(Resource.Z, history.filterByResource(Resource.Z).filterByWon(Boolean.TRUE).size());
                    min =  Math.min(Math.min(map.get(Resource.W),map.get(Resource.X)),Math.min(map.get(Resource.Y),map.get(Resource.Z)));
                    if(map.get(resource)==min) return 35;
                    else return 15;
                case Q: //DONE
                    return 5;
            }

            return 0;
        }

        private Integer maxEnd() {

            switch(resource) {
                case C: //DONE
                    int allC = initialStack.stream().filter(p -> p == Resource.C).collect(Collectors.toList()).size();
                    int notSoldC = currentStack.stream().filter(p -> p == Resource.C).collect(Collectors.toList()).size();
                    if(  notSoldC > 0.5*allC  ) return 0; //first half of C - dont buy them
                    else {
                        //bis du eins bekommst!
                        if( history.filterByResource(Resource.C).filterByWon(Boolean.TRUE).size()==0 ) return 51;
                        else return 0;
                    }
                case D: //TODO
                    break;
                case E: //DONE
                    if( history.filterByResource(Resource.E).filterByWon(Boolean.TRUE).size()<2 ) return 5;
                    else return 6;
                case J: //TODO
                case K:
                    break;
                case M: //DONE
                    if( initialStack.size()/2 < currentStack.size()) { //first half
                        return 12;
                    }
                    return 15;
                case N: //DONE
                    HashMap<Resource, Integer> map = new HashMap<>();
                    map.put(Resource.M, history.filterByResource(Resource.M).filterByWon(Boolean.TRUE).size());
                    map.put(Resource.N, history.filterByResource(Resource.N).filterByWon(Boolean.TRUE).size());
                    if(map.get(Resource.N) < map.get(Resource.M)) return 15;
                    else return 0;
                case W: //DONE
                case X:
                case Y:
                case Z:
                    map = new HashMap<>();
                    map.put(Resource.W, history.filterByResource(Resource.W).filterByWon(Boolean.TRUE).size());
                    map.put(Resource.X, history.filterByResource(Resource.X).filterByWon(Boolean.TRUE).size());
                    map.put(Resource.Y, history.filterByResource(Resource.Y).filterByWon(Boolean.TRUE).size());
                    map.put(Resource.Z, history.filterByResource(Resource.Z).filterByWon(Boolean.TRUE).size());
                    int min =  Math.min(Math.min(map.get(Resource.W),map.get(Resource.X)),Math.min(map.get(Resource.Y),map.get(Resource.Z)));
                    if(map.get(resource)==min) return 30;
                    else return 21;
                case Q: //DONE
                    break;
            }

            return 0;
        }




        /************************************
         * HELPER & DEBUG METHODS
         ************************************/
        private Integer fib(int n) {
            Integer a = 0, b = 1;
            for (int i = 0; i < n; i++) {
                Integer tmp = b;
                b = a + b;
                a = tmp;
            }
            return a;
        }

        private void printStack() {
            for( Resource s : currentStack) System.out.print(s+" ");
            System.out.println();
        }

        public String toString() {
            return "Calculator(bidderId="+bidderId+";resource="+resource+";minOffer="+minOffer+")";
        }

    }

    /******************************
     * CLASS FOR STORING HISTORY
     ******************************/

    private class History {
        private List<BuyFact> history;

        public History() {
            this. history = new ArrayList<>();
        }
        public History(List<BuyFact> history) {
            this. history = history;
        }

        /**************************************************
         * ADD AND RETRIEVE TO AND FROM THE TOP OF HISTORY
         **************************************************/
        public List<BuyFact> getAll() {
            return this.history;
        }

        public void add(Resource resource, Integer myPrice, Integer soldPrice, Boolean won ) {
            this.history.add(0, new BuyFact(resource, myPrice, soldPrice, won));
        }
        public BuyFact get() {
            return this.history.get(0);
        }
        public BuyFact get(int i) {
            return this.history.get(i);
        }

        public int size() {
            return this.history.size();
        }

        /*************************************
         * FILTER HISTORY BY DIFFRENT CRITERIA
         *************************************/

        public History filterByResource(Resource resource) {
            return new History( history.stream().filter(p -> p.getResource() == resource).collect(Collectors.toList()) );
        }

        public History filterByWon(Boolean won) {
            return new History( history.stream().filter(p -> p.getWon() == won).collect(Collectors.toList()) );
        }



        private class BuyFact {

            private Resource resource;
            private Integer myPrice;
            private Integer soldPrice;
            private Boolean won;

            public BuyFact(Resource resource, Integer myPrice, Integer soldPrice, Boolean won) {
                this.resource = resource;
                this.myPrice = myPrice;
                this.won = won;
            }

            public Integer getMyPrice() {
                return myPrice;
            }

            public void setMyPrice(Integer myPrice) {
                this.myPrice = myPrice;
            }

            public Integer getSoldPrice() {
                return soldPrice;
            }

            public void setSoldPrice(Integer soldPrice) {
                this.soldPrice = soldPrice;
            }

            public Resource getResource() {
                return resource;
            }

            public void setResource(Resource resource) {
                this.resource = resource;
            }

            public Boolean getWon() {
                return won;
            }

            public void setWon(Boolean won) {
                this.won = won;
            }

        }


    }





}
