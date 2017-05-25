Track B: Minimum Fill-In
====

This software computes a minimum fill-in of a graph.
The code will be submitted to [Track B of PACE 2017](https://pacechallenge.wordpress.com/pace-2017/track-b-minimum-fill-in/).

## Requirement
Java 1.8 or higher

## Build
Run build.sh.
```
./build.sh
```

## Usage
Run run.sh. The input graph is given from the standard input.
```
./run.sh < 1.graph
```
Please see [here](https://pacechallenge.wordpress.com/pace-2017/track-b-minimum-fill-in/) for the input graph format.

## Description
The implemented algorithm is a modified version of a positive-instance driven dynamic programming for treewidth given by [3], which is submitted to Track A, and has some known and new preprocessing techniques.

As for preprocessing, we use the following basic techniques.
- Remove simplicial vertices.
- Decompose by clique separators [4].

Bodlaender et al. [1] gave a sufficient condition for edges that can be safely added for the minimum fill-in problem. They showed that if a graph has a minimal separator that has exactly one missing edge and is contained in the set of neighbors of a vertex, then this separator can be filled into a clique without losing optimality. We generalize this condition for minimal separators that have more than one missing edges and give a polynomial time algorithm for detecting this condition (the detail will be published together with some experimental evaluations).

The implemented dynamic programming basically follows the algorithm in [3]. His algorithm is based on the algorithm of Bouchitté and Todinca [2] and computes an optimal tree decomposition by enumerating ``relevant'' potential maximal cliques and minimal separators. We modified his algorithm to compute a tree decomposition with minimum number of fill edges.

## References
[1] H.L. Bodlaender, P. Heggernes, Y. Villanger: Faster Parameterized Algorithms for Minimum Fill-In.
Algorithmica 61(4), 817-838, 2011.  
[2] V. Bouchitté, I. Todinca: Treewidth and minimum fill-in: Grouping the minimal separators, SIAM Journal on Computing 31(1), 212-232, 2001.  
[3] H. Tamaki: Prositive-instance driven dynamic programming for treewidth. arXiv:1704.05286[cs.DS], 2017.  
[4] R.E. Tarjan: Decomposition by clique separators. Discrete Mathematics 55(2), 221-232, 1985.

## Authors
Yasuaki Kobayashi (Kyoto University) and Hisao Tamaki (Meiji University)
