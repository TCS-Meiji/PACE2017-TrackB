Track B: Minimum Fill-In
====

This software computes a minimum fill-in of graphs.
The code will be submitted to [Track B of PACE 2017](https://pacechallenge.wordpress.com/pace-2017/track-b-minimum-fill-in/).


## Requirement
Java 1.8 or higher

## Build
To build, execute build.sh.
```
./build.sh
```

## Usage
Run run.sh. The graph is given from the standard input.
```
./run.sh < 1.graph
```
Please see [here](https://pacechallenge.wordpress.com/pace-2017/track-b-minimum-fill-in/) for the input graph format.

## Description
The implemented algorithm is a modified version of a positive-instance driven dynamic programming
for treewidth given by [2], which is submitted to Track A, and has some known and new preprocessing techniques.

For preprocessing, we use the following basic techniques
- remove simplicial vertices,
- decompose by clique separators [3].

Bodlaender et al. [1] gave a sufficient condition for edges that can be safely added.
They showed that if a graph has a minimal separator that has exactly one missing edge and satisfies an additional condition, then this separator can be filled into a clique without losing optimality.
We generalize this condition for minimal separators that has more than one missing edges and give a polynomial time algorithm for detecting this condition (the detail will be published togeter with
some experimental evaluations).



## References
[1] H.L. Bodlaender, P. Heggernes, Y. Villanger: Faster Parameterized Algorithms for Minimum Fill-In.
Algorithmica 61(4), 817-838, 2011.  
[2] H. Tamaki: Prositive-instance driven dynamic programming for treewidth. arXiv:1704.05286[cs.DS], 2017.  
[3] R.E. Tarjan: Decomposition by clique separators. Discrete Mathematics 55(2), 221-232, 1985.

## Authors
Yasuaki Kobayashi (Kyoto University) and Hisao Tamaki (Meiji University)
