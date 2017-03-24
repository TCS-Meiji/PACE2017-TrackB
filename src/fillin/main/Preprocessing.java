package fillin.main;

import java.util.ArrayList;
import java.util.HashSet;

import tw.common.LabeledGraph;
import tw.common.Pair;
import tw.common.XBitSet;

public class Preprocessing {
	
//	private static final boolean DEBUG = true;
	private static final boolean DEBUG = false;
	private static HashSet<Pair<String, String>> safeFill = new HashSet<>();
	
	public static HashSet<Pair<String, String>> getSafeFillEdges()
	{
		return safeFill;
	}
	
	public static int fillSafeFillEdges(LabeledGraph g)
	{
		int cnt = 0;
		
		for (int u = 0; u < g.n; u++) {
			// find a minimal separator S \subseteq N(u) with F(S) = 1
			ArrayList< XBitSet > components = g.getComponents( g.neighborSet[ u ] );
			for (XBitSet comp: components) {
				if (comp.get( u )) {
					continue;
				}
				XBitSet sep = g.neighborSet( comp );
				if (g.countFill( sep ) != 1) {
					continue;
				}
				// add the missing edge in sep
				for (int v = sep.nextSetBit( 0 ); v >= 0; v = sep.nextSetBit( v + 1 )) {
					for (int w = sep.nextSetBit( v + 1 ); w >= 0; w = sep.nextSetBit( w + 1 )) {
						if (g.areAdjacent( v , w ) == false) {
							g.addEdge(v, w);
							String vl = g.getLabel( v );
							String wl = g.getLabel( w );
							if (vl.compareTo( wl ) < 0) { 
								safeFill.add(new Pair< String, String >( vl, wl ));
							} else {
								safeFill.add(new Pair< String, String >( wl, vl ));
							}
							cnt++;
						}
					}
				}
			}
		}
		if (cnt > 0) {
			return cnt + fillSafeFillEdges( g ); 
		} else {
			return 0;
		}
	}
	
	private static boolean isMinimalSeparator(LabeledGraph g, XBitSet sep)
	{
		if (g.getFullComponents( sep ).size() >= 2) {
			return true;
		} else {
			return false;
		}
	}
	
	public static XBitSet reduceSimplicial(LabeledGraph g, XBitSet compo) {
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

}