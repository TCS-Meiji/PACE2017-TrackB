package fillin.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeSet;

import tw.common.LabeledGraph;

public class Instance {
	
	public static LabeledGraph read(Scanner sc)
	{
		TreeSet< String > labels = new TreeSet< String >();
		List< String[] > lines = new LinkedList< String[] >();
		while (sc.hasNextLine()) {
			String line = sc.nextLine().trim();
			if (line.startsWith( "#" ) || line.isEmpty()) {
				continue; // line is either a comment or empty
			}
			
			String[] token = line.split( " " );
			if (token.length != 2) {
				throw new RuntimeException( "Input file is not correct." );
			}
			
			labels.add( token[ 0 ] );
			labels.add( token[ 1 ] );
			lines.add( token );
		}
		
		int N = 0;
		String[] labelArray =  new String[ labels.size() ];
		for (String s: labels) {
			labelArray[ N++ ] = s;
		}
		
		LabeledGraph g = new LabeledGraph( labelArray );
		
		for (String[] line: lines) {
			g.addEdge( line[ 0 ] , line[ 1 ] );
		}
				
		return g;
		
	}
	
	public static LabeledGraph read(File file) 
	{
		try {
			return read( new Scanner( file ) );
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return read( );
		}
	}
	
	public static LabeledGraph read(String fileName)
	{
		try {
			Scanner sc = new Scanner( new File( fileName ) );
			return read( sc );
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return read( );
		}
	}
	
	public static LabeledGraph read()
	{
		return read( new Scanner( System.in ) );
	}
	
	public static LabeledGraph randomGraph(int N, int prob, long seed)
	{
		Random rnd = new Random( seed );
		String data = "";
		for (int i = 0; i < N; i++) {
			for (int j = i + 1; j < N; j++) {
				if (rnd.nextInt( 100 ) < prob) {
					data += i + " " + j + "\n";
				}
			}
		}
		
		System.out.println(data);
		
		return read( new Scanner( data ) );
	}
	
	public static LabeledGraph randomGraph(int N, int prob)
	{
		Random rnd = new Random();
		long seed = rnd.nextLong();
//		System.out.println( seed );
		return randomGraph(N, prob, seed);
	}
	
}
