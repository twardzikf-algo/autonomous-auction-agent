package de.dailab.jiactng.aot.auction.beans;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import de.dailab.jiactng.agentcore.AbstractAgentBean;

/**
 * The sole purpose of this bean is to trigger a new auction in regular intervals
 * and to log the results of past auctions.
 * 
 * This assumes that the auctioneer itself will start the first auction; after that,
 * the auction runner's execute() method will regularly check the results of the
 * previous run and trigger a new auction via the corresponding actions.
 */
public class AuctionRunnerBean extends AbstractAgentBean {

	private AtomicInteger counter = new AtomicInteger();
	
	private String secretToken;
	
	private Integer numberOfAuctions;
	
	
	@Override
	public void execute() {
		try {
			Serializable[] results = invokeAction(AuctioneerBean.ACTION_GET_PHASE, -1);
			if ("IDLE".equals(results[0])) {
				
				// get results from past auction
				results = invokeAction(AuctioneerBean.ACTION_GET_LAST_RESULTS, -1, secretToken);
				String res = (String) results[0];
				
				// properly log the results
				log.info(String.format("RESULTS FOR AUCTION %d:\n%s", counter.getAndIncrement(), res));
				
				if (numberOfAuctions == null || counter.get() < numberOfAuctions) {
					// trigger next auction
					invokeAction(AuctioneerBean.ACTION_START_AUCTION, -1, secretToken);
				} else {
					log.info("ALL AUCTIONS ARE OVER");
					setExecutionInterval(-1);
				}
			} else {
				log.info("Auction still running...");
			}
				
		} catch (Exception e) {
			log.error(e);
		}
	}
	
	
	public void setSecretToken(String secretToken) {
		this.secretToken = secretToken;
	}
	
	public void setNumberOfAuctions(Integer numberOfAuctions) {
		this.numberOfAuctions = numberOfAuctions;
	}
}
