package de.dailab.jiactng.aot.auction.onto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Different types of resources
 */
public enum Resource {

	// value function: 4*C + 50
	C,

	// value function: fib(D)
	D,

	// value function: 5*E - 10
	E,

	// value function: 40 * min(J, K)
	// initially in possession of bidders, have to be traded
	J, K,

	// value function: 20 * min(M, N)
	// M appear in first half of auction, N in second half
	M, N,

	// value function: 80 * min(W, X, Y, Z)
	W, X, Y, Z,

	// value function: 4 * Q
	// few Q sold at the very end
	Q;
	
	/**
	 * Generate list of resources using some probability distribution
	 */
	public static List<Resource> generateRandomResources(int num, long seed) {
		Random random = new Random(seed);
		List<Resource> items = new ArrayList<>();
		for (int i = 0; i < num; i++) {
			double r = random.nextDouble();
			Resource item = null;
			// block 1: 40% C, D, E, Q
			if (0.00 <= r && r < 0.05) item = Resource.D; //  5% D
			if (0.05 <= r && r < 0.15) item = Resource.E; // 10% E
			if (0.15 <= r && r < 0.40) item = (i > 0.8 * num && random.nextBoolean())
					? Resource.Q : Resource.C; // 25% C or Q (at the end)

			// block 2: 20% M, N
			if (0.4 <= r && r < 0.6) item = (i < 0.5 * num)
					? Resource.M : Resource.N; // 10% N and M
			
			// block 3: 40% W, X, Y, Z
			if (0.6 <= r && r < 0.7) item = Resource.W; // 10% W
			if (0.7 <= r && r < 0.8) item = Resource.X; // 10% X
			if (0.8 <= r && r < 0.9) item = Resource.Y; // 10% Y
			if (0.9 <= r && r < 1.0) item = Resource.Z; // 10% Z
			items.add(item);
		}
		return items;
	}
	
	/**
	 * Calculate Value of the resources in this wallet
	 */
	public static long calculateValue(Map<Resource, Integer> resources) {
		int c = resources.getOrDefault(Resource.C, 0);
		int d = resources.getOrDefault(Resource.D, 0);
		int e = resources.getOrDefault(Resource.E, 0);
		int q = resources.getOrDefault(Resource.Q, 0);
		int j = resources.getOrDefault(Resource.J, 0);
		int k = resources.getOrDefault(Resource.K, 0);
		int m = resources.getOrDefault(Resource.M, 0);
		int n = resources.getOrDefault(Resource.N, 0);
		int w = resources.getOrDefault(Resource.W, 0);
		int x = resources.getOrDefault(Resource.X, 0);
		int y = resources.getOrDefault(Resource.Y, 0);
		int z = resources.getOrDefault(Resource.Z, 0);
		
		long value = 0;
		value += c > 0 ? 4 * c + 50 : 0;
		value += fib(d);
		value += e > 0 ? 5 * e - 10 : 0;
		value += 40 * Math.min(j, k);
		value += 20 * Math.min(m, n);
		value += 80 * IntStream.of(w, x, y, z).min().getAsInt();
		value += 4 * q;
		return value;
	}
	
	private static long fib(int n) {
		long a = 0, b = 1;
		for (int i = 0; i < n; i++) {
			long tmp = b;
			b = a + b;
			a = tmp;
		}
		return a;
	}
}
