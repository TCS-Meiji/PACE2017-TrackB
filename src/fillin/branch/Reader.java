package fillin.branch;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import fillin.branch.Vertex;

public class Reader {
	public static Vertex[] read(Scanner sc)
	{
		int max = 0;
		List< int[] > ls = new LinkedList<>();
		while (sc.hasNext()) {
			int s = sc.nextInt();
			int t = sc.nextInt();
			max = Math.max( max, s );
			max = Math.max( max, t );
			ls.add( new int[]{ s, t } );
		}
		
		Vertex[] graph = new Vertex[ max + 1 ];
		for (int i = 0; i < graph.length; i++) {
			graph[ i ] = new Vertex( i, i+"" );
		}
		for (int[] p: ls) {
			graph[ p[0] ].addEdge( graph[ p[1] ] );
			graph[ p[1] ].addEdge( graph[ p[0] ] );
		}
		
		return graph;
	}
	
	public static Vertex[] read()
	{
		return read( new Scanner( System.in ) );
	}
	
	public static Vertex[] read(String filepath)
	{
		try {
			return read( new Scanner( new File( filepath ) ) );
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return read();
		}
	}
}
