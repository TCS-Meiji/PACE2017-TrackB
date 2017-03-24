package fillin.experiment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import fillin.main.Instance;
import tw.common.LabeledGraph;
import tw.common.XBitSet;

public class Decompose {
	
	static void decompose(LabeledGraph g, PrintWriter pw)
	{
		if (g.n <= 3) {
			return;
		}
		LabeledGraph[] graphs = toGraphs( g, g.getComponents( new XBitSet() ) );
		Arrays.stream( graphs ).forEach(sg -> decomposeConnected( sg, pw ) );
	}
	static void decomposeConnected(LabeledGraph g, PrintWriter pw)
	{
		if (g.n <= 3) {
			return;
		}
		LabeledGraph[] graphs = toGraphs( g, g.decomposeByCutPoints() );
		Arrays.stream( graphs ).forEach(sg -> decomposeBiconnected( sg, pw ));
	}
	static void decomposeBiconnected(LabeledGraph g, PrintWriter pw)
	{
		if (g.n <= 3) {
			return;
		}
		LabeledGraph[] graphs = toGraphs( g, g.decomposeByCliqueSeparators() );
		Arrays.stream( graphs ).forEach(sg -> pw.println("====================\n" + sg + "==") );
	}
	
	private static LabeledGraph[] toGraphs(LabeledGraph g, ArrayList< XBitSet > comps)
	{
		LabeledGraph[] graphs = new LabeledGraph[ comps.size() ];
		int k = 0;
		for (XBitSet comp: comps) {
			graphs[ k++ ] = toGraph( g, comp );
		}
		return graphs;
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
			if (g.n <= 4000 || g.n > 10000) continue;
			PrintWriter pw = new PrintWriter( new File("graphs/" + tc + ".graphs"));	
			decompose(g, pw);
			pw.close();
		}
	}
}
