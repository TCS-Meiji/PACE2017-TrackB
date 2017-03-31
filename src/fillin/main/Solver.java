package fillin.main;

//import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import tw.common.IntQueue;
import tw.common.LabeledGraph;
import tw.common.Pair;
import tw.common.TreeDecomposition;
import tw.common.XBitSet;

public class Solver {

	HashSet< Pair<String, String> > fillEdges;
	HashSet< Pair<String, String> > safeFill;
	private static final int DIRECT_SOLVING_THRESHOLD = 30;
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
		LabeledGraph[] graphs = toGraphs( g, g.getComponents( new XBitSet() ) );
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
		if (g.n <= 3) {
			return;
		}
		
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
		
//		int ub = getUpperbound( g );
//		Decomposer dec = new Decomposer( g );
//		TreeDecomposition td = dec.decompose( ub );
//		fillEdges.addAll( td.computeFill( g ) );
	}
	
	private int getUpperbound(LabeledGraph g) {
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
				for (int u = compo.nextSetBit(0); u >= 0; u = compo.nextSetBit(u + 1)) {
					for (int v = compo.nextSetBit(u + 1); v >= 0; v = compo.nextSetBit(v + 1)) {
						if (h.areAdjacent(u, v) ||
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
		if (g.isChordal()) {
			return 0;
		}
		
		Decomposer dec = new Decomposer(g);
		dec.decompose(-1);
		return dec.getOpt();
	}
	
	XBitSet bestSeparator(LabeledGraph g) {
		XBitSet best = null;
		for (int v = 0; v < g.n; v++) {
			ArrayList<XBitSet> components = g.getComponents(g.neighborSet[v]);
			if (components.size() == 1) {
				continue;
			}
			for (XBitSet compo: components) {
				XBitSet separator = g.neighborSet(compo);
				if (best == null || g.countFill(separator) < g.countFill(best)) {
					best = separator;
				} else if (g.countFill(separator) == g.countFill(best) &&
						smallestComponentSize(g, separator) > smallestComponentSize(g, best)) {
					best = separator;
				}
			}
		}
		return best;
	}
	
	XBitSet balancedSeparator(LabeledGraph g)
	{
		XBitSet best = null;
		int size = g.n + 1;
		for (int v = 0; v < g.n; v++) {
			ArrayList< XBitSet > components = g.getComponents( g.neighborSet[ v ] );
			if (components.size() == 1) {
				continue;
			}
			for (XBitSet compo: components) {
				XBitSet separator = g.neighborSet( compo );
				int tsize = largestComponentSize(g, separator);
				if (tsize < size) {
					best = separator;
					size = tsize;
				} else if (tsize == size && g.countFill(separator) < g.countFill(best)) {
					best = separator;
				}
			}
		}
		return best;
	}
	private int largestComponentSize(LabeledGraph g, XBitSet separator)
	{
		ArrayList< XBitSet > components = g.getComponents( separator );
		return components.stream().max(Comparator.comparing(comp -> comp.cardinality())).get().cardinality();
	}
	
	private int smallestComponentSize(LabeledGraph g, XBitSet separator) {
		ArrayList<XBitSet> components = g.getComponents(separator);
		return components.stream().min(Comparator.comparing(comp -> comp.cardinality())).get().cardinality();
	}
	
	private XBitSet safeSeparator(LabeledGraph g)
	{
		for (int u = 0; u < g.n; u++) {
			ArrayList< XBitSet > components = g.getComponents( g.neighborSet[ u ] );
			for (XBitSet comp: components) {
				if (comp.get( u )) {
					continue;
				}
				XBitSet separator = g.neighborSet( comp );
				
				int missing = g.countFill( separator );
				if (missing == 0) {
					return separator;
				}
				if (missing != 1) {
					continue;
				}

				safeFilling( g, separator );
				return separator;
			}
		}
		
		for (int u = 0; u < g.n; u++) {
			ArrayList< XBitSet > components = g.getComponents( g.neighborSet[ u ] );
			for (XBitSet comp: components) {
				if (comp.get( u )) {
					continue;
				}
				if (isSafe( g, comp, u ) == false) {
					continue;
				}
				
				XBitSet separator = g.neighborSet( comp );
				safeFilling( g, separator );
				return separator;
			}
		}
		
//		for (int u = 0; u < g.n; u++) {
//			ArrayList< XBitSet > components = g.getComponents( g.neighborSet[ u ] );
//			L: for (XBitSet comp: components) {
//				XBitSet separator = g.neighborSet( comp );
//				if (separator.size() == 4) {
//					if (g.countFill( separator ) != 2) {
//						continue;
//					}
//					for (int v = separator.nextSetBit( 0 ); v >= 0; v = separator.nextSetBit( v + 1 )) {
//						if (g.neighborSet[ v ].intersectWith( separator ).cardinality() != 2) {
//							continue L;
//						}
//					}
//					// separator induces an induced C_4
//					for (int v = separator.nextSetBit( 0 ); v >= 0; v = separator.nextSetBit( v + 1 )) {
//						for (int w = separator.nextSetBit( v + 1 ); w >= 0; w = separator.nextSetBit( w + 1 )) {
//							if (g.areAdjacent( v, w )) {
//								continue;
//							}
//							g.addEdge( v, w );
//							if (isSafe(g, comp, u) == false) {
//								g.removeEdge( v, w );
//								continue L;
//							}
//							g.removeEdge( v, w );
//						}
//					}
//					// every minimal fill-in of G[separator] is guard-safe
//					System.out.println("detect");
//					safeFilling( g, separator );
//					return separator;
//				}
//			}
//		}
		
		return null;
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
		
		// separator must be clique + x, where x is the unique element of separator \ R
		R.andNot( g.neighborSet[ x ] );
		V[] path = new V[ g.n ];
		ArrayList< V > vs = new ArrayList<>();
		for (int y = R.nextSetBit( 0 ); y >= 0; y = R.nextSetBit( y + 1 )) {
			V v = new V();
			vs.add( v );
			XBitSet Nxy = g.neighborSet[ x ].intersectWith( g.neighborSet[ y ] );
			Nxy.and( A );
			for (int a = Nxy.nextSetBit( 0 ); a >= 0; a = Nxy.nextSetBit( a + 1 )) {
				if (path[ a ] == null) {
					path[ a ] = new V();
				}
				v.add( path[ a ] );
				path[ a ].add( v );
			}
		}
		
		int matching = matching( vs );
		if (missing == matching) {
			return true;
		}
		
		// find chordless paths from x to each y \in Y in A
		XBitSet rest = A.unionWith( R );
		for (int y = R.nextSetBit( 0 ); y >= 0; y = R.nextSetBit( y + 1 )) {
			XBitSet cp = visit( g, rest, x, y );
			if (cp == null) {
				return false;
			}
			rest.andNot( cp );
		}
		
		return true;
	}
	
	private XBitSet visit(LabeledGraph g, XBitSet rest, int from, int to)
	{
		IntQueue que = new IntQueue( rest.cardinality() );
		que.offer( from );
		int[] prev = new int[ g.n ];
		Arrays.fill( prev, -1 );
		prev[ from ] = from;
		while (que.isEmpty() == false) {
			int v = que.poll();
			XBitSet next = rest.intersectWith( g.neighborSet[ v ] );
			for (int w = next.nextSetBit( 0 ); w >= 0; w = next.nextSetBit( w + 1 )) {
				if (prev[ w ] == -1) {
					prev[ w ] = v;
					que.offer( w );
					if (w == to) {
						XBitSet path = new XBitSet( g.n );
						while (prev[ w ] != w) {
							path.set( w );
							w = prev[ w ];
						}
						return path;
					}
				}
			}
		}
		return null;
	}

	private void safeFilling(LabeledGraph g, XBitSet separator) {
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
	
	private static int matching(ArrayList< V > vs) {
		int match = 0;
		for (V u: vs) {
			if (u.match == null) {
				for (V v: vs) {
					v.visited = false;
				}
				if (dfs( u )) {
					match++;
				}
			}
		}
		return match;
	}
	
	private static boolean dfs(V v) {
		v.visited = true;
		for (V u: v) {
			if (u.match == null || u.match.visited == false && dfs( u.match )) {
				v.match = u;
				u.match = v;
				return true;
			}
		}
		return false;
	}

	private static class V extends ArrayList< V >{
		V match;
		boolean visited;
	}
	
	private static XBitSet reduceSimplicial(LabeledGraph g, XBitSet compo) {
		XBitSet res = (XBitSet)compo.clone();
		boolean hasSimplicial = false;
		for (int u = compo.nextSetBit( 0 ); u >= 0; u = compo.nextSetBit( u + 1 )) {
			if (g.isClique( compo.intersectWith( g.neighborSet[ u ] ) )) {
				res.clear( u );
				hasSimplicial = true;
			}
		}
		if (hasSimplicial) {
			return reduceSimplicial( g , res );
		} else {
			return res;
		}
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
		String name = "100.graph";
		LabeledGraph g = Instance.read("instances/" + name);
		int n = g.n;
		int m = g.edges();
		long stime = System.currentTimeMillis();
//		LabeledGraph g = Instance.read();
		solver.solve( g );
		long ftime = System.currentTimeMillis();
//		solver.fillEdges.forEach(e -> System.out.println(e.first + " " + e.second));
		System.out.println("name = " + name + ", |V| = " + n + ", |E| = " + m +
				", opt = " + solver.fillEdges.size() + ", safe = " + solver.safeFill.size() + ", time = " + (ftime - stime));
	}
}
