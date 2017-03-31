package fillin.experiment;

import fillin.main.Instance;
import fillin.main.Solver;
import tw.common.LabeledGraph;

public class Experiment {
	public static void main(String[] args) {
		for (int tc = 1; tc <= 100; tc++) {
			String name = tc + ".graph";
			LabeledGraph g = Instance.read("instances/" + name);
			if (g.n > 5000) continue;
			Solver solver = new Solver();
			solver.solve( g );
			System.out.println(name + "\t" + solver.getOpt());
		}
	}
}
