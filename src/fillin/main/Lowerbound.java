package fillin.main;

import java.util.Arrays;
import java.util.LinkedList;

import tw.common.LabeledGraph;
import tw.common.Pair;
import tw.common.XBitSet;

public class Lowerbound {

	private static final boolean DEBUG = true;
//	private static final boolean DEBUG = false;
	
	LabeledGraph g;
	XBitSet whole;
	int lowerbound;
	
	public Lowerbound(LabeledGraph g)
	{
		this.g = g;
	}
	
	public int get()
	{
		whole = new XBitSet( g.n );
		for (int u = 0; u < g.n; u++) {
			whole.set( u );
		}
		return get( whole );
	}
	
	public int get(XBitSet S)
	{
		lowerbound = 0;
		whole = S;
		XBitSet SS = procedure1( S );
		procedure2( SS );
		return lowerbound;
	}
	
	public int get(XBitSet component, XBitSet separator)
	{
		LinkedList<Pair<Integer, Integer>> fill = new LinkedList<>();
		
		for (int u = separator.nextSetBit( 0 ); u >= 0; u = separator.nextSetBit( u + 1 )) {
			for (int v = separator.nextSetBit( u + 1 ); v >= 0; v = separator.nextSetBit( v + 1 )) {
				if (g.areAdjacent(u, v) == false) {
					g.addEdge(u, v);
					fill.add(new Pair<>(u, v));
				}
			}
		}
		int res = get(component.unionWith( separator ));
		fill.forEach(e -> g.removeEdge( e.first, e.second ));
		
		return res;
	}
	
	private XBitSet procedure1(XBitSet B)
	{
		XBitSet res = (XBitSet)B.clone();
		while (true) {
			int[] cycle = findChordlessCycle( res );
			if (cycle.length == 0) {
				break;
			}
			if (DEBUG) {
				System.out.println( "detect a chordless cycles of type 1: " 
						+ Arrays.toString( cycle ) );
			}
			Arrays.stream( cycle ).forEach(v -> res.clear( v ));
			lowerbound += cycle.length - 3;
		}
		
		return res;
	}

	private static final int NOT_VISITED = -1;
	private static final int AVOID = -2;
	private int[] findChordlessCycle(XBitSet B)
	{
		int N = B.cardinality();
		if (N < 4) {
			return new int[ 0 ];
		}
		int[] weight = new int[ g.n ];
		boolean[] used = new boolean[ g.n ];
		int[] order = new int[ N ];
		int[] alpha = new int[ g.n ];
		XBitSet[] bucket = new XBitSet[ N ];
		
		bucket[ 0 ] = (XBitSet)B.clone();
		for (int i = 1; i < N; i++) {
			bucket[ i ] = new XBitSet( g.n );
		}
		
		int pt = 0;
		for (int i = N - 1; i >= 0; i--) {
			while (bucket[ pt ].isEmpty()) {
				pt--;
			}
			
			int v = bucket[ pt ].nextSetBit( 0 );
			bucket[ pt ].clear( v );
			if (used[ v ]) {
				continue;
			}
			used[ v ] = true;
			alpha[ v ] = i;
			order[ i ] = v;
			for (int w: g.neighbor[ v ]) {
				if (used[ w ] || B.get( w ) == false) {
					continue;
				}
				bucket[ weight[ w ] ].clear( w );
				bucket[ ++weight[ w ] ].set( w );
			}
			pt++;
		}
		
		boolean isChordal = true;
		int[] follower = new int[ g.n ];
		int[] index = new int[ g.n ];
		for (int i = 0; i < N; i++) {
			int w = order[ i ];
			follower[ w ] = w;
			index[ w ] = i;
			for (int v: g.neighbor[ w ]) {
				if (B.get( v )) {
					if (alpha[ v ] < i) {
						index[ v ] = i;
						if (follower[ v ] == v) {
							follower[ v ] = w;
						}
					}
				}
			}
			for (int v: g.neighbor[ w ]) {
				if (alpha[ v ] < i && B.get(v)) {
					if (index[ follower[ v ] ] < i) {
						isChordal = false;
					}
				}
			}
		}

		if (isChordal) {
			return new int[ 0 ];
		}
		
		int maxu = -1;
		L: for (int i = N - 1; i >= 0; i--) {
			int u = order[ i ];
			for (int w: g.neighbor[ u ]) {
				if (B.get( w ) == false) {
					continue;
				}
				if (alpha[ w ] > i && follower[ u ] != w &&
						g.areAdjacent( follower[ u ] , w ) == false) {
					maxu = u;
					break L;
				}
			}
		}
		
		int maxv = -1;
		int maxw = -1;
		for (int v: g.neighbor[ maxu ]) {
			if (B.get( v ) == false) {
				continue;
			}
			XBitSet C = B.intersectWith( g.neighborSet[ maxu ] ).subtract( g.neighborSet[ v ] );
			for (int w = C.nextSetBit( 0 ); w >= 0; w = C.nextSetBit( w + 1 ) ) {
				if (alpha[ w ] > alpha[ v ] && alpha[ v ] > alpha[ maxu ]) {
					if (maxv == -1) {
						maxv = v;
						maxw = w;
					} else if (alpha[ maxv ] < alpha[ v ]) {
						maxv = v;
						maxw = w;
					} else if (alpha[ maxv ] == alpha[ v ] && alpha[ maxw ] < alpha[ w ]) {
						maxw = w;
					}
				}
			}
		}
		
		int[] pred = new int[ g.n ];
		Arrays.fill( pred , NOT_VISITED );
		for (int v = 0; v < g.n; v++) {
			if (B.get( v ) == false) {
				pred[ v ] = AVOID;
			} else if (v != maxv && v != maxw && (v == maxu || g.neighborSet[ maxu ].get( v ))) {
				pred[ v ] = AVOID;
			}
		}
		IntQueue que = new IntQueue( N );
		que.offer( maxv );
		pred[ maxv ] = maxv;
		while (que.isEmpty() == false) {
			int v = que.poll();
			if (v == maxw) {
				// detect a chordless path between maxv and maxw
				int[] cycle = new int[ N + 2 ];
				cycle[ 0 ] = maxw;
				int k = 1;
				while (pred[ v ] != maxv) {
					v = pred[ v ];
					cycle[ k++ ] = v;
				}
				cycle[ k++ ] = maxv;
				cycle[ k++ ] = maxu;
				
				return Arrays.copyOf( cycle , k );
			}
			for (int w: g.neighbor[ v ]) {
				if (pred[ w ] == NOT_VISITED) {
					pred[ w ] = v;
					que.offer( w );
				}
			}
			
		}

		if (DEBUG) {
			System.out.println( "ERROR: there is an error in MCS." );
		}
		return new int[ 0 ];
	}
	
	private XBitSet procedure2(XBitSet B)
	{
		XBitSet res = (XBitSet)B.clone();
		while (true) {
			int[] cycle = findCrossingChordlessCycle( res );
			
			if (cycle.length == 0) {
				break;
			}
			
			if (DEBUG) {
				System.out.println("detect a chordless cycles of type 2: " + Arrays.toString( cycle ));
			}
			
			int start = 0;
			for (int i = 0; i < cycle.length; i++) {
				if (res.get( cycle[ i ] ) == false) {
					start = i;
					break;
				}
			}
			
			XBitSet path = new XBitSet( g.n );
			int[] path_len = new int[ cycle.length ];
			int n_path = 0;
			for (int i = 1; i <= cycle.length; i++) {
				if (res.get( cycle[ ( i + start ) % cycle.length ] )) {
					path.set( cycle[ ( i + start ) % cycle.length ] );
				} else {
					if (path.cardinality() > 1) {
						path_len[ n_path++ ] = path.cardinality() - 1;
						res.andNot( path );
					}
					path.clear();
				}
			}
			
			if (n_path == 1) {
				if (path_len[ 0 ] == cycle.length - 2) {
					lowerbound += path_len[ 0 ] - 1;
				} else {
					lowerbound += path_len[ 0 ];
				}
			} else if (n_path > 1) {
				int sum = 0;
				int max = 0;
				for (int i = 0; i < n_path; i++) {
					sum += path_len[ i ];
					max = Math.max( max, path_len[ i ] );
				}
				
				lowerbound += Math.max( max , sum / 2);
			} else {
				if (DEBUG) {
					throw new RuntimeException( "ERROR: there is at least one subpath of length at least one." );
				}
			}			
		} 
		return res;
	}
	
	/**
	 * @param B
	 * @return a chordless cycle in G containing at least
	 * two consecutive vertices from B.
	 */
	private int[] findCrossingChordlessCycle(XBitSet B)
	{
		XBitSet A = whole.subtract( B );
		for (int y = B.nextSetBit( 0 ); y >= 0; y = B.nextSetBit( y + 1 )) {
			for (int x: g.neighbor[ y ]) {
				if (A.get( x ) == false) {
					continue;
				}
				// (x, y) is an edge, where x \in G \setminus B and y \in B	
				XBitSet cNx = g.closedNeighborSet(x);
				XBitSet cNy = g.closedNeighborSet(y);
				XBitSet avoid = cNx.intersectWith( cNy ).unionWith(
						cNy.subtract( cNx ).intersectWith( A ));
				XBitSet from = cNy.subtract(cNx).intersectWith( B );
				XBitSet to = cNx.subtract( cNy );
				int[] path = findPath( from, to, avoid );
				if (path.length > 0) {
					int[] cycle = Arrays.copyOf( path, path.length + 2);
					cycle[ path.length ] = y;
					cycle[ path.length + 1 ] = x;
					
					return cycle;
				}
			}
		}
		return new int[ 0 ];
	}
	
	private int[] findPath(XBitSet from, XBitSet to, XBitSet avoid) {
		if (from.cardinality() == 0 || to.cardinality() == 0) {
			return new int[ 0 ];
		}
		
		IntQueue que = new IntQueue( whole.cardinality() );
		int[] pred = new int[ g.n ];
		Arrays.fill( pred, NOT_VISITED );
		for (int u = 0; u < g.n; u++) {
			if (whole.get( u ) == false || avoid.get( u )) {
				pred[ u ] = AVOID;
			}
		}
		
		for (int u = from.nextSetBit( 0 ); u >= 0; u = from.nextSetBit( u + 1 )) {
			que.offer( u );
			pred[ u ] = u;
		}
		while (que.isEmpty() == false) {
			int u = que.poll();
			for (int v: g.neighbor[ u ]) {
				if (pred[ v ] != NOT_VISITED) {
					continue;
				}
				pred[ v ] = u;
				que.offer( v );
				if (to.get( v )) {
					int[] path = new int[ whole.cardinality() ];
					int s = v;
					int k = 0;
					while (pred[ s ] != s){
						path[ k++ ] = s;
						s = pred[ s ];
					}
					path[ k++ ] = s;
					return Arrays.copyOf(path, k);
				}
			}
		}
		return new int[ 0 ];
	}
	
	class IntQueue {
		private final int[] queue;
		private int head, tail;
		
		IntQueue(int size) {
			head = tail = 0;
			queue = new int[ size ];
		}
		
		final int poll()
		{
			if (head == tail) {
				return 0;
			}
			int v = queue[ head++ ];
			if (head == queue.length) {
				head = 0;
			}
			return v;
		}
		
		final void offer(int e)
		{
			queue[ tail++ ] = e;
			if (tail == queue.length) {
				tail = 0;
			}
		}
		
		final boolean isEmpty()
		{
			return head == tail;
		}
	}

	


	public static void main(String[] args) {
		LabeledGraph g = Instance.read();
		Lowerbound lb = new Lowerbound( g );
		System.out.println(lb.get());
	}
}
