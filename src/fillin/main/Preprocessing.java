package fillin.main;

import java.util.ArrayList;

import tw.common.LabeledGraph;
import tw.common.XBitSet;

public class Preprocessing {
	
//	private static final boolean DEBUG = true;
	private static final boolean DEBUG = false;
	
	public static int fillSafeFillEdge(LabeledGraph g, int SIZE, int K)
	{
		int cnt = 0;
		for (int u = 0; u < g.n; u++) {
			if (g.degree[ u ] > SIZE) {
				continue;
			}
			int D = g.degree[ u ];
			for (int k = 2; k <= K; k++) {
				for (int S = KSubSet.initKSubset( k ); KSubSet.hasNext( S , D ); S = KSubSet.nextKSubset( S )) {
					XBitSet sep = new XBitSet( g.n );
					for (int i = 0; i < D; i++) {
						if (((S >> i) & 1) == 1) {
							sep.set( g.neighbor[ u ][ i ] );
						}
					}
					if (g.countFill( sep ) != 1) {
						continue;
					}
					if (isMinimalSeparator( g, sep )) {
						L: for (int v = sep.nextSetBit( 0 ); v >= 0; v = sep.nextSetBit( v + 1 )) {
							for (int w = sep.nextSetBit( v + 1 ); w >= 0; w = sep.nextSetBit( w + 1 )) {
								if (g.areAdjacent( v, w ) == false) {
									if (DEBUG) {
										System.out.println("add" + "(" + g.getLabel( v ) + ", " + g.getLabel( w ) + "): " + sep.cardinality());
									}
									g.addEdge( v, w );
									cnt++;
									break L;
								}
							}
						}
					}
				}
			}
		}
		if (cnt > 0) {
			return cnt + fillSafeFillEdge( g, SIZE, K );
		} else {
			return cnt;
		}
	}
	
	static class KSubSet
	{	
		private KSubSet(){ }
		
		static int nextKSubset(int S)
		{
			int x = S & -S;
			int y = S + x;
			return ((S & ~y) / x >> 1) | y;
		}
		
		static int initKSubset(int k)
		{
			return (1 << k) - 1;
		}
		
		static boolean hasNext(int S, int N)
		{
			return S < 1 << N;
		}
	}
	
	private static boolean isSeparator(LabeledGraph g, XBitSet sep)
	{
		if (g.getFullComponents(sep).size() >= 2) {
			return true;
		} else {
			return false;
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
	
	private static boolean isMinimalSeparator(LabeledGraph g, XBitSet sep, ArrayList<XBitSet> comps)
	{
		if (g.getFullComponents(sep, comps).size() >= 2) {
			return true;
		} else {
			return false;
		}
	}
}