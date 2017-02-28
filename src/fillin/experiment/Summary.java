package fillin.experiment;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import fillin.main.Instance;
import tw.common.LabeledGraph;

public class Summary {
	public static void main(String[] args) throws FileNotFoundException {
		String instanceDir = "instances/";
		String resultDir = "result/";
		
		for (int i = 1; i <= 100; i++) {
			String graphFile = instanceDir + i + ".graph";
			LabeledGraph g = Instance.read( graphFile );
			int n = g.n;
			int m = g.edges();
			Scanner scan = new Scanner( new File( resultDir + i + ".result" ) );
			int compsize = 0;
			int opt = -1;
			int lb = 0;
			long time = 0;
			while (scan.hasNextLine()) {
				String s = scan.nextLine();
				if (s.startsWith("Current target cost: ")) {
					lb = Math.max( lb, Integer.parseInt( s.replace( "Current target cost: ", "" ) ) );
				} else if (s.startsWith("n = ")) {
					compsize = Math.max( compsize , Integer.parseInt( s.split(" ")[ 2 ] ));
				} else if (s.startsWith("name = ")) {
					opt = Integer.parseInt(s.split(" ")[ 5 ].replace(",", ""));
					time = Long.parseLong(s.split(" ")[ 8 ]);
				}
			}
			if (opt != -1) {
				System.out.println( i + "graph, " + n + ", " + m + ", " + compsize + ", " + opt + ", " + time );
			} else {
				System.out.println( i + "graph, " + n + ", " + m + ", " + compsize + ", " + lb + " <=, " );
			}
		}
	}
}
