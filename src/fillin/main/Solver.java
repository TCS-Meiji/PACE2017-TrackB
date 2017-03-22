package fillin.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import tw.common.LabeledGraph;
import tw.common.Pair;
import tw.common.XBitSet;

public class Solver {

	HashSet< Pair<String, String> > fillEdges;
	int safe = 0;
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
			res += solveBiConnected( sg, sg.n, 2 );
		}
		
		return res;
	}
	
	public int solveBiConnected(LabeledGraph g, int N, int sep)
	{
		if (g.n <= 3) {
			return 0;
		}
		
		int fill = Preprocessing.fillSafeFillEdge( g, N, sep );
		Preprocessing.getSafeFillEdges().forEach(e -> this.fillEdges.add( e ));
		safe += fill;
		ArrayList< XBitSet > components = g.decomposeByCliqueSeparators();
		
		if (components.size() == 1) {
			return solveComponent( g );
		}

		LabeledGraph[] graphs = toGraphs( g, components );
		int res = 0;
		for (LabeledGraph sg: graphs) {
			res += solveBiConnected( sg, sg.n, sep + 2 );
		}
		return res + fill;
	}
	
	private XBitSet reduceSimplicial(LabeledGraph g, XBitSet compo) {
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
	
	public int solveComponent(LabeledGraph g)
	{
		if (g.isChordal()) {
			return 0;
		}

		Decomposer dec = new Decomposer( g );
		fillEdges.addAll( dec.decompose().computeFill( g ) );
		return dec.getOpt();
	}	
	
	public static void main(String[] args) throws FileNotFoundException {
		Solver solver = new Solver();
		solver.solve( Instance.read("instances/2.graph") );
		solver.fillEdges.forEach(e -> System.out.println(e.first + " " + e.second));
		System.out.println(solver.fillEdges.size());
	}
}
