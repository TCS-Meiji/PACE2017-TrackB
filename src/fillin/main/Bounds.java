package fillin.main;

import java.util.Arrays;

import tw.common.LabeledGraph;
import tw.common.XBitSet;

public class Bounds {
	private static final int MAX_CYCLE_LENGTH = 16;
	private static final int DIRECT_SOLVING_THRESHOLD = 30;

	LabeledGraph g;

	XBitSet vertexSet;
	XBitSet[] neighborSet;
	XBitSet[] chordNeighborSet;

	int[] path;
	int lb;
	int ub;

	public Bounds(LabeledGraph g) {
		this.g = g;
		neighborSet = new XBitSet[g.n];
		chordNeighborSet = new XBitSet[g.n];
		path = new int[g.n];
	}

	public int lowerbound() {
		return lowerbound(g.all, new XBitSet(g.n));
	}

	public int lowerbound(XBitSet component, XBitSet separator) {
		setSubgraph(component, separator);
		lb = 0;
		XBitSet available = (XBitSet) vertexSet.clone();
		for (int v = vertexSet.nextSetBit(0); v >= 0;
				v = vertexSet.nextSetBit(v + 1)) {
			available.clear(v);
			path[0] = v;
			generateChordlessCycles(v, 1, (XBitSet) available.clone());
		}
		return lb;
	}

	private void setSubgraph(XBitSet component, XBitSet separator) {
		vertexSet = component.unionWith(separator);
		for (int v = vertexSet.nextSetBit(0); v >= 0; v = vertexSet.nextSetBit(v + 1)) {
			neighborSet[v] = g.neighborSet[v].intersectWith(vertexSet);
			if (separator.get(v)) {
				neighborSet[v].or(separator);
			}
			neighborSet[v].clear(v);
			chordNeighborSet[v] = (XBitSet) neighborSet[v].clone();
		}
	}

	private boolean generateChordlessCycles(int v, int i, XBitSet available) {
		int last = path[i - 1];
		if (i >= 4 && neighborSet[last].get(v)) {
			lb += i - 3;
			for (int j = 0; j < i; j++) {
				int x = path[j];
				for (int k = j + 2; k < i; k++) {
					int y = path[k];
					chordNeighborSet[x].set(y);
					chordNeighborSet[y].set(x);
				}
			}
			return true;
		}
		if (i == MAX_CYCLE_LENGTH) {
			return false;
		}

		if (i >= 3 && chordNeighborSet[last].get(v)) {
			return false;
		}
		XBitSet candidate = available.intersectWith(neighborSet[last]);
		for (int w = candidate.nextSetBit(0); w >= 0; w = candidate.nextSetBit(w + 1)) {
			path[i] = w;
			available.clear(w);
			XBitSet del = null;
			if (i >= 2) {
				del = available.intersectWith(chordNeighborSet[last]);
				available.andNot(del);
			}

			boolean changed = generateChordlessCycles(v, i + 1, available);

			if (i >= 2) {
				available.or(del);
			}
			available.set(w);
			if (i >= 3 && changed) {
				return true;
			}
		}
		return false;
	}
}
