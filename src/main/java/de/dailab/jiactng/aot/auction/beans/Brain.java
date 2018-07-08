package de.dailab.jiactng.aot.auction.beans;

import de.dailab.jiactng.aot.auction.onto.Resource;
import de.dailab.jiactng.aot.auction.onto.Wallet;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.dailab.jiactng.aot.auction.onto.Resource.*;

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
    int[] sellCalls;

    public Brain(String bidderId, Wallet wallet, List<Resource> stack) {
        this.bidderId = bidderId;
        this.wallet = wallet;
        this.initial = new OpenStack(stack);
        this.open = new OpenStack(stack);
        this.closed = new ClosedStack();
        this.sellCalls = new int[12];
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

    public int[] findBestPrices(int money, int[] openitems, int[] curRes) {
        Model model = new Model("Find good prices");
        ArrayList<IntVar> num_goods = new ArrayList<>(Arrays.asList(model.intVarArray("num_goods", 12, -1000, 1000, true)));
        ArrayList<IntVar> use_goods = new ArrayList<>(Arrays.asList(model.intVarArray("use_goods", 7, -1000, 1000000, true)));

        // good 1: u1 = 4*n1 + 50
        IntVar temp1 = model.intVar("temp1",0, 1000);
        model.times(num_goods.get(0), 4, temp1).post();
        model.arithm(use_goods.get(0), "=", temp1, "+", 50).post();

        // good 2: u2 = fib(n2)
        model.arithm(use_goods.get(1), "=", num_goods.get(1), "*", (fib(curRes[1]+openitems[1])-fib(curRes[1]))/(openitems[1]+1)).post();

        // good 3:
        IntVar temp2 = model.intVar("temp2",0, 1000);
        model.times(num_goods.get(2), 5, temp2).post();
        model.arithm(use_goods.get(2), "=", temp2, "-", 10).post();

        // good 4, 5:
        IntVar temp3 = model.intVar("temp3",0, 1000);
        model.min(temp3, num_goods.get(3), num_goods.get(4)).post();
        model.arithm(use_goods.get(3), "=", temp3, "*", 40).post();

        // good 6, 7:
        IntVar temp4 = model.intVar("temp4",0, 1000);
        model.min(temp4, num_goods.get(5), num_goods.get(6)).post();
        model.arithm(use_goods.get(4), "=", temp4, "*", 20).post();

        // good 8,9,10,11
        IntVar temp5 = model.intVar("temp5",0, 1000);
        model.min(temp5, new IntVar[]{num_goods.get(7), num_goods.get(8), num_goods.get(9), num_goods.get(10)}).post();
        model.arithm(use_goods.get(5), "=", temp5, "*", 80).post();

        // good 12
        model.arithm(use_goods.get(6), "=", num_goods.get(11), "*", 4).post();


        //create profit variable
        IntVar profit = model.intVar("Profit", 0, 1000000);
        IntVar cost = model.intVar("cost", 0, 1000000);

        IntVar[] use_goods_array = new IntVar[use_goods.size()];
        IntVar[] num_goods_array = new IntVar[use_goods.size()];

        // add constraint to buy all available resources at most
        for (int i = 0; i < num_goods.size(); i++) {
            model.arithm(num_goods.get(i), "<=", openitems[i] + curRes[i]).post();
        }

    //    // add constraint to buy all available resources at most
    //    for (int i = 0; i < num_goods.size(); i++) {
    //        model.arithm(num_goods.get(i), ">=", curRes[i]).post();
    //    }

        // add constraint to maximize profi and constrain it to spend money at most
        model.scalar(use_goods.toArray(use_goods_array), new int[]{1, 1, 1, 1, 1, 1, 1}, "=", profit).post();
        model.scalar(num_goods.toArray(num_goods_array), new int[]{(curRes[0]==0)?50:4, fib(curRes[1]+openitems[1])-fib(curRes[1]), 5, 20, 20, 10, 10, 20, 20, 20, 20, 4}, "=", cost).post();
        model.arithm(cost, "<=", money).post();

        Solver solver = model.getSolver();
        model.setObjective(Model.MAXIMIZE, profit);
        try {
            solver.propagate();
        } catch (ContradictionException e) {
            e.printStackTrace();
        }
        Solution best = solver.findOptimalSolution(profit, true);

        int[] optimNum = num_goods.stream().mapToInt(var -> best.getIntVal(var)).toArray();
        return IntStream.range(0,12).map(i -> optimNum[i]-curRes[i]).toArray();
    }

    private Integer optimizedProfit() { // IN PROGRESS , SEMI FUNCTIONAL BUT NOT VERY EFFECTIVE
        ArrayList<Integer> openStackCount = new ArrayList<>();
        int[] resCount = Arrays.stream(new Resource[]{C, D, E, J, K, M, N, W, X, Y, Z, Q}).mapToInt(res -> open.countByResource(res)).toArray();
        int[] currResCount = Arrays.stream(new Resource[]{C, D, E, J, K, M, N, W, X, Y, Z, Q}).mapToInt(res -> wallet.get(res)).toArray();
        int[] best = findBestPrices(wallet.getCredits(),resCount, currResCount);
        switch (resource) {
            case C: //DONE
                return (best[0] > 0)?4:54;
            case D:
                return (best[1] > 0)?fib(wallet.get(D)+1)-fib(wallet.get(D)):0;
            case E: //DONE
                return (best[2]> 0)?5:0;
            case J:
                sellCalls[3] = (currResCount[3] > 0 && best[3] < 0)?20:0;
                return (best[3]> 0)?20:0;
            case K:
                sellCalls[4] = (currResCount[4] > 0 && best[4] < 0)?20:0;
                return (best[4]> 0)?20:0;
            case M:
                sellCalls[5] = (currResCount[5] > 0 && best[5] < 0)?10:0;
                return (best[5]> 0)?10:0;
            case N:
                sellCalls[6] = (currResCount[6] > 0 && best[6] < 0)?10:0;
                return (best[6]> 0)?10:0;
            case W:
                sellCalls[7] = (currResCount[7] > 0 && best[7] < 0)?10:0;
                return (best[7]> 0)?20:0;
            case X:
                sellCalls[8] = (currResCount[8] > 0 && best[8] < 0)?10:0;
                return (best[8]> 0)?20:0;
            case Y:
                sellCalls[9] = (currResCount[9] > 0 && best[9] < 0)?10:0;
                return (best[9]> 0)?20:0;
            case Z:
                sellCalls[10] = (currResCount[10] > 0 && best[10] < 0)?10:0;
                return (best[10]> 0)?20:0;
            case Q:
                return (best[11]> 0)?4:0;
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
