package fillin.main;

import java.io.FileNotFoundException;
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
	HashSet< Pair<String, String> > safeFill;
	private static final int DIRECT_SOLVING_THRESHOLD = 32;
	private static final int SIZE_DECREMENT = 8;

	
	public int getOpt() {
		return fillEdges.size();
	}
	
	public Solver() {
		fillEdges = new HashSet<>();
		safeFill = new HashSet<>();
	}

	public void solve(LabeledGraph g)
	{
		if (g.n <= 3) {
			return;
		}
		LabeledGraph[] graphs = toGraphs( g, g.getComponents( new XBitSet( g.n ) ) );
		Arrays.stream( graphs ).forEach(sg -> solveConnected( sg ));
		fillEdges.addAll( safeFill );
	}
	
	public void solveConnected(LabeledGraph g)
	{
		if (g.n <= 3) {
			return;
		}
		
		LabeledGraph[] graphs = toGraphs( g, g.decomposeByCutPoints() );
		Arrays.stream( graphs ).forEach(sg -> solveBiconnected( sg ));
	}
	
	public void solveBiconnected(LabeledGraph g)
	{
		XBitSet separator = safeSeparator( g );
		if (separator == null) {
			solveComponent( g );
		} else {
			ArrayList< XBitSet > components = g.getComponents( separator );
			if (components.size() == 1) {
				solveComponent( g );
			} else {
				for (XBitSet comp: components) {
					comp.or( separator );
					solveBiconnected( toGraph( g, comp ) );
				}
			}
		}
	}
	
	public void solveComponent(LabeledGraph g)
	{
		if (g.n <= 3) {
			return;
		}
		ArrayList< XBitSet > components = g.decomposeByCliqueSeparators();
		
		if (components.size() == 1) {
			solveReduced( g );
		} else {
			LabeledGraph[] graphs = toGraphs( g, components );
			for (LabeledGraph sg: graphs) {
				solveReduced( sg );
			}
		}
	}
	
	public void solveReduced(LabeledGraph g)
	{
		if (g.isChordal()) {
			return;
		}
//		g = eliminate(g);
		int ub = getUpperbound( g );
		Decomposer dec = new Decomposer( g );
		TreeDecomposition td = dec.decompose( ub );
		fillEdges.addAll( td.computeFill( g ) );
	}
	
	private int getUpperbound(LabeledGraph g) {
		if (g.n <= DIRECT_SOLVING_THRESHOLD) {
			return solveComponentDirect(g);
		}

		int deltaUB = 0;
		LabeledGraph h = g;
		while (h.n > g.n - SIZE_DECREMENT) {
			XBitSet separator = bestSeparator(h);
			if (separator == null) {
				break;
			}
			int nfill = h.countFill(separator);
			deltaUB += nfill;

			ArrayList<XBitSet> components = h.getComponents(separator);

			int nc = components.size();

			Collections.sort(components, (a, b) -> a.cardinality() - b.cardinality());

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
				for (int u = compo.nextSetBit(0); u >= 0; u = compo.nextSetBit(u + 1)) {
					for (int v = compo.nextSetBit(u + 1); v >= 0; v = compo.nextSetBit(v + 1)) {
						if (h.areAdjacent(u, v) || separator.get(u) && separator.get(v)) {
							h1.addEdge(conv[u], conv[v]);
						}
					}
				}
				h1 = toGraph(h1, h1.all);
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
		if (g.isChordal()) {
			return 0;
		}

		Decomposer dec = new Decomposer(g);
		dec.decompose(-1);
		return dec.getOpt();
	}
	
	XBitSet bestSeparator(LabeledGraph g) {
		XBitSet best = null;
		int bestNcomp = 0;
		for (int v = 0; v < g.n; v++) {
			ArrayList<XBitSet> components = g.getComponents(g.neighborSet[v]);
			if (components.size() == 1) {
				continue;
			}
			for (XBitSet compo: components) {
				XBitSet separator = g.neighborSet(compo);
				if (best == null || g.countFill(separator) < g.countFill(best)) {
					best = separator;
					bestNcomp = g.getComponents( separator ).size();
				} else if (g.countFill(separator) == g.countFill(best)) {
					int ncomp = g.getComponents( separator ).size();
					if (ncomp > bestNcomp) {
						best = separator;
						bestNcomp = ncomp;
					} else if (ncomp == bestNcomp) {
						if (smallestComponentSize(g, separator) > smallestComponentSize(g, best)) {
							best = separator;
							bestNcomp = ncomp;
						}
					}
				}
			}
		}
		return best;
	}
	
	private int smallestComponentSize(LabeledGraph g, XBitSet separator) {
		ArrayList<XBitSet> components = g.getComponents(separator);
		return components.stream().min(Comparator.comparing(comp -> comp.cardinality())).get().cardinality();
	}

	private XBitSet safeSeparator(LabeledGraph g)
	{
		XBitSet safeSeparator = null;
		for (int u = 0; u < g.n; u++) {
			ArrayList< XBitSet > components = g.getComponents( g.neighborSet[ u ] );
			for (XBitSet comp: components) {
				if (comp.get( u )) {
					continue;
				}
				XBitSet separator = g.neighborSet( comp );
				
				int missing = g.countFill( separator );
				if (missing == 0) {
					if (safeSeparator == null) {
						safeSeparator = separator;
					}
				} else if (missing == 1) {
					if (safeSeparator == null) {
						safeSeparator = separator;
					}
					safeFill( g, separator );
				}
			}
		}
		
		if (safeSeparator != null) {
			return safeSeparator;
		}
		
		for (int u = 0; u < g.n; u++) {
			ArrayList< XBitSet > components = g.getComponents( g.neighborSet[ u ] );
			for (XBitSet comp: components) {
				if (comp.get( u )) {
					continue;
				}
				if (isSafe( g, comp, u )) {
					XBitSet separator = g.neighborSet( comp );
					safeFill( g, separator );
					if (safeSeparator == null) {
						safeSeparator = separator;
					}
				} 
				
			}
		}
		return safeSeparator;
	}
	
	private boolean isSafe(LabeledGraph g, XBitSet A, int b)
	{
		XBitSet separator = g.neighborSet( A );
		XBitSet R = (XBitSet) separator.clone();
		int missing = g.countFill( separator );
		if (missing == 0) {
			return true;
		}
		int x = 0;
		// separator must not be a clique
		for (int u = separator.nextSetBit( 0 ); u >= 0; u = separator.nextSetBit( u + 1 )) {
			R.clear( u );
			
			if (g.isClique( R )) {
				x = u;
				break;
			}
			
			R.set( u );
		}
		if (R.cardinality() == separator.cardinality()) {
			return false;
		}
		for (XBitSet comp: g.getComponents(separator)) {
			if (comp.get( b )) continue;
			XBitSet subSep = g.neighborSet( comp );
			if (subSep.isSubset( separator )) {
				A.or( comp );
			}
		}
		
		// separator must be an almost clique (that is, clique + x for some x in R)
		R.andNot( g.neighborSet[ x ] );
		VertexDisjointPaths vdp = new VertexDisjointPaths(g, x, R, A);
		return vdp.find(R.cardinality());
	}

	private void safeFill(LabeledGraph g, XBitSet separator) {
		int missing = g.countFill( separator );
		if (missing == 0) {
			return;
		}
		for (int v = separator.nextSetBit( 0 ); v >= 0; v = separator.nextSetBit( v + 1 )) {
			for (int w = separator.nextSetBit( v + 1 ); w >= 0; w = separator.nextSetBit( w + 1 )) {
				if (g.areAdjacent( v , w ) == false) {
					g.addEdge(v, w);
					String vl = g.getLabel( v );
					String wl = g.getLabel( w );
					if (vl.compareTo( wl ) < 0) { 
						safeFill.add(new Pair< String, String >( vl, wl ));
					} else {
						safeFill.add(new Pair< String, String >( wl, vl ));
					}
					missing--;
					if (missing <= 0) {
						return;
					}
				}
			}
		}
	}
	
	private XBitSet reduceSimplicial(LabeledGraph g, XBitSet compo) {
		XBitSet res = (XBitSet)compo.clone();
		boolean hasSimplicial;
		do {
			hasSimplicial = false;
			for (int u = compo.nextSetBit( 0 ); u >= 0; u = compo.nextSetBit( u + 1 )) {
				if (g.isClique( compo.intersectWith( g.neighborSet[ u ] ) )) {
					res.clear( u );
					hasSimplicial = true;
				}
			}
			compo = res;
		} while (hasSimplicial);
		return compo;
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
		comp = reduceSimplicial(g, comp);
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
		LabeledGraph g = Instance.read();
		solver.solve( g );
		solver.fillEdges.forEach(e -> System.out.println(e.first + " " + e.second));
	}
}
