package de.dailab.jiactng.aot.auction.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.comm.CommunicationAddressFactory;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.IGroupAddress;
import de.dailab.jiactng.aot.auction.onto.*;
import org.chocosolver.solver.*;
import org.chocosolver.solver.variables.IntVar;
import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.agentcore.ontology.IActionDescription;

import static de.dailab.jiactng.aot.auction.onto.Resource.*;


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
    private Brain brain;

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
        int offer = brain.bid();
        brain.addClosed(payload.getResource(), offer);
        send(new Bid(bidderId, payload.getCallId(), offer), auctioneer);
    }

    private void handleInformBuy(JiacMessage message) {
        InformBuy payload = (InformBuy) message.getPayload();
        brain.removeOpen();
        brain.updateClosed(payload.getPrice(), (payload.getType() == InformBuy.BuyType.WON));
        if (payload.getType() == InformBuy.BuyType.WON) {
            brain.updateWallet(-payload.getPrice());
            brain.updateWallet(payload.getResource(), 1);
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

    /******************************
     * BRAIN:
     * - stores and controlles the wallet
     * - stores and controlles lists of open and clossed calls
     * - handles offer calculation
     ******************************/

    private class Brain {

        /* static attributes - remain fixed for whole auction*/
        private String bidderId;
        private Wallet wallet;
        private OpenStack initial;

        /* variable attributes - updated at the start of each call */
        private Integer callId;
        private Resource resource;
        private Integer minOffer;

        /* variable attributes - updated at the end of each call */
        private OpenStack open;
        private ClosedStack closed;

        public Brain(String bidderId, Wallet wallet, List<Resource> stack) {
            this.bidderId = bidderId;
            this.wallet = wallet;
            this.initial = new OpenStack(stack);
            this.open = new OpenStack(stack);
            this.closed = new ClosedStack();
        }

        public void newCall(Integer callId, Resource resource, Integer minOffer) {
            this.callId = callId;
            this.resource = resource;
            this.minOffer = minOffer;
        }

        public void addClosed(Resource resource, Integer myPrice) {
            closed.add(resource, myPrice, null, null);
        }

        public void updateClosed(Integer soldPrice, Boolean won) {
            closed.get(0).setSoldPrice(soldPrice);
            closed.get(0).setWon(won);
        }

        public void removeOpen() {
            open.remove();
        }

        public Wallet getWallet() {
            return wallet;
        }

        public void updateWallet(Integer credits) {
            wallet.updateCredits(credits);
        }

        public void updateWallet(Resource resource, Integer amount) {
            wallet.add(resource, amount);
        }

        /************************************************
         * CHOOSE METHOD BASED ON AGENT NAME (bidder.xml)
         ************************************************/

        public int bid() {
            int offer;
            switch(bidderId) {
                case "20_uniform": offer = uniformDistribution();
                case "20_avgBid": offer = avgBid();
                case "20_maxProfit": offer = maxProfit();
                case "20_maxEnd": offer = maxEnd();
                case "20_OptimProfit": offer = optimizedProfit();
                default: offer = uniformDistribution();
            }
            return (offer <= wallet.getCredits()) ? offer : minOffer;
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

        private Integer maxProfit() { // MORE OR LESS DONE
            switch (resource) {
                case C: //DONE
                    if (closed.getByResource(Resource.C).getByWon(Boolean.TRUE).countAll() == 0) return 28;
                    else return 4;
                case D: //DONE
                    int allD = initial.getAll().stream().filter(p -> p == Resource.D).collect(Collectors.toList()).size();
                    return fib(allD) / 2 + 1;
                case E: //DONE
                    return 0;
                case J: //DONE
                case K:
                    HashMap<Resource, Integer> map = new HashMap<>();
                    map.put(Resource.J, closed.getByResource(Resource.J).getByWon(Boolean.TRUE).countAll());
                    map.put(Resource.K, closed.getByResource(Resource.K).getByWon(Boolean.TRUE).countAll());
                    int min = Math.min(map.get(Resource.J), map.get(Resource.K));
                    if (map.get(resource) == min) return 20;
                    else return 0;
                case M: //DONE
                    return 13;
                case N:
                    int currentM = closed.getByResource(Resource.M).getByWon(Boolean.TRUE).countAll();
                    int currentN = closed.getByResource(Resource.N).getByWon(Boolean.TRUE).countAll();
                    if (currentM > currentN) return 7;
                    else return 0;
                case W: //DONE
                case X:
                case Y:
                case Z:
                    map = new HashMap<>();
                    map.put(Resource.W, closed.getByResource(Resource.W).getByWon(Boolean.TRUE).countAll());
                    map.put(Resource.X, closed.getByResource(Resource.X).getByWon(Boolean.TRUE).countAll());
                    map.put(Resource.Y, closed.getByResource(Resource.Y).getByWon(Boolean.TRUE).countAll());
                    map.put(Resource.Z, closed.getByResource(Resource.Z).getByWon(Boolean.TRUE).countAll());
                    min = Math.min(Math.min(map.get(Resource.W), map.get(Resource.X)), Math.min(map.get(Resource.Y), map.get(Resource.Z)));
                    if (map.get(resource) == min) return 35;
                    else return 15;
                case Q: //DONE
                    return 5;
            }

            return 0;
        }

        private Integer maxEnd() { // IN PROGRESS , SEMI FUNCTIONAL BUT NOT VERY EFFECTIVE

            switch (resource) {
                case C: //DONE
                    int allC = initial.getAll().stream().filter(p -> p == Resource.C).collect(Collectors.toList()).size();
                    int notSoldC = open.getAll().stream().filter(p -> p == Resource.C).collect(Collectors.toList()).size();
                    if (notSoldC > 0.5 * allC) return 0; //first half of C - dont buy them
                    else {
                        //bis du eins bekommst!
                        if (closed.getByResource(Resource.C).getByWon(Boolean.TRUE).countAll() == 0) return 51;
                        else return 0;
                    }
                case D: //TODO
                    break;
                case E: //DONE
                    if (closed.getByResource(Resource.E).getByWon(Boolean.TRUE).countAll() < 2) return 5;
                    else return 6;
                case J: //TODO
                case K:
                    break;
                case M: //DONE
                    if (initial.countAll() / 2 < open.countAll()) { //first half
                        return 12;
                    }
                    return 15;
                case N: //DONE
                    HashMap<Resource, Integer> map = new HashMap<>();
                    map.put(Resource.M, closed.getByResource(Resource.M).getByWon(Boolean.TRUE).countAll());
                    map.put(Resource.N, closed.getByResource(Resource.N).getByWon(Boolean.TRUE).countAll());
                    if (map.get(Resource.N) < map.get(Resource.M)) return 15;
                    else return 0;
                case W: //DONE
                case X:
                case Y:
                case Z:
                    map = new HashMap<>();
                    map.put(Resource.W, closed.getByResource(Resource.W).getByWon(Boolean.TRUE).countAll());
                    map.put(Resource.X, closed.getByResource(Resource.X).getByWon(Boolean.TRUE).countAll());
                    map.put(Resource.Y, closed.getByResource(Resource.Y).getByWon(Boolean.TRUE).countAll());
                    map.put(Resource.Z, closed.getByResource(Resource.Z).getByWon(Boolean.TRUE).countAll());
                    int min = Math.min(Math.min(map.get(Resource.W), map.get(Resource.X)), Math.min(map.get(Resource.Y), map.get(Resource.Z)));
                    if (map.get(resource) == min) return 30;
                    else return 21;
                case Q: //TODO
                    break;
            }

            return 0;
        }

        public int[] findBestPrices(int money, ArrayList<Integer> openitems) {
            Model model = new Model("Find good prices");
            // create array of goods
            ArrayList<IntVar> num_goods = new ArrayList<>(Arrays.asList(model.intVarArray("num", 7, 0, 1000, true)));

            // create array of squared goods
            ArrayList<IntVar> num_goods_squared = new ArrayList<>(Arrays.asList(model.intVarArray("num_squared", 7, 0, 1000000, true)));
            for (int i = 0; i < num_goods.size(); i++) {
                model.times(num_goods.get(i), num_goods.get(i), num_goods_squared.get(i)).post();
            }
            num_goods_squared.add(num_goods.get(0));
            num_goods_squared.add(num_goods.get(2));
            System.out.println(num_goods_squared.size());

            //add constant to good array
            num_goods.add(model.intVar("const", 40));

            //create profit variable
            IntVar profit = model.intVar("Profit", 0, 1000000);
            IntVar cost = model.intVar("cost", 0, 1000);

            IntVar[] num_goods_array = new IntVar[num_goods.size()];
            IntVar[] num_goods_array_squared = new IntVar[num_goods_squared.size()];

            // add constraint to buy all available ressources at most
            for (int i = 0; i < openitems.size(); i++) {
                model.arithm(num_goods.get(i), "<=", openitems.get(i)).post();
            }

            // add constraint to maximize profi and constrain it to spend money at most
            model.scalar(num_goods.toArray(num_goods_array), new int[]{4, 100, 5, 40, 20, 80, 4, 1}, "=", profit).post();
            model.scalar(num_goods_squared.toArray(num_goods_array_squared), new int[]{4, 100, 5, 80, 40, 320, 4, 50, -10}, "=", cost).post();
            model.arithm(cost, "<=", money).post();
            //model.setObjective(Model.MAXIMIZE, profit);

            Solver solver = model.getSolver();
            Solution best = solver.findOptimalSolution(profit, true);

            return num_goods.stream().mapToInt(var -> best.getIntVal(var)).toArray();
        }

        private Integer optimizedProfit() { // IN PROGRESS , SEMI FUNCTIONAL BUT NOT VERY EFFECTIVE
            ArrayList<Integer> openStackCount = new ArrayList<>();
            Resource[] allResources = new Resource[]{C, D, E, J, K, M, N, W, X, Y, Z, Q};
            for (int j = 0; j < allResources.length; j++) {
                int i = 0;
                switch (allResources[j]) {
                    case C: //DONE
                        i += open.countByResource(C);
                        break;
                    case D:
                        i += open.countByResource(D);
                        break;
                    case E: //DONE
                        i += open.countByResource(E);
                        break;
                    case J:
                        i += open.countByResource(J);
                    case K:
                        i += open.countByResource(K);
                        break;
                    case M:
                        i += open.countByResource(M);
                    case N: //DONE
                        i += open.countByResource(N);
                        break;
                    case W:
                        i += open.countByResource(W);
                    case X:
                        i += open.countByResource(X);
                    case Y:
                        i += open.countByResource(Y);
                    case Z:
                        i += open.countByResource(Z);
                        break;
                    case Q:
                        i += open.countByResource(Q);
                        break;
                }
                openStackCount.add(i);
            }
            int[] best = findBestPrices(wallet.getCredits(),openStackCount);
            switch (resource) {
                case C: //DONE
                    return 4*best[0]+ 50;
                case D:
                    return fib(best[1]);
                case E: //DONE
                    return Math.max(5*best[2]-10, 0);
                case J:
                case K:
                    return 40*best[3];
                case M:
                case N:
                    return 20*best[4];
                case W:
                case X:
                case Y:
                case Z:
                    return 80*best[5];
                case Q: //TODO
                    return 4*best[6];
                default:
                    return 0;
            }
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


    }

    /**************************************************************
     * OPENSTACK:
     * - stores resources that are yet to be bidded on in a list
     * - allows getting particular Elements from the list,
     *   removing from the top (sold Resources)
     * - also allows basic filtering functions
     **************************************************************/
    private class OpenStack {
        private List<Resource> items;

        public OpenStack(List<Resource> items) {
            this.items = items;
        }

        public void remove() {
           if(!items.isEmpty()) items.remove(0);
        }

        public Resource get() {
            return items.get(0);
        }

        public Resource get(int i) {
            return items.get(i);
        }

        public List<Resource> getAll() {
            return this.items;
        }

        public int countAll() {
            return this.items.size();
        }

        public List<Resource> getByResource(Resource resource) {
            return items.stream().filter(p -> p == resource).collect(Collectors.toList());
        }

        public int countByResource(Resource resource) {
            return items.stream().filter(p -> p == resource).collect(Collectors.toList()).size();
        }

        public String toString() {
            return "OpenStack(items=" + items + ")";
        }
    }

    /*******************************************************
     * CLOSEDSTACK:
     * - stores items that are already sold in a list
     *   as ClosedItem objects
     * - allows getting particular Elements from the list,
     *   adding to the top (newly sold Resources)
     * - also allows basic filtering functions
     *******************************************************/

    private class ClosedStack {
        private List<ClosedItem> items;

        public ClosedStack() {
            this.items = new ArrayList<>();
        }

        public ClosedStack(List<ClosedItem> items) {
            this.items = items;
        }

        public void add(Resource resource, Integer myPrice, Integer soldPrice, Boolean won) {
            items.add(0, new ClosedItem(resource, myPrice, soldPrice, won));
        }

        public ClosedItem get() {
            return items.get(0);
        }

        public ClosedItem get(int i) {
            return items.get(i);
        }

        public List<ClosedItem> getAll() {
            return this.items;
        }

        public int countAll() {
            return this.items.size();
        }

        public ClosedStack getByResource(Resource resource) {
            return new ClosedStack(items.stream().filter(p -> p.getResource() == resource).collect(Collectors.toList()));
        }

        public ClosedStack getByWon(Boolean won) {
            return new ClosedStack(items.stream().filter(p -> p.getWon() == won).collect(Collectors.toList()));
        }

        public String toString() {
            return items.stream().map(Object::toString).collect(Collectors.joining(",\n"));
        }

        /*************************************************************
         * CLOSEDITEM:
         * - single Element of "history" of calls
         * - stores Resource, Price that our agent bidded on him
         *   price, for which was it sold and if we won this resource
         * - also provides standard getters/setters
         *************************************************************/

        private class ClosedItem {

            private Resource resource;
            private Integer myPrice;
            private Integer soldPrice;
            private Boolean won;

            public ClosedItem(Resource resource, Integer myPrice, Integer soldPrice, Boolean won) {
                this.resource = resource;
                this.myPrice = myPrice;
                this.soldPrice = soldPrice;
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

            public String toString() {
                return "ClosedItem(resource=" + resource + ";myPrice=" + myPrice + ";soldPrice=" + soldPrice + ";won=" + won + ")";
            }
        }
    }
}













