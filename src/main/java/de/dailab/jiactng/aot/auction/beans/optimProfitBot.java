package de.dailab.jiactng.aot.auction.beans;

import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.aot.auction.onto.InitializeBidder;
import de.dailab.jiactng.aot.auction.onto.Resource;
import de.dailab.jiactng.aot.auction.onto.Wallet;

import java.util.List;

public class optimProfitBot extends BidderBean {

    private void handleInitializeBidder(JiacMessage message) {
        Wallet wallet = ((InitializeBidder) message.getPayload()).getWallet();
        List<Resource> stack = ((InitializeBidder) message.getPayload()).getItems();
        this.brain = new Brain("20_optimProfit", wallet, stack);
    }
}
