package org.jastadd.util.test;

import org.jgrapht.*;
import org.jgrapht.graph.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.jastadd.util.NaiveDominators;
import org.junit.Test;

public class TestNaiveDominators {
	static Graph<Integer, DefaultEdge> createGraph(int nVertices) {
		Graph<Integer, DefaultEdge> g = new DefaultDirectedGraph<Integer, DefaultEdge>(DefaultEdge.class);
		for (int i = 0; i < nVertices; ++i)
			g.addVertex(i);
		return g;
	}

	static Set<Integer> set(Integer... is) {
		TreeSet<Integer> s = new TreeSet<Integer>();
		for (int i : is)
			s.add(i);
		return s;
	}

	@Test public void test1() {
		Graph<Integer, DefaultEdge> g = createGraph(4);

		g.addEdge(0, 1);
		g.addEdge(0, 2);
		g.addEdge(1, 3);
		g.addEdge(2, 3);

		NaiveDominators<Integer, DefaultEdge> dom = new NaiveDominators<Integer, DefaultEdge>(g, 0);

		assertEquals(set(0, 1), dom.dominators(1));
		assertEquals(set(0, 2), dom.dominators(2));
		assertEquals(set(0, 3), dom.dominators(3));
		assertEquals(set(0), dom.dominators(0));

		assertEquals(null, dom.immediateDominator(0));
		assertEquals(0, (int)dom.immediateDominator(1));
		assertEquals(0, (int)dom.immediateDominator(2));
		assertEquals(0, (int)dom.immediateDominator(3));
	}

	@Test public void test2() {
		Graph<Integer, DefaultEdge> g = createGraph(5);

		g.addEdge(0, 1);
		g.addEdge(1, 2);
		g.addEdge(2, 3);
		g.addEdge(3, 1);
		g.addEdge(2, 4);

		NaiveDominators<Integer, DefaultEdge> dom = new NaiveDominators<Integer, DefaultEdge>(g, 0);

		assertEquals(set(0), dom.dominators(0));
		assertEquals(set(0, 1), dom.dominators(1));
		assertEquals(set(0, 1, 2), dom.dominators(2));
		assertEquals(set(0, 1, 2, 3), dom.dominators(3));
		assertEquals(set(0, 1, 2, 4), dom.dominators(4));

		assertEquals(null, dom.immediateDominator(0));
		assertEquals(0, (int)dom.immediateDominator(1));
		assertEquals(1, (int)dom.immediateDominator(2));
		assertEquals(2, (int)dom.immediateDominator(3));
		assertEquals(2, (int)dom.immediateDominator(4));
	}

	@Test public void test3() {
		Graph<Integer, DefaultEdge> g = createGraph(3);
		g.addEdge(0, 1);
		g.addEdge(0, 2);
		g.addEdge(1, 2);
		g.addEdge(2, 1);

		NaiveDominators<Integer, DefaultEdge> dom = new NaiveDominators<Integer, DefaultEdge>(g, 0);

		assertEquals(null, dom.immediateDominator(0));
		assertEquals(0, (int)dom.immediateDominator(1));
		assertEquals(0, (int)dom.immediateDominator(2));
	}
}
