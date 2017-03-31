package fillin.main;

import java.util.ArrayList;
import java.util.Arrays;

import tw.common.LabeledGraph;
import tw.common.XBitSet;

public class Bounds {
	//  private static final boolean DEBUG = true;
	private static final boolean DEBUG = false;
	private static final int MAX_CYCLE_LENGTH = 8;

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
		for (int v = vertexSet.nextSetBit(0); v >= 0; v = vertexSet.nextSetBit(v + 1)) {
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

	// generate chordless cycles that contains v
	private boolean generateChordlessCycles(int v, int i, XBitSet available) {
		if (DEBUG) {
			System.out.print("generateChordlessCycles called:");
			for (int j = 0; j < i; j++) {
				System.out.print(" " + path[j]);
			}
			System.out.println();
		}
		int last = path[i - 1];
		if (i >= 4 && neighborSet[last].get(v)) {
			if (DEBUG) {
				System.out.print("chordless cycle found:");
				for (int j = 0; j < i; j++) {
					System.out.print(" " + path[j]);
				}
				System.out.println();
			}
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

	public int upperbound() {
		return g.n * (g.n - 1) / 2 - g.numberOfEdges();
	}

	private int[] chordlessCycle(int v, int i) {
		if (i >= 4 && neighborSet[path[i - 1]].get(v)) {
			if (DEBUG) {
				System.out.print("chordless cycle found:");
				for (int j = 0; j < i; j++) {
					System.out.print(" " + path[j]);
				}
				System.out.println();
			}
			return Arrays.copyOf(path, i);
		}
		int u = path[i - 1];
		if (i >= 3 && neighborSet[u].get(v)) {
			return null;
		}
		for (int w = neighborSet[u].nextSetBit(0); w >= 0;
				w = neighborSet[u].nextSetBit(w + 1)) {
			if (indexOf(w, path, i) < 0 &&
					!hasChord(i, neighborSet[w])) {
				path[i] = w;
				int[] cycle = chordlessCycle(v, i + 1);
				if (cycle != null) {
					return cycle;
				}
			}
		}
		return null;
	}

	private int indexOf(int x, int[] a, int n) {
		for (int i = 0; i < n; i++) {
			if (a[i] == x) {
				return i;
			}
		}
		return -1;
	}

	private boolean hasChord(int i, XBitSet chordNeighbors) {
		for (int j = 1; j < i - 1; j++) {
			if (chordNeighbors.get(path[j])) {
				return true;
			}
		}
		return false;
	}
}
