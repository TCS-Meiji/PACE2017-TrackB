package fillin.main;

import java.io.File;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import tw.common.BlockSieve;
import tw.common.Graph;
import tw.common.LabeledGraph;
import tw.common.Pair;
import tw.common.TreeDecomposition;
import tw.common.XBitSet;

public class Decomposer {

//	static final boolean VERBOSE = true;
	private static final boolean VERBOSE = false;
	private static boolean DEBUG = false;
	//	static boolean DEBUG = true;

	LabeledGraph g;
	Bounds bounds;
	int lowerbound;

	BlockSieve tBlockSieve;

	Queue< MBlock > readyQueue;

	Map<XBitSet, MBlock> mBlockMap;

	Map<XBitSet, TBlock> tBlockMap;

	Map<XBitSet, Block> blockMap;

	Map<XBitSet, PMC> pmcMap;

	TreeSet<PMC> pmcQueue;

	int targetCost;

	PMC solution;

	int[][] forbiddenEdges;

	File logFile;

	int tentativeUB;

	boolean noUpperbound;

	public void setNoUpperbound(boolean noUpperbound) {
		this.noUpperbound = noUpperbound;
	}

	public Decomposer(LabeledGraph g) {
		this.g = g;
	}

	public int getOpt()
	{
		return targetCost;
	} 

	public TreeDecomposition decompose(int upperbound) {
		// if upperbound is given then we have only one iteration
		// with the given upperbound
		// otherwise (shown by upperbound < 0), we start from
		// lowerbonund + increment and iterate until a solution
		// is found

		// we need the lowerbound anyway, to be used in the relevant() method
		// of TBlock
		bounds = new Bounds(g);
		long time = System.currentTimeMillis();
		lowerbound = bounds.lowerbound();

		if (VERBOSE) {
			System.out.println("n = " + g.n + 
					", lb = " + lowerbound + ", ub = " + upperbound);
		}

		int increment = 1;
		int start = upperbound;
		int end = upperbound;
		if (upperbound < 0) {
			increment = 1 + lowerbound / 2; 
			start = lowerbound + increment;
			end = Integer.MAX_VALUE;
		}

		blockMap = new HashMap<>();

		for (tentativeUB = start; tentativeUB <= end;
				tentativeUB += increment){

			mBlockMap = new HashMap<>();
			pmcMap = new HashMap<>();
			pmcQueue = new TreeSet<>();

			tBlockMap = new HashMap<>();
			tBlockSieve = new BlockSieve(g.n);

			readyQueue = new LinkedList<>();
			for (int v = 0; v < g.n; v++) {
				XBitSet cnb = g.closedNeighborSet( v );

				if (isPMC(cnb) == false || 
						pmcMap.get(cnb) != null) {
					continue;
				}

				PMC pmc = new PMC(cnb);
				pmcMap.put(cnb, pmc);
				pmcQueue.add(pmc);
			}

			readyQueue.addAll( mBlockMap.values() );
			
			for (targetCost = 0 ; targetCost <= tentativeUB; targetCost++) {
				// (1) all M-blocks with optimal cost < targetCost
				// that use only bags with fillin <= tentativeUB
				// have been generated and have the optimal cost computed
				// (2) all T-blocks obtainable as a combination of M-blocks 
				// as in (1) that have a separator with fillin <= tentativeUB
				// have been generated
				// (3) all PMCs whose all inbound M-blocks as in (1)
				// have been generated

				log("Current target cost: " + targetCost);

				while (true) {
					//	        log("outer while");
					log("processing pmc queue");
					if (DEBUG) {
						for (PMC pmc: pmcQueue) {
							System.out.print(pmc.lowerBound + " ");
						}
						System.out.println();
					}
					ArrayList<PMC> toConsider = new ArrayList<>();
					while (!pmcQueue.isEmpty()) {
						PMC pmc = pmcQueue.first();
						if (pmc.lowerBound <= targetCost) {
							toConsider.add(pmc);
							pmcQueue.remove(pmc);
						} else {
							break;
						}
					}

					toConsider.forEach(pmc -> pmc.process());

					if (readyQueue.isEmpty()) {
						break;
					}
					
					log("processing ready queue");
					while (readyQueue.isEmpty() == false) {
						//	          log(" inner while");
						MBlock ready = readyQueue.poll();
						ready.process();
					}
				}

				if (solution != null) {
					TreeDecomposition td = constructTD();
					return td;
				}

				if (DEBUG) {
					System.out.println( "== MBlocks of cost at most " + targetCost );
					mBlockMap.values().forEach(m -> System.out.println( m ));
				}
			}
		}
		return null;
	}

	private final Block getBlock(XBitSet component) {
		Block block = blockMap.get(component);
		if (block == null) {
			block = new Block(component);
			blockMap.put(component, block);
		}
		return block;
	}

	TreeDecomposition constructTD() {
		TreeDecomposition td = new TreeDecomposition(0, g.n - 1, g);
		solution.recurseTD(td);
		return td;
	}

	int[] toBag(XBitSet set) {
		int result[] = new int[set.cardinality()];
		int i = 0;
		for (int v = set.nextSetBit(0); v >= 0; v = set.nextSetBit(v + 1)) {
			result[i++] = v;
		}
		return result;
	}

	ArrayList<Block> getBlocks(XBitSet separator) {
		//		int sepSize = separator.cardinality();
		ArrayList<Block> result = new ArrayList<Block>();
		XBitSet rest = g.all.subtract(separator);
		for (int v = rest.nextSetBit(0); v >= 0; 
				v = rest.nextSetBit(v + 1)) {
			XBitSet c = g.neighborSet[v].subtract(separator);
			XBitSet toBeScanned = (XBitSet) c.clone();
			c.set(v);
			while (!toBeScanned.isEmpty()) {
				XBitSet save = (XBitSet) c.clone();
				for (int w = toBeScanned.nextSetBit(0); w >= 0;
						w = toBeScanned.nextSetBit(w + 1)) {
					c.or(g.neighborSet[w]);
				}
				c.andNot(separator);
				toBeScanned = c.subtract(save);
			}

			Block block = getBlock(c); 
			result.add(block);
			rest.andNot(c);
		}
		return result;
	}

	private boolean isPMC(XBitSet separator) {
		ArrayList<Block> blockList = getBlocks(separator);
		for (int v = separator.nextSetBit(0); v >= 0; v = separator.nextSetBit(v + 1)) {
			// rest is the subset of vertices of separator each of which
			// is not adjacent to v and whose index is greater than that of v.
			// For each w in rest, there is a separator S with {v, w} \subseteq S in blockList.
			XBitSet rest = separator.subtract( g.neighborSet[v] ); 
			for (int w = rest.nextSetBit(v + 1); w >= 0; w = rest.nextSetBit(w + 1)) {
				boolean covered = false;
				for (Block b: blockList) {
					if (b.separator.get(v) && b.separator.get(w)) {
						covered = true;
						break;
					}
				}
				if (!covered) {
					return false;
				}
			}
		}
		return true;
	}

	class Block implements Comparable<Block> {
		XBitSet component;
		XBitSet separator;
		XBitSet outbound;

		Block(XBitSet component) {
			this.component = component;
			this.separator = g.neighborSet(component);

			XBitSet rest = g.all.subtract(component);
			rest.andNot(separator);

			int minCompo = component.nextSetBit(0);

			// the scanning order ensures that the first full component
			// encountered is the outbound one
			for (int v = rest.nextSetBit(0); v >= 0; v = rest.nextSetBit(v + 1)) {
				XBitSet c = (XBitSet) g.neighborSet[v].clone();
				XBitSet toBeScanned = c.subtract(separator);
				c.set(v);
				while (!toBeScanned.isEmpty()) {
					XBitSet save = (XBitSet) c.clone();
					for (int w = toBeScanned.nextSetBit(0); w >= 0; 
							w = toBeScanned.nextSetBit(w + 1)) {
						c.or(g.neighborSet[w]);
					}
					toBeScanned = c.subtract(save).subtract(separator);
				}
				if (separator.isSubset(c)) {
					// full block other than "component" found
					if (v < minCompo) {
						outbound = c.subtract(separator);
					}
					else {
						// v > minCompo
						outbound = component;
					}
					return;
				}
				rest.andNot(c);
			}
		}

		boolean isOutbound() {
			return outbound == component;
		}

		boolean ofMinimalSeparator() {
			return outbound != null;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (outbound == component) {
				sb.append("o");
			} 
			else {
				if (mBlockMap.get(component) != null) {
					sb.append("f");
				} else {
					sb.append("i");
				}
			}
			sb.append(component + "(" + separator + ")");
			return sb.toString();
		}

		@Override
		public int compareTo(Block b) {
			return component.nextSetBit(0) - b.component.nextSetBit(0);
		}
	}

	class MBlock {
		XBitSet inbound;
		XBitSet separator;
		XBitSet outbound;
		PMC endorser;
		int cost;

		MBlock(XBitSet inbound, XBitSet separator, XBitSet outbound, PMC endorser, int cost)
		{
			if (inbound.equals(outbound)) {
				throw new RuntimeException(inbound + " equals " + outbound);
			}
			this.inbound = inbound;
			this.separator = separator;
			this.outbound = outbound;
			this.endorser = endorser;
			this.cost = cost;

			if (DEBUG) {
				System.out.println("MBlock constructor" + this);
			}
		}

		void process() {
			if (DEBUG) {
				System.out.print("processing " + this);
			}

			makeSimpleTBlock();

			tBlockSieve.collectSuperblocks(inbound, separator, new ArrayList<>()).forEach(tBlock -> tBlock.plugin( this ));

			//			LinkedList< TBlock > tBlocks = new LinkedList<>();
			//			tBlockTrie.collectSuperSet( inbound , tBlocks );
			//			tBlocks.forEach(tBlock -> tBlock.plugin(this));
		}

		void makeSimpleTBlock() {

			if (DEBUG) {
				System.out.print("makeSimple: " + this);
			}

			TBlock tBlock = tBlockMap.get(separator); 
			if (tBlock == null) {
				tBlock = new TBlock(separator, outbound);
				tBlockMap.put(separator, tBlock);
				if (tBlock.relevant()) {
					tBlockSieve.put(outbound, tBlock);
					tBlock.crown();
				} else {
					if (DEBUG) {
						System.out.println("discarding " + tBlock + 
								", fill count = " + g.countFill(tBlock.separator) + 
								", tentativeUB = " + tentativeUB);
					}
				}
			}
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("MBlock:" + separator + "\n");
			sb.append("  cost:" + cost + "\n");
			sb.append("  in  :" + inbound + "\n");
			sb.append("  out :" + outbound + "\n");
			return sb.toString();
		}
	}

	public class TBlock {
		XBitSet separator;
		XBitSet openComponent;
		Block blocks[];

		TBlock(XBitSet separator, XBitSet openComponent) {
			this.separator = separator;
			this.openComponent = openComponent;
			ArrayList<Block> blockList = getBlocks(separator.unionWith(openComponent));
			blocks = new Block[blockList.size()];
			blockList.toArray(blocks);
		}

		boolean relevant() {
			int cost = g.countFill(separator);
			if (cost > tentativeUB) {
				return false;
			}
			for (Block block: blocks) {
				if (!block.isOutbound()) {
					MBlock mBlock = mBlockMap.get(block.component);
					if (mBlock == null) {
						// cost of the inbound block is at least the
						// current target (otherwise it would already be optimal)
						cost += targetCost;
					} else {
						cost += mBlock.cost;
					}
				}
			}
			if (cost > tentativeUB) {
				return false;
			}
			if (cost + lowerbound <= tentativeUB) {
				return true;
			}

			int lbRest = bounds.lowerbound(openComponent, separator);
			//      System.out.println("cost = " + cost + ", lb = " + lbRest +
			//          ", " + openComponent + ", " + separator);
			return cost + lbRest <= tentativeUB;

		}

		void plugin(MBlock mBlock) {

			if (DEBUG) {
				System.out.println("plugin " + mBlock);
				System.out.println("  to " + this);
			}

			XBitSet newsep = separator.unionWith(mBlock.separator);

			if (g.countFill(newsep) > tentativeUB) {
				return;
			}

			XBitSet extendedSep = g.all.subtract(openComponent).unionWith(mBlock.separator);

			ArrayList<Block> blockList = getBlocks(extendedSep);

			Block fullBlock = null;
			int nSep = newsep.cardinality();

			for (Block block: blockList) {
				if (block.separator.cardinality() == nSep) {
					if (fullBlock != null) {
						// minimal separator: treated elsewhere
						return;
					}
					fullBlock = block;
				} else if (!block.ofMinimalSeparator()){
					return;
				}
			}

			if (fullBlock != null) {
				//      if (g.isVital( newsep, targetCost ) == false) {
				//        return;
				//      }

				TBlock tBlock = tBlockMap.get(newsep); 
				if (tBlock == null) {
					tBlock = new TBlock(newsep, fullBlock.component);
					tBlockMap.put(newsep, tBlock);
					if (tBlock.relevant()) {
						tBlockSieve.put(fullBlock.component, tBlock);
						tBlock.crown();
					}
				}
			} else {
				if (isPMC( newsep ) == false) {
					return;
				}

				PMC pmc = new PMC(newsep);

				pmcMap.put(newsep, pmc);

				pmc.process();
			}
		}

		void crown() {
			for (int v = separator.nextSetBit(0); v >= 0;
					v = separator.nextSetBit(v + 1)) {
				if (DEBUG) {
					System.out.println("try crowing by " + v);
				}

				XBitSet addition = g.neighborSet[v].intersectWith(openComponent);

				XBitSet rest = openComponent.subtract(addition);

				if (rest.isEmpty()) {
					// rest is empty: should be dealt with the base case
					continue;
				}

				XBitSet newsep = separator.unionWith(addition);

				if (g.countFill(newsep) > tentativeUB) {
					continue;
				}

				if (DEBUG) {
					System.out.println("crowing by " + v + ":" + this);
				}

				if (isPMC( newsep ) == false) {
					continue;
				}

				PMC pmc = new PMC(newsep);
				pmc.process();
			} 
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("TBlock:\n");
			for (Block b: blocks) {
				sb.append(b + "\n");
			}
			sb.append("  sep :" + separator + "\n");
			sb.append("  open:" + openComponent + "\n");
			return sb.toString();
		}
	}

	class PMC implements Comparable<PMC> {
		XBitSet separator;
		Block inbounds[];
		Block outbound;
		boolean ready;
		int lowerBound;

		PMC(XBitSet separator) {
			this.separator = separator;
			if (separator.isEmpty()) {
				return;
			}
			ArrayList<Block> blockList = getBlocks(separator);

			for (Block block: blockList) {
				if (block.isOutbound() &&
						(outbound == null || 
						outbound.separator.isSubset(block.separator))){
					outbound = block;
				}
			}
			if (outbound == null) {
				inbounds = blockList.toArray(
						new Block[blockList.size()]);  
			}
			else {
				inbounds = new Block[blockList.size()];
				int k = 0;
				for (Block block: blockList) {
					if (!block.separator.isSubset(outbound.separator)) {
						inbounds[k++] = block;
					}
				}
				if (k < inbounds.length) {
					inbounds = Arrays.copyOf(inbounds, k);
				}
			}
		}

		void evaluate() {
			//	    System.out.println("evaluating endorser: " + separator);
			if (!ready) {
				lowerBound = g.countFill(separator);
				if (outbound != null) 
					lowerBound -= g.countFill(outbound.separator);

				ready = true;
				for (Block block: inbounds) {
					MBlock mBlock = mBlockMap.get(block.component);
					if (mBlock == null || mBlock.cost > targetCost) {
						//	          System.out.println("inbound MBlock: " + mBlock);
						ready = false;
						lowerBound += targetCost;
					}
					else {
						lowerBound += mBlock.cost;
					}
				}
			}
			if (DEBUG) {
				System.out.println("pmc evaluated " + this);
			}
		}

		void process() {
			evaluate();

			if (DEBUG) {
				System.out.print("processing " + this);
			}
			if (ready && lowerBound <= targetCost) {
				if (DEBUG) {
					System.out.print("endorsing " + this);
				}
				endorse();
			} else {
				if (DEBUG) {
					System.out.println("back to pmc queue: " + this);
				}
				pmcQueue.add(this);
			}
		}

		void endorse() {
			if (DEBUG) {
				System.out.print("endorsing " + this);
			}

			if (DEBUG) {
				System.out.println("ontbound= " + outbound);
			}

			int cost = countFill();

			assert cost == lowerBound: "cost " + cost + 
					" must be equal to the lower bound " + lowerBound +  
					" when endorsing: " + this;

			if (cost < targetCost) {
				// this pmc must have been generated in some other way and
				// have been processed
				return;
			}
			if (outbound == null) {
				solution = this;
				return;
			} else {
				XBitSet target = getTarget();
				endorse( target, cost );
			}
		}

		void endorse(XBitSet target, int cost) {
			if (DEBUG) {
				System.out.println("endorsed = " + target);
			}

			MBlock mBlock = mBlockMap.get( target );
			if (mBlock != null) {
				assert mBlock.cost <= cost: "the cost " + mBlock.cost + 
						" of the mBlock fist set must be optimal but the cost " + 
						cost + " now found is smaller: " + this;
				if (DEBUG) {
					System.out.println(" already known optimal, cost = " +  
							mBlock.cost);
				}
				return;
			}
			if (mBlock == null) {
				mBlock = new MBlock( target, outbound.separator, outbound.component, this, cost);
				mBlockMap.put(target, mBlock);
				mBlock.cost = cost;
				mBlock.endorser = this;
				readyQueue.add( mBlock );
			} 
		}

		XBitSet getTarget() {
			if (outbound == null) {
				return null;
			}
			XBitSet combined = separator.subtract(outbound.separator);
			for (Block b: inbounds) {
				combined.or(b.component);
			}
			return combined;
		}

		int countFill()
		{
			int fill = 0;
			for (Block block: inbounds) {
				MBlock mBlock = mBlockMap.get( block.component );
				if (mBlock != null) {
					fill += mBlock.cost;
				}
			}

			fill += g.countFill( separator );
			if (outbound != null) {
				fill -= g.countFill( g.neighborSet( outbound.component ) );
			}
			return fill;
		}

		int recurseTD(TreeDecomposition td) {
			if (DEBUG) {
				System.out.print("recurseTD:" + this);
			}
			int j = td.addBag(toBag(separator));

			for (Block block: inbounds) {
				if (DEBUG) {
					System.out.println("compo = " + block.component);
				}
				PMC subEndorser = mBlockMap.get(block.component).endorser;
				if (subEndorser == null) {
					System.out.println("subendorser is null, compo = " + 
							block.component);
					continue;
				}
				int k = subEndorser.recurseTD(td);
				td.addEdge(j, k);
			}
			return j;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("Endorser:\n");
			sb.append("  cost   = " + countFill() + "\n");
			sb.append("  costLB = " + lowerBound + "\n");
			sb.append("  ready  = " + ready + "\n");
			sb.append("  outbound: " + outbound + "\n");
			if (outbound != null) {
				ArrayList<Block> blockList = getBlocks(separator);
				for (Block b: blockList) {
					if (b.separator.isSubset(outbound.separator)) {
						sb.append("       " + b + "\n");
					}
				}
			}

			for (Block b: inbounds) {
				sb.append("  " + b + "\n");
			}
			sb.append("  sep      :" + separator + "\n");
			return sb.toString();
		}

		@Override
		public int compareTo(PMC pmc) {
			if (lowerBound != pmc.lowerBound) {
				return lowerBound - pmc.lowerBound;  
			}
			return 
					XBitSet.descendingComparator.compare(separator, pmc.separator);
		}
	}

	void log(String logHeader) {
		if (VERBOSE) {
			log(logHeader, System.out);
		}
		if (logFile != null) {
			PrintStream ps;
			try {
				ps = new PrintStream(new FileOutputStream(logFile, true));

				log(logHeader, ps);
				ps.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	void log(String logHeader, PrintStream ps) {
		ps.print(logHeader);

		ps.println();

		//		int sizes[] = tBlockSieve.getSizes();

		ps.print("n = " + g.n) ;

		ps.print(" cost = " + targetCost + 
				"/" + tentativeUB + ", tBlocks = " + tBlockMap.size());
		ps.print("(" + tBlockSieve.size() + ")");

		ps.print(
				", ready = " + 
						readyQueue.size());
		ps.print(
				", endorsed = " + 
						mBlockMap.size());
		ps.print(
				", pendings = " + 
						pmcQueue.size());
		ps.println(
				", blocks = " + blockMap.size());
	}

	private static void test() {
		String path = "instances/";
		//		String path = "instances/test/";
		//		String name = "error5.graph";
		//		String name = "wa10.graph";
		//		String name = "wa14.graph";
		//				String name = "wa11.graph";
		//				String name = "test5.graph";
		//		String name = "2.graph";
		//		String name = "3.graph";
		//		String name = "4.graph";
		//		String name = "5.graph";
		String name = "13.graph";
		LabeledGraph g = Instance.read(path + name);
		//		LabeledGraph g = Instance.read();

		//		System.out.println("Graph " + name + " read");
		//		    for (int v = 0; v < g.n; v++) {
		//		      System.out.println(v + ": " + g.degree[v] + ", " + g.neighborSet[v]);
		//		    }

		long t0 = System.currentTimeMillis();
		Decomposer dec = new Decomposer(g);
		TreeDecomposition td = dec.decompose(-1);

		HashSet<Pair<String, String>> fillEdges = td.computeFill( g );
		long t = System.currentTimeMillis();
		System.out.println(name + " solved with cost " + dec.getOpt() + " with " + (t - t0) + " millisecs");
		System.out.println("== optimal solution ==");
		for (Pair<String, String>  edge: fillEdges) {
			System.out.println(edge.first + " " + edge.second);
		}
		System.out.println("====");
		System.out.println(fillEdges.size() + " edges, " +
				"cost = " + dec.getOpt());
	}

	static class MinComparator implements Comparator<XBitSet> {
		@Override
		public int compare(XBitSet o1, XBitSet o2) {
			return o1.nextSetBit(0) - o2.nextSetBit(0);
		}
	}
	public static void main(String args[]) {
		test();
	}
}

