package de.dailab.jiactng.aot.auction.beans;

import de.dailab.jiactng.aot.auction.onto.Resource;
import de.dailab.jiactng.aot.auction.onto.Wallet;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static de.dailab.jiactng.aot.auction.onto.Resource.*;
import static de.dailab.jiactng.aot.auction.onto.Resource.Q;
import static de.dailab.jiactng.aot.auction.onto.Resource.Z;

/******************************
 * BRAIN:
 * - stores and controlles the wallet
 * - stores and controlles lists of open and clossed calls
 * - handles offer calculation
 ******************************/

public class Brain {

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
            case "20_optimProfit": offer = optimizedProfit();
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
        assert (num_goods.size() == openitems.size());
        // add constraint to buy all available ressources at most
        for (int i = 0; i < num_goods.size(); i++) {
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
