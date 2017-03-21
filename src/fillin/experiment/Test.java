package fillin.experiment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import java.util.Scanner;

import fillin.branch.Branching;
import fillin.branch.RandomGen;
import fillin.branch.Vertex;
import fillin.main.Decomposer;
import fillin.main.Instance;
import fillin.main.Lowerbound;
import fillin.main.Solver;
import tw.common.LabeledGraph;
import tw.common.TreeDecomposition;

public class Test {
	public static void main(String[] args) throws FileNotFoundException {
		int N = 100;
		int nTest = 1000;
		int density = 35;
		int K = 30;
		
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
			
			LabeledGraph lg = Instance.read( new Scanner( data ) );
			Lowerbound lowerbound = new Lowerbound( lg );
			Solver solver = new Solver();
			int dp = solver.solve( lg );
			int lb = lowerbound.get();
			System.out.println(lb + " " + dp);
			if (lb > dp) {
				// dp is provably incorrect
				System.out.println(data);
				System.out.println(lb + " vs " + dp);
				return;
			}
		}
		System.out.println("complete");
	}
}
