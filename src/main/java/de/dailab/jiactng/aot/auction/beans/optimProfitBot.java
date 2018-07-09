package de.dailab.jiactng.aot.auction.beans;

import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.aot.auction.onto.InformBuy;
import de.dailab.jiactng.aot.auction.onto.InitializeBidder;
import de.dailab.jiactng.aot.auction.onto.Resource;
import de.dailab.jiactng.aot.auction.onto.Wallet;

import java.util.List;

import static de.dailab.jiactng.aot.auction.onto.Resource.*;
import static de.dailab.jiactng.aot.auction.onto.Resource.Q;
import static de.dailab.jiactng.aot.auction.onto.Resource.Z;

public class optimProfitBot extends BidderBean {

    private void handleInitializeBidder(JiacMessage message) {
        Wallet wallet = ((InitializeBidder) message.getPayload()).getWallet();
        List<Resource> stack = ((InitializeBidder) message.getPayload()).getItems();
        this.brain = new Brain("20_optimProfit", wallet, stack);
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
}
