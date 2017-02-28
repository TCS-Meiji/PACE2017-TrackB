package fillin.experiment;

import java.util.Scanner;

import fillin.branch.Branching;
import fillin.branch.RandomGen;
import fillin.branch.Vertex;
import fillin.main.Decomposer;
import fillin.main.Instance;
import tw.common.LabeledGraph;
import tw.common.TreeDecomposition;

public class Test {
	public static void main(String[] args) {
		int N = 100;
		int nTest = 1000;
		int density = 35;
		int K = 15;
		
		
		// for each test, the instance is given as a random graph of N vertices such that
		// with probability density / 100, there is an edge between two distinct vertices.
		for (int i = 0; i < nTest; i++) {
			String data = "";
//			Vertex[] g = RandomGen.generate(N, density);
			Vertex[] g = RandomGen.generateChordalMinusK( N, density, K );
			for (Vertex v: g) {
				for (int w = v.adj.firstElement(); w >= 0; w = v.adj.nextElement( w )) {
					if (v.id < w) {
						data += v.id + " " + w + "\n";
					}
				}
			}
//			System.out.println(data);
			Branching branch = new Branching( g );
			int br = 0;
			for (int k = 0; k < 100; k++) {
				if (branch.solve( k )) {
					br = k;
					System.out.println("k = " + k + ": " + true);
					break;
				} else {
					System.out.println("k = " + k + ": " + false);
				}
			}
			
			LabeledGraph lg = Instance.read( new Scanner( data ) );
			Decomposer dec = new Decomposer( lg );
			TreeDecomposition td = dec.decompose();
			int dp = dec.getOpt();
			if (br != dp) {
				// dp is provably incorrect
				System.out.println(br + " vs " + dp);
//				System.out.println( "== fill edges(dp) ==" ); 
//				for (Pair< String, String > edge: td.computeFill( lg )) {
//					System.out.println( edge[ 0 ] + " " + edge[ 1 ] );
//				}
//				System.out.println( "====" );
				return;
			}
		}
		System.out.println("complete");
	}
}
