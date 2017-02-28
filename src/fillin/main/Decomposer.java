package fillin.main;

import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeSet;

import tw.common.BlockSieve;
import tw.common.LabeledGraph;
import tw.common.TreeDecomposition;
import tw.common.XBitSet;

public class Decomposer {

	static final boolean VERBOSE = true;
	//  private static final boolean VERBOSE = false;
	private static boolean DEBUG = false;
	//	static boolean DEBUG = true;

	LabeledGraph g;

	BlockSieve tBlockSieve;
	//	SetTrie< TBlock > tBlockTrie;

	Queue< MBlock > readyQueue;

	Map<XBitSet, MBlock> mBlockMap;

	Map<XBitSet, TBlock> tBlockMap;

	Map<XBitSet, Block> blockMap;

	Map<XBitSet, PMC> pmcMap;

	TreeSet<PMC> pmcQueue;

	int targetCost;

	PMC solution;

	File logFile;

	int tentativeUB;

	public Decomposer(LabeledGraph g) {
		this.g = g;
	}
	
	public int getOpt()
	{
		return targetCost;
	} 

	public TreeDecomposition decompose() {

		blockMap = new HashMap<>();
		
		int upperbound = g.n * (g.n - 1) / 2 - g.edges();
		for (tentativeUB = 1; 
			 tentativeUB <= upperbound;
			 tentativeUB = Math.min(upperbound, tentativeUB * 2)) {
			mBlockMap = new HashMap<>();
			pmcMap = new HashMap<>();
			pmcQueue = new TreeSet<>();

			tBlockMap = new HashMap<>();
			tBlockSieve = new BlockSieve(g.n);

			readyQueue = new LinkedList<>();
			for (int v = 0; v < g.n; v++) {
				XBitSet cnb = (XBitSet) g.neighborSet[v].clone();
				cnb.set(v);

				if (isPMC(cnb) == false || pmcMap.get(cnb) != null) {
					continue;
				}

				PMC pmc = new PMC(cnb);
				pmcMap.put(cnb, pmc);
				pmcQueue.add(pmc);
			}
			
			readyQueue.addAll(mBlockMap.values());
			
			for (targetCost = 0 ; targetCost <= tentativeUB; targetCost++) {
				// (1) all M-blocks with optimal cost < targetCost
				// that use only bags with fillin <= tentativeUB
				// have been generated and have the optimal cost computed
				// (2) all T-blocks obtainable as a combination of M-blocks 
				// as in (1) that have a separator with fillin <= tentativeUB
				// have been generated
				// (3) all PMCs whose all inbound M-blocks as in (1)
				// have been generated

				while (true) {
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

					for (PMC pmc: toConsider) {
						pmc.process();
					}

					if (readyQueue.isEmpty()) {
						break;
					}

					while (readyQueue.isEmpty() == false) {
						readyQueue.poll().process();
					}
				}

				if (solution != null) {
					return constructTD();
				}
			}
		}
		return null;
	}

	private final Block getBlock(XBitSet component)
	{
		Block block = blockMap.get(component);
		if (block == null) {
			block = new Block(component);
			blockMap.put(component, block);
		}
		return block;
	}

	TreeDecomposition constructTD()
	{
		TreeDecomposition td = new TreeDecomposition(0, g.n - 1, g);
		solution.recurseTD( td );
		return td;
	}

	private int[] toBag(XBitSet set) {
		int[] result = new int[set.cardinality()];
		for (int v = set.nextSetBit(0), i = 0; v >= 0; v = set.nextSetBit(v + 1)) {
			result[ i++ ] = v;
		}
		return result;
	}

	XBitSet getMirrorComponent(Block block, XBitSet separator) {
		XBitSet ns = block.separator;
		XBitSet diff = separator.subtract(ns);

		int v = diff.nextSetBit(0);
		XBitSet work = (XBitSet) g.neighborSet[v].clone();
		work.set(v);
		XBitSet scope = work.subtract(ns);

		while (!scope.isEmpty()) {
			XBitSet work1 = (XBitSet) work.clone();
			for (int w = scope.nextSetBit(0); w >= 0;
					w = scope.nextSetBit(w + 1)) {
				work1.or(g.neighborSet[w]);
			}
			scope = work1.subtract(work).subtract(ns);
			work = work1;
		}
		if (separator.isSubset(work)) {
			return work.subtract(ns);
		} else {
			return null;
		}
	}

	ArrayList<Block> getBlocks(XBitSet separator) {
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
				if (tBlock.relevant()) {
					tBlockMap.put(separator, tBlock);
					tBlockSieve.put(outbound, tBlock);
					//        tBlockTrie.put( outbound, tBlock );
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
					}
					else {
						cost += mBlock.cost;
					}
				}
			}
			return cost <= tentativeUB;
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

				TBlock tBlock = tBlockMap.get(newsep); 
				if (tBlock == null) {
					tBlock = new TBlock(newsep, fullBlock.component);
					if (tBlock.relevant()) {
						tBlockSieve.put(fullBlock.component, tBlock);
						tBlockMap.put(newsep, tBlock);
						//        tBlockTrie.put(fullBlock.component, tBlock);
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
			} else if (outbound == null) {
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

