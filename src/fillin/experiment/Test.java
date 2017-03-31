package fillin.experiment;

import java.io.FileNotFoundException;

import java.util.Scanner;

import fillin.branch.Branching;
import fillin.branch.RandomGen;
import fillin.branch.Vertex;
import fillin.main.Instance;
import fillin.main.Solver;
import tw.common.LabeledGraph;

public class Test {
	public static void main(String[] args) throws FileNotFoundException {
		int N = 100;
		int nTest = 1000;
		int density = 80;
		int K = 20;
		
//		System.setOut( new PrintStream( new File( "out" ) ) );
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
			Branching branch = new Branching( g );
			int br = 0;
			for (int k = 0; k <= 100; k++) {
				if (branch.solve( k )) {
					br = k;
					break;
				} else {
					System.out.println("fail: " + k);
				}
			}
			
//			System.out.println(data);
			LabeledGraph lg = Instance.read( new Scanner( data ) );
			Solver solver = new Solver();
			solver.solve( lg );
			int dp = solver.getOpt();
			
			System.out.println(br + " " + dp);
			if (br > dp) {
				// dp is provably incorrect
				System.out.println(br + " vs " + dp);
				return;
			}
		}
		System.out.println("complete");
	}
}
