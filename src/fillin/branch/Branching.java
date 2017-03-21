package fillin.branch;


import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import fillin.branch.Vertex;
import fillin.branch.VertexSet;
import fillin.main.Instance;

public class Branching {
	
	private Vertex[] graph;
	private VertexSet whole;
	private int N;
	private Stack< Vertex[] > fillEdge;
	private boolean[][] forbid;
	
	public Branching(Vertex[] graph)
	{
		this.graph = graph;
		this.N = graph.length;
		fillEdge = new Stack<>();
		whole = new VertexSet();
		for (Vertex v: graph) {
			whole.add( v );
		}
	}
	
	public String[][] edges;
	
	public boolean solve(int k)
	{
		forbid = new boolean[ N ][ N ];
		if (branch( whole, k )) {
			System.out.println( "==fill edges==" );
			edges = new String[ k ][];
			int kk = 0;
			while (fillEdge.isEmpty() == false) {
				Vertex[] edge = fillEdge.pop();
				edges[ kk++ ] = new String[]{ edge[ 0 ].label, edge[ 1 ].label };
				System.out.println( edge[ 0 ].label + " " + edge[ 1 ].label );
				fill( edge[ 0 ], edge[ 1 ] );
			}
			
			if (isChordal( whole )) {
				System.out.println( "filled graph is chordal" );
			} else {
				System.out.println( "filled graph is not chordal");
			}
			
			return true;
		}
		return false;
	}
	
	private boolean branch(VertexSet G, int k)
	{
		// extract unmarked simplicial vertices
		VertexSet simplicial = extractSimplicial( G ); 
		if (simplicial.isEmpty() == false) {
			return branch( G.deleteImut( simplicial ), k );
		}
		
		Vertex[] cycle = findChordlessCycle( G );
		if (k >= 0 && cycle.length == 0) {
			return true;
		}
		if (k <= 0) {
			return false;
		}

		int len = cycle.length;
		int[][] rec = new int[ len ][];
		int n_rec = 0;
		// there are two branches: adding (c0, c2) or (c1, c3)
		if (forbid[ cycle[ 1 ].id ][ cycle[ len - 1 ].id ] == false) {
			if (branch_sub( G, k, cycle[ 1 ], cycle[ len - 1 ] )) {
				return true;
			}
			forbid[ cycle[ 1 ].id ][ cycle[ len - 1 ].id ] = true;
			forbid[ cycle[ len - 1 ].id ][ cycle[ 1 ].id ] = true;
			rec[ n_rec++ ] = new int[]{ cycle[ 1 ].id, cycle[ len - 1 ].id };
		}
		for (int i = 2; i < len - 1; i++) {
			if (forbid[ cycle[ 0 ].id ][ cycle[ i ].id ] == false) {
				if (branch_sub( G, k, cycle[ 0 ], cycle[ i ] )) {
					return true;
				}
				forbid[ cycle[ 0 ].id ][ cycle[ i ].id ] = true;
				forbid[ cycle[ i ].id ][ cycle[ 0 ].id ] = true;
				rec[ n_rec++ ] = new int[]{ cycle[ 0 ].id, cycle[ i ].id };
			}
		}
		
		for (int i = 0; i < n_rec; i++) {
			forbid[ rec[ i ][ 0 ] ][ rec[ i ][ 1 ]] = false;
			forbid[ rec[ i ][ 1 ] ][ rec[ i ][ 0 ]] = false;
		}
		
		return false;
	}
	
	public Vertex[] findChordlessCycle(VertexSet B)
	{
		if (B.size() <= 3) {
			return new Vertex[ 0 ];
		}
		return mcs( B );
	}
	
	public Vertex[] mcs(VertexSet B)
	{
		int SN = B.size();
		int[] weight = new int[ N ];
		boolean[] used = new boolean[ N ];
		int[] order = new int[ SN ];
		int[] alpha = new int[ N ];
		VertexSet[] bucket = new VertexSet[ SN ];
		VertexSet[] NB = new VertexSet[ N ];
		
		bucket[ 0 ] = B.copy();
		for (int i = 1; i < SN; i++) {
			bucket[ i ] = new VertexSet();
		}
		for (int v = B.firstElement(); v >= 0; v = B.nextElement( v )) {
			NB[ v ] = graph[ v ].adj.intersectImut( B );
		}
		int pt = 0;
		for (int i = SN - 1; i >= 0; i--) {
			while (bucket[ pt ].isEmpty()) {
				pt--;
			}
			Vertex v = graph[ bucket[ pt ].firstElement() ];
			bucket[ pt ].remove( v );
			if (used[ v.id ] == true) {
				continue;
			}
			used[ v.id ] = true;
			alpha[ v.id ] = i;
			order[ i ] = v.id;
			for (int w = NB[ v.id ].firstElement(); w >= 0; w = NB[ v.id ].nextElement( w )) {
				if (used[ w ] == true) { // w has been already in order[]
					continue;
				}
				bucket[ weight[ w ] ].remove( graph[ w ] );
				bucket[ ++weight[ w ] ].add( graph[ w ] );
			}
			pt++;
		}
		
		// zero fill-in test
		boolean isChordal = true;
		Vertex[] follower = new Vertex[ N ];
		int[] index = new int[ N ];
		for (int i = 0; i < SN; i++) {
			Vertex w = graph[ order[ i ] ];
			follower[ w.id ] = w;
			index[ w.id ] = i;
			for (int v = NB[ w.id ].firstElement(); v >= 0; v = NB[ w.id ].nextElement( v )) {
				if (alpha[ v ] < i) {
					index[ v ] = i;
					if (follower[ v ] == graph[ v ]) {
						follower[ v ] = w;
					}
				}
			}
			for (int v = NB[ w.id ].firstElement(); isChordal && v >= 0; v = NB[ w.id ].nextElement( v )) {
				if (alpha[ v ] < i) {
					if (index[ follower[ v ].id ] < i) {
						isChordal = false;
						break;
					}
				}
			}
		}
		
		if (isChordal == true) {
			return new Vertex[ 0 ];
		}
		
		Vertex maxu = null;
		L: for (int i = SN - 1; i >= 0; i--) {
			Vertex u = graph[ order[ i ] ];
			for (int w = NB[ u.id ].firstElement(); w >= 0; w = NB[ u.id ].nextElement( w )) {
				if (alpha[ w ] > i && 
						follower[ u.id ] != graph[ w ] && 
						follower[ u.id ].isAdjacent( graph[ w ] ) == false) {
					// (u, follower[ u.id ], w ) is a violating triple.
					maxu = u;
					break L;
				}
			}
		}
		
		Vertex maxv = null;
		Vertex maxw = null;
		for (int v = NB[ maxu.id ].firstElement(); v >= 0; v = NB[ maxu.id ].nextElement( v )) {
			VertexSet wS = NB[ maxu.id ].deleteImut( NB[ v ] );
			for (int w = wS.firstElement(); w >= 0; w = wS.nextElement( w )) {
				if (alpha[ w ] > alpha[ v ] && alpha[ v ] > alpha[ maxu.id ]) {
					if (maxv == null) {
						maxv = graph[ v ];
						maxw = graph[ w ];
					} else if (alpha[ maxv.id ] < alpha[ v ]) {
						maxv = graph[ v ];
						maxw = graph[ w ];
					} else if (alpha[ maxv.id ] == alpha[ v ] && alpha[ maxw.id ] < alpha[ w ]) {
						maxw = graph[ w ];
					}
				}
			}
		}
		
		VertexSet avoid = NB[ maxu.id ].addImut( maxu ).remove( maxv ).remove( maxw );
		Vertex[] pred = new Vertex[ N ];
		int[] dist = new int[ N ];
		Queue< Vertex > que = new LinkedList<>();
		que.offer( maxv );
		pred[ maxv.id ] = maxv;
		while (que.isEmpty() == false) {
			Vertex v = que.poll();
			if (v == maxw) {
				// a chordless path is found
				Vertex[] cycle = new Vertex[ dist[ v.id ] + 2 ];
				int k = 2;
				cycle[ 0 ] = maxu;
				cycle[ 1 ] = maxv;
				while (pred[ v.id ] != maxv) {
					v = pred[ v.id ];
					cycle[ k++ ] = v;
				}
				cycle[ k++ ] = maxw; 
				
				return cycle;
			}
			for (int w = NB[ v.id ].firstElement(); w >= 0; w = NB[ v.id ].nextElement( w )) {
				if (pred[ w ] == null && avoid.contains( graph[ w ] ) == false) {
					pred[ w ] = v;
					dist[ w ] = dist[ v.id ] + 1;
					que.offer( graph[ w ] );
				}
			}
		}
		
		return new Vertex[ 0 ];
		
	}
	
	
	private boolean branch_sub(VertexSet G, int k, Vertex v, Vertex w)
	{
		fill( v, w );
		fillEdge.push( new Vertex[]{v, w} );
		if (branch( G, k - 1 )) {
			return true;
		}
		fillEdge.pop();
		remove( v, w );
		return false;
	}
	
	private void fill(Vertex v, Vertex w)
	{
		v.addEdge( w );
		w.addEdge( v );
	}
	
	private void remove(Vertex v, Vertex w)
	{
		v.removeEdge( w );
		w.removeEdge( v );
	}
		
	private VertexSet extractSimplicial(VertexSet G) 
	{
		VertexSet simplicial = new VertexSet();
		
		L: for (int v = G.firstElement(); v >= 0; v = G.nextElement( v )) {
			// decide if v is a simplicial vertex in G
			VertexSet C = graph[ v ].adj.intersectImut( G );
			int size = C.size();
			for (int w = C.firstElement(); w >= 0; w = C.nextElement( w )) {
				if ( graph[ w ].adj.intersectImut( C ).size() != size - 1) {
					continue L; // C is not a clique in G
				}
			}
			
			// C = N(v) is simplicial in G
			simplicial.add( graph[ v ] );
		}
		
		return simplicial;
	}

	private boolean isChordal(VertexSet G)
	{
		int SN = G.size();
		int[] weight = new int[ N ];
		boolean[] used = new boolean[ N ];
		int[] order = new int[ SN ];
		int[] alpha = new int[ N ];
		VertexSet[] bucket = new VertexSet[ SN ];
		VertexSet[] NG = new VertexSet[ N ];

		bucket[ 0 ] = G.copy();
		for (int i = 1; i < SN; i++) {
			bucket[ i ] = new VertexSet();
		}
		for (int v = G.firstElement(); v >= 0; v = G.nextElement( v )) {
			NG[ v ] = graph[ v ].adj.intersectImut( G );
		}
		int pt = 0;
		for (int i = SN - 1; i >= 0; i--) {
			while (bucket[ pt ].isEmpty()) {
				pt--;
			}
			Vertex v = graph[ bucket[ pt ].firstElement() ];
			bucket[ pt ].remove( v );
			if (used[ v.id ] == true) {
				continue;
			}
			used[ v.id ] = true;
			alpha[ v.id ] = i;
			order[ i ] = v.id;
			for (int w = NG[ v.id ].firstElement(); w >= 0; w = NG[ v.id ].nextElement( w )) {
				if (used[ w ] == true) { // w has been already in order[]
					continue;
				}
				bucket[ weight[ w ] ].remove( graph[ w ] );
				bucket[ ++weight[ w ] ].add( graph[ w ] );
			}
			pt++;
		}

		// zero fill-in test
		Vertex[] follower = new Vertex[ N ];
		int[] index = new int[ N ];
		for (int i = 0; i < SN; i++) {
			Vertex w = graph[ order[ i ] ];
			follower[ w.id ] = w;
			index[ w.id ] = i;
			for (int v = NG[ w.id ].firstElement(); v >= 0; v = NG[ w.id ].nextElement( v )) {
				if (alpha[ v ] < i) {
					index[ v ] = i;
					if (follower[ v ] == graph[ v ]) {
						follower[ v ] = w;
					}
				}
			}
			for (int v = NG[ w.id ].firstElement(); v >= 0; v = NG[ w.id ].nextElement( v )) {
				if (alpha[ v ] < i) {
					if (index[ follower[ v ].id ] < i) {
						return false;
					}
				}
			}
		}

		return true;
	}
	
	public static void main(String[] args) {
		Vertex[] g = Reader.read("instances/90.graph");
		Branching br = new Branching( g );
		for (int k = 0; k < 100; k++) {
			if (br.solve(k)) {
				System.out.println(k + " true");
				return;
			} else {
				System.out.println(k + " false");
			}
		}
	}

}
