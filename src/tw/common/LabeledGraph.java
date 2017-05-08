package tw.common;

import java.util.ArrayList;
import java.util.Arrays;

public class LabeledGraph extends Graph {
	
	private String[] labels;
	private boolean[] visited;
	
	public LabeledGraph(String[] labels)
	{
		super( labels.length );
		Arrays.sort( labels );
		this.labels = labels;
	}
	
	public int edges()
	{
		int sum = 0;
		for (int d: degree) {
			sum += d;
		}
		return sum / 2;
	}

	public void addEdgeBetween(String u, String v)
	{
		int uid = Arrays.binarySearch( labels, u );
		int vid = Arrays.binarySearch( labels, v );
		super.addEdge( uid, vid );
	}
	
	public final String getLabel(final int v)
	{
		return labels[ v ];
	}
	
	private int[] num;
	private int[] low;
	private int time;
	private ArrayList< XBitSet > components;
	private IntStack st;
	
	public ArrayList< XBitSet > decomposeByCutPoints()
	{
		components = new ArrayList< >();
		num = new int[ n ];
		low = new int[ n ];
		st = new IntStack( n );
		
		for (int u = 0; u < n; u++) {
			if (num[ u ] == 0) {
				time = 0;
				visitForBiComponent( -1, u );
			}
		}
		
		return components;
	}
	
	private void visitForBiComponent(int p, int u)
	{
		low[ u ] = num[ u ] = ++time;
		st.push( u );
		for (int v: neighbor[ u ]) {
			if (num[ v ] == 0) {
				visitForBiComponent( u, v );
				low[ u ] = Math.min( low[ u ], low[ v ] );
				if (low[ v ] >= num[ u ]) {
					XBitSet bcomp = new XBitSet( n );
					bcomp.set( u );
					while (true) {
						int w = st.pop();
						bcomp.set( w );
						if (w == v) {
							break;
						}
					}
					components.add( bcomp );
				}
			} else {
				low[ u ] = Math.min( low[ u ], num[ v ] );
			}
		}
	}

	/**
	 * The method computes a clique separator decomposition of this graph.
	 * This method returns an ArrayList whose elements of the form { XBitSet1, XBitSet2 }
	 * where XBitSet2 is a clique separator and XBitSet1 is the component associated to XBitSet2.
	 * This method implements an O(nm) time algorithm given by [Tarjan 85]
	 */
	public ArrayList< XBitSet > decomposeByCliqueSeparators()
	{
		ArrayList< XBitSet > components = new ArrayList< XBitSet >();
		XBitSet[] filledGraph = mcs_m();
		XBitSet B = (XBitSet)all.clone();
		XBitSet high = (XBitSet)all.clone();
		
		for (int i = 0; i < n; i++) {
			int v = alpha[ i ];
			if (B.get( v ) == false) {
				continue;
			}
			high.clear( v );
			XBitSet C = filledGraph[ v ].intersectWith( B ).intersectWith( high );
			if (isClique( C )) {
				visited = new boolean[ n ];
				for (int u = C.nextSetBit( 0 ); u >= 0; u = C.nextSetBit( u + 1 )) {
					visited[ u ] = true;
				}
				XBitSet A = separate( v, B, new XBitSet( n ) );
				XBitSet nB = B.subtract( C ).subtract( A );
				if (nB.isEmpty() == false) {
					components.add( A.unionWith( C ) );
					B = B.subtract( A );
				}
			}
		}

		if (B.isEmpty() == false) {
			components.add( B );
		}
		
		return components;
	}

	private XBitSet separate(int v, XBitSet subgraph, XBitSet comp)
	{
		visited[ v ] = true;
		comp.set( v );
		for (int w: neighbor[ v ]) {
			if (visited[ w ] == false && subgraph.get( w )) {
				separate( w, subgraph, comp );
			}
		}
		return comp;
		
	}

	public boolean isClique(XBitSet C)
	{
		for (int u = C.nextSetBit( 0 ); u >= 0; u = C.nextSetBit( u + 1 )) {
			C.clear( u );
			if (C.isSubset( neighborSet[ u ] ) == false) {
				C.set( u );
				return false;
			}
			C.set( u );
		}
		return true;
	}

	/**
	 * computes a minimal fill-in by MCS-M [Berry et al. 2002]
	 */
	private int[] alpha;
	private XBitSet[] mcs_m()
	{
		XBitSet[] filledGraph = new XBitSet[ n ];
		for (int i = 0; i < n; i++) {
			filledGraph[ i ] = (XBitSet)neighborSet[ i ].clone();
		}
		int[] weight = new int[ n ];
		alpha = new int[ n ];
		XBitSet[] bucket = new XBitSet[ n ];
		XBitSet unnumbered = (XBitSet)all.clone();
		bucket[ 0 ] = (XBitSet)all.clone();
		for (int i = 1; i < n; i++) {
			bucket[ i ] = new XBitSet( n );
		}
		
		int pt = 0;
		for (int i = n - 1; i >= 0; i--) {
			while (bucket[ pt ].cardinality() == 0) {
				pt--;
			}
			
			int z = bucket[ pt ].nextSetBit( 0 );
			bucket[ pt ].clear( z );
			alpha[ i ] = z;
			unnumbered.clear( z );
			
			UnionFind uf = new UnionFind( n );
			XBitSet replace = new XBitSet( n );
			XBitSet smallWeight = new XBitSet( n );
			smallWeight.set( z );
			for (int j = 0; j <= pt; j++) {
				for (int y = bucket[ j ].nextSetBit( 0 ); y >= 0; y = bucket[ j ].nextSetBit( y + 1 )) {
					for (int x: neighbor[ y ]) {
						if (smallWeight.get( x ) && uf.find( x, z )) {
							replace.set( y );
						}
					}
				}
				
				smallWeight.or( bucket[ j ] );
				
				for (int y = bucket[ j ].nextSetBit( 0 ); y >= 0; y = bucket[ j ].nextSetBit( y + 1 )) {
					for (int x: neighbor[ y ]) {
						if (smallWeight.get( x )) {
							uf.union( y, x );
						}
					}
				}
			}
			
			for (int y = replace.nextSetBit( 0 ); y >= 0; y = replace.nextSetBit( y + 1 ) ) {
				bucket[ weight[ y ] ].clear( y );
				bucket[ ++weight[ y ] ].set( y );
			}
			pt++;
		}
		
		XBitSet prefix = new XBitSet( n );
		for (int i = 0; i < n; i++) {
			int v = alpha[ i ];
			prefix.set( v );
			visited = new boolean[ n ];
			XBitSet reachable = dfs( v, prefix, new XBitSet( n ) );
			for (int w = reachable.nextSetBit( 0 ); w >= 0; w = reachable.nextSetBit( w + 1 )) {
				filledGraph[ v ].set( w );
				filledGraph[ w ].set( v );
			}
		}
		
		return filledGraph;
	}

	private XBitSet dfs(int v, XBitSet prefix, XBitSet res)
	{
		visited[ v ] = true;
		for (int w: neighbor[ v ]) {
			if (visited[ w ]) {
				continue;
			}
			if (prefix.get( w )) {
				res = dfs( w, prefix, res );
			} else {
				res.set( w );
			}
		}
		return res;
	}
	
	/**
	 *  The method decides whether this labeled graph is chordal or not.
	 *  This method implements a linear time algorithm given by [Tarjan and Yannakakis 84].
	 */
	public boolean isChordal()
	{
		
		if (n <= 3) {
			return true;
		}
		
		int[] weight = new int[ n ];
		int[] order = new int[ n ];
		int[] alpha = new int[ n ];
		boolean[] used = new boolean[ n ];
		
		XBitSet[] bucket = new XBitSet[ n ];
		bucket[ 0 ] = (XBitSet)all.clone();
		for (int i = 1; i < n; i++) {
			bucket[ i ] = new XBitSet( n );
		}
		
		int pt = 0;
		for (int i = n - 1; i >= 0; i--) {
			while (bucket[ pt ].isEmpty()) {
				pt--;
			}
			
			int v = bucket[ pt ].nextSetBit( 0 );
			bucket[ pt ].clear( v );
			used[ v ] = true;
			alpha[ v ] = i;
			order[ i ] = v;
			for (int w: neighbor[ v ]) {
				if (used[ w ]) {
					continue;
				}
				bucket[ weight[ w ] ].clear( w );
				bucket[ ++weight[ w ] ].set( w );
			}
			pt++;
		}
		
		int[] follower = new int[ n ];
		int[] index = new int[ n ];
		for (int i = 0; i < n; i++) {
			int v = order[ i ];
			follower[ v ] = v;
			index[ v ] = i;
			for (int w: neighbor[ v ]) {
				if (alpha[ w ] < i) {
					index[ w ] = i;
					if (follower[ w ] == w) {
						follower[ w ] = v;
					}
				}
			}
			for (int w: neighbor[ v ]) {
				if (alpha[ w ] < i) {
					if (index[ follower[ w ] ] < i) {
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	public int countFill(XBitSet S)
	{
		int size = S.cardinality();
		int deg_sum = 0;
		for (int u = S.nextSetBit( 0 ); u >= 0; u = S.nextSetBit( u + 1 )) {
			deg_sum += S.intersectWith( neighborSet[ u ] ).cardinality();
		}
		return (size * (size - 1) - deg_sum) / 2;
	}
	
	public int countFill(XBitSet X, XBitSet Y)
	{
		int deg_sum = 0; 
		for (int x = X.nextSetBit( 0 ); x >= 0; x = X.nextSetBit( x + 1 )) {
			deg_sum += Y.intersectWith( neighborSet[ x ] ).cardinality();
		}
		return X.cardinality() * Y.cardinality() - deg_sum;
	}
}
