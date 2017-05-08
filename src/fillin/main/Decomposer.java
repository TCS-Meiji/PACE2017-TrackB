package fillin.main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import tw.common.BlockSieve;
import tw.common.LabeledGraph;
import tw.common.TreeDecomposition;
import tw.common.XBitSet;

public class Decomposer {

	private static final boolean VERBOSE = true;
//	private static final boolean VERBOSE = false;

	LabeledGraph g;
	int lowerbound;

	BlockSieve tBlockSieve;
	Queue< MBlock > readyQueue;
	Map<XBitSet, MBlock> mBlockMap;
	Map<XBitSet, TBlock> tBlockMap;
	Map<XBitSet, Block> blockMap;
	Map<XBitSet, PMC> pmcMap;
//	Map<XBitSet, Boolean> pmcCache;
	PriorityQueue<PMC> pmcQueue;
	
	PMC solution;
	int targetCost;
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
		Bounds bounds = new Bounds(g);
		lowerbound = bounds.lowerbound();
		if (VERBOSE) {
			System.out.println("n = " + g.n +  ", lb = " + lowerbound + ", ub = " + upperbound);
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
//		pmcCache = new HashMap<>();

		for (tentativeUB = start; tentativeUB <= end; tentativeUB += increment){

			mBlockMap = new HashMap<>();
			pmcMap = new HashMap<>();
//			pmcQueue = new TreeSet<>();
			pmcQueue = new PriorityQueue<>((a, b) -> a.lowerBound - b.lowerBound);
			tBlockMap = new HashMap<>();
			tBlockSieve = new BlockSieve(g.n);
			readyQueue = new LinkedList<>();
			
			for (int v = 0; v < g.n; v++) {
				XBitSet cnb = g.closedNeighborSet( v );

				if (pmcMap.get(cnb) != null || isPMC(cnb) == false) {
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
				if (VERBOSE) {
					System.out.println("Trying " + targetCost + "/" + tentativeUB + " TBlocks = " + tBlockMap.size());
				}

				while (true) {
					ArrayList<PMC> toConsider = new ArrayList<>();
					while (!pmcQueue.isEmpty()) {
//						PMC pmc = pmcQueue.first();
						PMC pmc = pmcQueue.peek();
						if (pmc.lowerBound <= targetCost) {
							toConsider.add(pmc);
//							pmcQueue.remove(pmc);
							pmcQueue.poll();
						} else {
							break;
						}
					}

					toConsider.forEach(pmc -> pmc.process());

					if (readyQueue.isEmpty()) {
						break;
					}
					
					while (readyQueue.isEmpty() == false) {
						MBlock ready = readyQueue.poll();
						ready.process();
					}
				}

				if (solution != null) {
					TreeDecomposition td = constructTD();
					return td;
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
//		return new Block(component);
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
	
	public boolean isPMC(XBitSet separator) {
//		Boolean isPMC = pmcCache.get(separator);
//		if (isPMC != null) {
//			return isPMC;
//		}
		ArrayList<Block> blockList = getBlocks(separator);
		int nsep = separator.cardinality();
		for (Block block: blockList) {
			if (nsep == block.separator.cardinality()) {
				// separator has a full component
//				pmcCache.put(separator, false);
				return false;
			}
		}
		for (int v = separator.nextSetBit(0); v >= 0; v = separator.nextSetBit(v + 1)) {	
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
//					pmcCache.put(separator, false);
					return false;
				}
			}
		}
//		pmcCache.put(separator, true);
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
					for (int w = toBeScanned.nextSetBit(0); w >= 0; w = toBeScanned.nextSetBit(w + 1)) {
						c.or(g.neighborSet[w]);
					}
					toBeScanned = c.subtract(save);
					toBeScanned.andNot(separator);
				}
				if (separator.isSubset(c)) {
					// full block other than "component" found
					if (v < minCompo) {
						outbound = c.subtract(separator);
					} else {
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

		@Override
		public int compareTo(Block b) {
			return component.nextSetBit(0) - b.component.nextSetBit(0);
		}

	}

	class MBlock {
		XBitSet inbound;
		XBitSet separator;
		XBitSet outbound;
		PMC pmc;
		int cost;

		MBlock(XBitSet inbound, XBitSet separator, XBitSet outbound, PMC pmc, int cost)
		{
			this.inbound = inbound;
			this.separator = separator;
			this.outbound = outbound;
			this.pmc = pmc;
			this.cost = cost;
		}

		void process() {
			makeSimpleTBlock();
			tBlockSieve.collectSuperblocks(inbound, separator, new ArrayList<>()).forEach(tBlock -> tBlock.plugin( this ));
		}

		void makeSimpleTBlock() {

			TBlock tBlock = tBlockMap.get(separator); 
			if (tBlock == null) {
				tBlock = new TBlock(separator, outbound);
				tBlockMap.put(separator, tBlock);
				if (tBlock.relevant()) {
					tBlockSieve.put(outbound, tBlock);
					tBlock.crown();
				}
			}
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
			return cost <= tentativeUB;

		}

		void plugin(MBlock mBlock) {

			XBitSet newsep = separator.unionWith(mBlock.separator);

			if (g.countFill(newsep) > tentativeUB) {
				return;
			}

			XBitSet extendedSep = g.all.subtract(openComponent);
			extendedSep.or(mBlock.separator);

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
			for (int v = separator.nextSetBit(0); v >= 0; v = separator.nextSetBit(v + 1)) {
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
				if (isPMC( newsep ) == false) {
					continue;
				}
				PMC pmc = new PMC(newsep);
				pmc.process();
			} 
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
				if (block.isOutbound() && (outbound == null || outbound.separator.isSubset(block.separator))){
					outbound = block;
				}
			}
			if (outbound == null) {
				inbounds = blockList.toArray(new Block[blockList.size()]);  
			} else {
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
			if (!ready) {
				lowerBound = g.countFill(separator);
				if (outbound != null) { 
					lowerBound -= g.countFill(outbound.separator);
				}
				ready = true;
				for (Block block: inbounds) {
					MBlock mBlock = mBlockMap.get(block.component);
					if (mBlock == null || mBlock.cost > targetCost) {
						ready = false;
						lowerBound += targetCost;
					} else {
						lowerBound += mBlock.cost;
					}
				}
			}
		}

		void process() {
			evaluate();
			if (ready && lowerBound <= targetCost) {
				endorse();
			} else {
				pmcQueue.add(this);
			}
		}

		void endorse() {
			int cost = countFill();
			if (cost < targetCost) {
				// this pmc must have been generated in some other way and have been processed
				return;
			}
			if (outbound == null) {
				solution = this;
				return;
			} else {
				endorse( getTarget(), cost );
			}
		}

		void endorse(XBitSet target, int cost) {
			MBlock mBlock = mBlockMap.get( target );
			if (mBlock == null) {
				mBlock = new MBlock( target, outbound.separator, outbound.component, this, cost);
				mBlockMap.put(target, mBlock);
				readyQueue.add( mBlock );
			} 
		}

		XBitSet getTarget() {
			if (outbound == null) {
				return null;
			}
			XBitSet combined = separator.subtract(outbound.separator);
			Arrays.stream(inbounds).forEach(b -> combined.or(b.component));
			return combined;
		}

		int countFill() {
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
			int j = td.addBag(toBag(separator));

			for (Block block: inbounds) {
				PMC subEndorser = mBlockMap.get(block.component).pmc;
				if (subEndorser == null) {
					continue;
				}
				int k = subEndorser.recurseTD(td);
				td.addEdge(j, k);
			}
			return j;
		}

		@Override
		public int compareTo(PMC pmc) {
			if (lowerBound != pmc.lowerBound) {
				return lowerBound - pmc.lowerBound;  
			}
			return XBitSet.descendingComparator.compare(separator, pmc.separator);
		}
	}

	static class MinComparator implements Comparator<XBitSet> {
		@Override
		public int compare(XBitSet o1, XBitSet o2) {
			return o1.nextSetBit(0) - o2.nextSetBit(0);
		}
	}
}

