package fillin.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import tw.common.LabeledGraph;
import tw.common.Pair;
import tw.common.TreeDecomposition;
import tw.common.XBitSet;

public class Solver {

	HashSet< Pair<String, String> > fillEdges;
	int safe = 0;
	private static final int DIRECT_SOLVING_THRESHOLD = 30;
	private static final int SIZE_DECREMENT = 10;
	
	public Solver() {
		fillEdges = new HashSet<>();
	}
	public int solve(LabeledGraph g)
	{
		if (g.n <= 3) {
			return 0;
		}
		LabeledGraph[] graphs = toGraphs( g, g.getComponents( new XBitSet() ) );
		int res = 0;
		for (LabeledGraph sg: graphs) {
			res += solveConnected( sg );
		}
		return res;
	}
	
	public int solveConnected(LabeledGraph g)
	{
		if (g.n <= 3) {
			return 0;
		}
		
		LabeledGraph[] graphs = toGraphs( g, g.decomposeByCutPoints() );
		int res = 0;
		for (LabeledGraph sg: graphs) {
			res += solveBiConnected( sg );
		}
		
		return res;
	}
	
	public int solveBiConnected(LabeledGraph g )
	{
		if (g.n <= 3) {
			return 0;
		}
		
		int fill = Preprocessing.fillSafeFillEdges( g );
		Preprocessing.getSafeFillEdges().forEach(e -> this.fillEdges.add( e ));
		safe += fill;
		ArrayList< XBitSet > components = g.decomposeByCliqueSeparators();
		
		if (components.size() == 1) {
			return solveComponent( g );
		}

		LabeledGraph[] graphs = toGraphs( g, components );
		int res = 0;
		for (LabeledGraph sg: graphs) {
			res += solveComponent( sg );
		}
		return res + fill;
	}
	
	public int solveComponent(LabeledGraph g)
	{
		if (g.isChordal()) {
			return 0;
		}
		
		int ub = getUpperbound( g );
		Decomposer dec = new Decomposer( g );
		TreeDecomposition td = dec.decompose( ub );
		fillEdges.addAll( td.computeFill( g ) );
		return dec.getOpt();
	}
	
	private int getUpperbound(LabeledGraph g) {
//		System.out.println("upperbounding: n = " + g.n);
		if (g.n <= DIRECT_SOLVING_THRESHOLD) {
			return solveComponentDirect(g);
		}

		int deltaUB = 0;
		LabeledGraph h = g;
		while (h.n > g.n - SIZE_DECREMENT) {
			XBitSet separator = bestSeparator(h);
//			System.out.println("n = " + h.n + ", separator: " + separator);
			if (separator == null) {
				break;
			}
			int nfill = h.countFill(separator);
//			System.out.println("n = " + h.n + ", separator: " + separator.cardinality() +
//					", fill = " + nfill);
			deltaUB += nfill;

			ArrayList<XBitSet> components = h.getComponents(separator);

			int nc = components.size();

			Collections.sort(components, 
					(a, b) -> a.cardinality() - b.cardinality());

			for (int i = 0; i < nc; i++) {
				XBitSet compo = components.get(i).unionWith(separator);
				int nv = compo.cardinality();
				String[] labelArray = new String[nv];
				int k = 0;
				int[] conv = new int[g.n];
				for (int v = 0; v < g.n; v++) {
					if (compo.get(v)) {
						conv[v] = k;
						labelArray[k++] = g.getLabel(v);
					}
				}
				LabeledGraph h1 = new LabeledGraph(labelArray);
				for (int u = compo.nextSetBit(0); u >= 0; 
						u = compo.nextSetBit(u + 1)) {
					for (int v = compo.nextSetBit(u + 1); v >= 0; 
							v = compo.nextSetBit(v + 1)) {
						if (h.areAdjacent(u , v) ||
								separator.get(u) && separator.get(v)) {
							h1.addEdge(conv[u], conv[v]);
						}
					}
				}
				if (i < nc - 1) {
					deltaUB += solveComponentDirect(h1);
				} else {
					h = h1;
				}
			}
		}
		if (h == g) {
			return solveComponentDirect(g);
		}
		int ub = getUpperbound(h);
		Decomposer dec = new Decomposer(h);
		dec.decompose(ub);
		return deltaUB + dec.getOpt();
	}
	
	private int solveComponentDirect(LabeledGraph g) {
//		System.out.println("direct: " + g.n);
		if (g.isChordal()) {
//			System.out.println("chordal: " + g.n);
			return 0;
		}
		Decomposer dec = new Decomposer(g);
		dec.decompose(-1);
		return dec.getOpt();
	}
	
	XBitSet bestSeparator(LabeledGraph g) {
		XBitSet best = null;
		for (int v = 0; v < g.n; v++) {
			ArrayList<XBitSet> components = 
					g.getComponents(g.neighborSet[v]);
			if (components.size() == 1) {
				continue;
			}
			for (XBitSet compo: components) {
				XBitSet separator = g.neighborSet(compo);
				if (best == null || g.countFill(separator) < g.countFill(best)) {
					best = separator;
				}
				else if (g.countFill(separator) == g.countFill(best) &&
						smallestComponentSize(g, separator) > smallestComponentSize(g, best)) {
					best = separator;
				}
			}
		}
		return best;
	}
	
	private int smallestComponentSize(LabeledGraph g, XBitSet separator) {
		ArrayList<XBitSet> components = g.getComponents(separator);
		return components.stream().min(Comparator.comparing(comp -> comp.cardinality())).get().cardinality();
	}

	private LabeledGraph[] toGraphs(LabeledGraph g, ArrayList< XBitSet > comps)
	{
		if (comps.size() == 1) {
			return new LabeledGraph[]{ g };
		}
		LabeledGraph[] graphs = new LabeledGraph[ comps.size() ];
		int k = 0;
		for (XBitSet comp: comps) {
			graphs[ k++ ] = toGraph( g, comp );
		}
		return graphs;
	}
	
	private LabeledGraph toGraph(LabeledGraph g, XBitSet comp)
	{
		comp = Preprocessing.reduceSimplicial(g, comp);
		int nv = comp.cardinality();
		String[] labelArray = new String[ nv ];
		int k = 0;
		int[] conv = new int[ g.n ];
		for (int v = 0; v < g.n; v++) {
			if (comp.get(v)) {
				conv[ v ] = k;
				labelArray[ k++ ] = g.getLabel( v );
			}
		}
		LabeledGraph graph  = new LabeledGraph( labelArray );
		for (int u = comp.nextSetBit( 0 ); u >= 0; u = comp.nextSetBit( u + 1 )) {
			for (int v = comp.nextSetBit( u + 1 ); v >= 0; v = comp.nextSetBit( v + 1 )) {
				if (g.areAdjacent( u , v )) {
					graph.addEdge( conv[ u ], conv[ v ] );
				}
			}
		}
		return graph;
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		Solver solver = new Solver();
		solver.solve( Instance.read("instances/2.graph") );
		solver.fillEdges.forEach(e -> System.out.println(e.first + " " + e.second));
	}
}
