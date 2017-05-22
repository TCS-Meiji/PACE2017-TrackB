package fillin.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import tw.common.LabeledGraph;

public class Instance {
	
	public static LabeledGraph read()
	{
		TreeSet< String > labels = new TreeSet< String >();
		List< String[] > lines = new LinkedList< String[] >();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line;
		try {
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.startsWith( "#" ) || line.isEmpty()) {
					continue; // line is either a comment or empty
				}
				
				String[] token = line.split( " " );
				if (token.length != 2) {
					throw new RuntimeException( "Input file is incorrect." );
				}
				if (token[ 0 ].equals( token[ 1 ] )) {
					continue; // self-loop
				}
				
				labels.add( token[ 0 ] );
				labels.add( token[ 1 ] );
				lines.add( token );
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		LabeledGraph g = new LabeledGraph( labels.toArray( new String[ labels.size() ] ) );
		lines.forEach(l -> g.addEdgeBetween( l[ 0 ] , l[ 1 ]) );
		return g;
	}
	
}
