package fillin.experiment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import fillin.main.Decomposer;
import fillin.main.Instance;
import tw.common.IntQueue;
import tw.common.LabeledGraph;
import tw.common.Pair;
import tw.common.TreeDecomposition;
import tw.common.XBitSet;

public class Decompose {

	public static void decompose(LabeledGraph g, PrintWriter pw)
	{
		if (g.n <= 3) {
			return;
		}
		LabeledGraph[] graphs = toGraphs( g, g.getComponents( new XBitSet() ) );
		Arrays.stream( graphs ).forEach(sg -> decomposeConnected( sg, pw ));
	}

	public static void decomposeConnected(LabeledGraph g, PrintWriter pw)
	{
		if (g.n <= 3) {
			return;
		}

		LabeledGraph[] graphs = toGraphs( g, g.decomposeByCutPoints() );
		Arrays.stream( graphs ).forEach(sg -> decomposeBiconnected( sg, pw ));
	}

	public static void decomposeBiconnected(LabeledGraph g, PrintWriter pw)
	{
		if (g.n <= 3) {
			return;
		}

		XBitSet separator = safeSeparator( g );
		if (separator == null) {
			decomposeComponent( g, pw );
		} else {
			ArrayList< XBitSet > components = g.getComponents( separator );
			if (components.size() == 1) {
				decomposeComponent( g, pw );
			} else {
				for (XBitSet comp: components) {
					comp.or( separator );
					decomposeBiconnected( toGraph( g, comp ), pw );
				}
			}
		}

	}

	public static void decomposeComponent(LabeledGraph g, PrintWriter pw)
	{
		if (g.n <= 3) {
			return;
		}

		ArrayList< XBitSet > components = g.decomposeByCliqueSeparators();

		if (components.size() == 1) {
			decomposeReduced( g, pw );
		} else {
			LabeledGraph[] graphs = toGraphs( g, components );
			for (LabeledGraph sg: graphs) {
				decomposeReduced( sg, pw );
			}
		}
	}

	public static void decomposeReduced(LabeledGraph g, PrintWriter pw)
	{
		if (g.isChordal()) {
			return;
		}

		pw.println("===================");
		pw.println("vertices = " + g.n + ", edges = " + g.edges());
		pw.println(g);
		//		fillEdges.addAll( td.computeFill( g ) );\
	}

	private static XBitSet safeSeparator(LabeledGraph g)
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

		for (int u = 0; u < g.n; u++) {
			ArrayList< XBitSet > components = g.getComponents( g.neighborSet[ u ] );
			L: for (XBitSet comp: components) {
				XBitSet separator = g.neighborSet( comp );
				if (separator.size() == 4) {
					if (g.countFill( separator ) != 2) {
						continue;
					}
					for (int v = separator.nextSetBit( 0 ); v >= 0; v = separator.nextSetBit( v + 1 )) {
						if (g.neighborSet[ v ].intersectWith( separator ).cardinality() != 2) {
							continue L;
						}
					}
					// separator induces an induced C_4
					for (int v = separator.nextSetBit( 0 ); v >= 0; v = separator.nextSetBit( v + 1 )) {
						for (int w = separator.nextSetBit( v + 1 ); w >= 0; w = separator.nextSetBit( w + 1 )) {
							if (g.areAdjacent( v, w )) {
								continue;
							}
							g.addEdge( v, w );
							if (isSafe(g, comp, u) == false) {
								g.removeEdge( v, w );
								continue L;
							}
							g.removeEdge( v, w );
						}
					}
					// every minimal fill-in of G[separator] is guard-safe
					System.out.println("detect");
					safeFilling( g, separator );
					return separator;
				}
			}
		}

		return null;
	}

	private static boolean isSafe(LabeledGraph g, XBitSet A, int b)
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

	private static XBitSet visit(LabeledGraph g, XBitSet rest, int from, int to)
	{
		IntQueue que = new IntQueue( rest.cardinality() + 2 );
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

	private static void safeFilling(LabeledGraph g, XBitSet separator) {
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
					missing--;
					if (missing <= 0) {
						return;
					}
				}
			}
		}

		System.out.println(missing + " edges are safely filled.");
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


	private static LabeledGraph[] toGraphs(LabeledGraph g, ArrayList< XBitSet > comps)
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

	private static LabeledGraph toGraph(LabeledGraph g, XBitSet comp)
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
		for (int tc = 1; tc <= 100; tc++) {
			System.out.println(tc);
			LabeledGraph g = Instance.read("instances/" + tc + ".graph");
			if (g.n > 1000) continue;
			PrintWriter pw = new PrintWriter( new File("graphs/" + tc + ".graphs"));	
			decompose(g, pw);
			pw.close();
		}
	}
}
