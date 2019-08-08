package org.jastadd.util;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jgrapht.Graph;

public class NaiveDominators<V, E> {
	private Graph<V, E> g;
	private V entry;
	private Map<V, Set<V>> dom = new HashMap<V, Set<V>>();
	private Map<V, Integer> vertex2Index = new HashMap<V, Integer>();
	private List<V> index2Vertex = new ArrayList<V>();

	private void init() {
		int idx = 0;
		for (V v : g.vertexSet()) {
			index2Vertex.add(v);
			vertex2Index.put(v, idx++);
			dom.put(v, new HashSet<V>());
		}
	}

	private void compute() {
		int nVertices = g.vertexSet().size();

		List<BitSet> bitdoms = new ArrayList<BitSet>();

		if (!g.incomingEdgesOf(entry).isEmpty())
			throw new RuntimeException("Entry node must not have incoming edges.");

		// Initialize the dominators sets; each node dominates
		// itself
		for (int i = 0; i < nVertices; ++i) {
			BitSet bs = new BitSet(nVertices);
			if (index2Vertex.get(i) == entry) {
				// The entry node dominates itself
				bs.set(i);
			} else {
				// All other nodes are dominated by all the nodes
				bs.set(0, nVertices);
			}
			bitdoms.add(bs);
		}

		// Fixpoint computation
		boolean change;
		do {
			change = false;
			for (int i = 0; i < nVertices; ++i) {
				V v = index2Vertex.get(i);
				BitSet intersection = new BitSet(nVertices);
				if (!g.incomingEdgesOf(v).isEmpty())
					intersection.set(0, nVertices);

				for (E e : g.incomingEdgesOf(v)) {
					V s = g.getEdgeSource(e);
					BitSet pred = bitdoms.get(vertex2Index.get(s));
					intersection.and(pred);
				}

				BitSet current = bitdoms.get(i);
				intersection.set(i);
				if (!intersection.equals(current)) {
					change = true;
					bitdoms.set(i, intersection);
				}
			}
		} while (change);

		// Translate from bitsets to vertex sets
		for (int i = 0; i < nVertices; ++i) {
			BitSet current = bitdoms.get(i);
			for (int j = current.nextSetBit(0); j >= 0; j = current.nextSetBit(j + 1)) {
				dom.get(index2Vertex.get(i)).add(index2Vertex.get(j));
			}
		}
	}

	public NaiveDominators(Graph<V, E> g, V entry) {
		this.g = g;
		this.entry = entry;

		init();
		compute();
	}

	public Set<V> dominators(V v) {
		return dom.get(v);
	}

	public boolean dominates(V dominator, V dominated) {
		return dom.get(dominated).contains(dominator);
	}

	public V immediateDominator(V v) {
		outer: for (V d : dominators(v)) {
			if (d == v)
				continue;
			for (V d1 : dominators(v)) {
				if (d1 != d && d1 != v && dominates(d, d1))
					continue outer;
			}
			return d;
		}
		return null;
	}
}
