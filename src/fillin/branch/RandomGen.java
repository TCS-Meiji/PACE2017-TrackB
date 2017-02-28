package fillin.branch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class RandomGen {
	
	private static final boolean VERBOSE = true;
	
	public static Vertex[] generate(int N, int p)
	{
		return generate( N, p, new Random().nextLong() );
	}
	
	public static Vertex[] generate(int N, int p, long seed)
	{
		if (VERBOSE) {
			System.out.println( "Random seed: " + seed );
		}
		Vertex[] graph = new Vertex[ N ];
		for (int i = 0; i < N; i++) {
			graph[ i ] = new Vertex( i, i+"" );
		}
		
		Random rand = new Random( seed );
		for (int i = 0; i < N; i++) {
			for (int j = i + 1; j < N; j++) {
				if (rand.nextInt( 100 ) < p) {
					graph[ i ].addEdge( graph[ j ] );
					graph[ j ].addEdge( graph[ i ] );
				}
			}
		}
		return graph;
	}
	
	public static Vertex[] generateChordalMinusK(int N, int p, int k)
	{
		return generateChordalMinusK( N, p, k, new Random().nextLong() );
	}
	
	private static Vertex[] graph;
	public static Vertex[] generateChordalMinusK(int N, int p, int k, long seed)
	{
		List< Vertex > ls = new LinkedList<>();
		for (Vertex v: generate( N, p )) {
			ls.add( v );
		}
		
		Collections.shuffle( ls, new Random( seed ) );
		graph = ls.toArray( new Vertex[ N ] );
		VertexSet prefix = new VertexSet();
		for (Vertex v: graph) {
			prefix.addImut( v );
			VertexSet toClique = v.adj.delete( prefix );
			for (int w = toClique.firstElement(); w >= 0; w = toClique.nextElement( w )) {
				for (int x = toClique.nextElement( w ); x >= 0; x = toClique.nextElement( x )) {
					graph[ w ].addEdge( graph[ x ] );
					graph[ x ].addEdge( graph[ w ] );
				}
			}
		}
		
		List< Vertex[] > edgeList = new ArrayList<>();
		for (Vertex v: graph) {
			for (int w = v.adj.nextElement( v.id ); w >= 0; w = v.adj.nextElement( w )) {
				edgeList.add(new Vertex[]{ v, graph[ w ] });
			}
		}
//		
//		for (Vertex v: graph) {
//			for (int w = v.adj.nextElement( v.id ); w >= 0; w = v.adj.nextElement( w )) {
//				System.out.println( v.id + " " + w );
//			}
//		}
//		
		Collections.shuffle( edgeList, new Random( seed ) );
		for (int i = 0; i < k; i++) {
			Vertex[] edge = edgeList.get( i );
			edge[ 0 ].removeEdge( edge[ 1 ] );
			edge[ 1 ].removeEdge( edge[ 0 ] );
//			System.out.println( " - " + edge[ 0 ].id + " " + edge[ 1 ].id );
		}
		
		Arrays.sort( graph, (a, b) -> a.id - b.id );
		
		return graph;
	}
	
	
	public static void main(String[] args) {
		Vertex[] graph = generateChordalMinusK(10, 30, 3, 1234L);
		
	}
}
