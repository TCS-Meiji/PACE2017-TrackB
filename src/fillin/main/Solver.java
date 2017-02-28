package fillin.main;

import java.util.ArrayList;
import java.util.HashSet;

import tw.common.LabeledGraph;
import tw.common.Pair;
import tw.common.XBitSet;

public class Solver {

	HashSet< Pair<String, String> > fillEdges;
	
	public Solver() {
		fillEdges = new HashSet<>();
	}
	public int solve(LabeledGraph g)
	{
		ArrayList< XBitSet > components = g.getComponents( new XBitSet() );
		
		int nc = components.size();
		if (nc == 1) {
			return solveConnected( g );
		}
		
		LabeledGraph[] graphs = new LabeledGraph[ nc ];
		for (int i = 0; i < nc; i++) {
			XBitSet compo = components.get( i );
			int nv = compo.cardinality();
			String[] labelArray = new String[ nv ];
			int k = 0;
			int[] conv = new int[ g.n ];
			for (int v = 0; v < g.n; v++) {
				if (compo.get(v)) {
					conv[ v ] = k;
					labelArray[ k++ ] = g.getLabel( v );
				}
			}
			graphs[ i ] = new LabeledGraph( labelArray );
			for (int u = compo.nextSetBit( 0 ); u >= 0; u = compo.nextSetBit( u + 1 )) {
				for (int v = compo.nextSetBit( u + 1 ); v >= 0; v = compo.nextSetBit( v + 1 )) {
					if (g.areAdjacent( u , v )) {
						graphs[ i ].addEdge( conv[ u ], conv[ v ] );
					}
				}
			}
		}
		
		int res = 0;
		for (LabeledGraph sg: graphs) {
			res += solveConnected( sg );
		}
		return res;
	}
	
	public int solveConnected(LabeledGraph g)
	{
		ArrayList< XBitSet[] > components = g.decomposeByCliqueSeparators();
		
		int nc = components.size();
		if (nc == 1) {
			return solveComponent( g );
		}
		
		LabeledGraph[] graphs = new LabeledGraph[ nc ];
		for (int i = 0; i < nc; i++) {
			XBitSet compo = components.get( i )[ 0 ].unionWith( components.get( i )[ 1 ] );
			int nv = compo.cardinality();
			String[] labelArray = new String[ nv ];
			int k = 0;
			int[] conv = new int[ g.n ];
			for (int v = 0; v < g.n; v++) {
				if (compo.get(v)) {
					conv[ v ] = k;
					labelArray[ k++ ] = g.getLabel( v );
				}
			}
			graphs[ i ] = new LabeledGraph( labelArray );
			for (int u = compo.nextSetBit( 0 ); u >= 0; u = compo.nextSetBit( u + 1 )) {
				for (int v = compo.nextSetBit( u + 1 ); v >= 0; v = compo.nextSetBit( v + 1 )) {
					if (g.areAdjacent( u , v )) {
						graphs[ i ].addEdge( conv[ u ], conv[ v ] );
					}
				}
			}
		}
		
		int res = 0;
		for (LabeledGraph sg: graphs) {
			res += solveComponent( sg );
		}
		return res;
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
	
	public static void main(String[] args) {
		Solver solver = new Solver();
		long start = System.currentTimeMillis();
		int sol = solver.solve( Instance.read( args[ 0 ] ) );
//		int sol = solver.solveComponent( Instance.read( ) );
		long end = System.currentTimeMillis();
		System.out.println("#name = " + args[ 0 ]  + ", sol = " +  sol + ", time = " + (end - start) + " msec" );
		//		for (Pair<String, String> e: solver.fillEdges) {
		//	System.out.println(e.first + " " + e.second);
		//}
	}
}
