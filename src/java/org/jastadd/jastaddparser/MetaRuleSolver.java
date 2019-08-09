package org.jastadd.jastaddparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jacop.constraints.SumInt;
import org.jacop.constraints.XeqC;
import org.jacop.constraints.XgtC;
import org.jacop.core.IntVar;
import org.jacop.core.Store;
import org.jacop.search.DepthFirstSearch;
import org.jacop.search.IndomainMin;
import org.jacop.search.Search;
import org.jacop.search.SelectChoicePoint;
import org.jacop.search.SimpleSelect;
import org.jastadd.jastaddparser.ast.GrammarEdgeType;
import org.jgrapht.Graph;
import org.jgrapht.graph.AbstractBaseGraph;

public class MetaRuleSolver {
	private Graph<String, GrammarEdgeType> g;
	private Set<String> excludeRules;

	public MetaRuleSolver(AbstractBaseGraph<String, GrammarEdgeType> g,
						  Set<String> excludeRules) {
		this.g = (Graph<String, GrammarEdgeType>) g.clone();
		this.excludeRules = excludeRules;
	}

	public void closeInclude() {
		// Do a closure of a include edges
		HashSet<GrammarEdgeType> worklist = new HashSet<GrammarEdgeType>(g.edgeSet());

		while (!worklist.isEmpty()) {
			GrammarEdgeType e = worklist.iterator().next();
			worklist.remove(e);
			if (e.isUse())
				continue;
			String src = g.getEdgeSource(e);
			for (GrammarEdgeType f : g.outgoingEdgesOf(g.getEdgeTarget(e))) {
				if (f.isUse())
					continue;
				String tgt = g.getEdgeTarget(f);
				if (g.getEdge(src, tgt) == null) {
					GrammarEdgeType newEdge = GrammarEdgeType.include();
					g.addEdge(src, tgt, newEdge);
					worklist.add(newEdge);
				}
			}
		}
	}

	public Set<String> solve() {
		closeInclude();

		Store store = new Store();
		Map<String, IntVar> name2Var = new HashMap<String, IntVar>();

		int nVertices = g.vertexSet().size();

		List<IntVar> allVars = new ArrayList<IntVar>();
		for (String r : g.vertexSet()) {
			IntVar bv = new IntVar(store, r, 0, 1);
			name2Var.put(r, bv);
			allVars.add(bv);
		}

		IntVar allSum = new IntVar(store, 0, nVertices);
		store.impose(new SumInt(allVars, "==", allSum));

		for (String r : g.vertexSet()) {
			boolean isList = excludeRules.contains(r);
			if (isList) {
				store.impose(new XeqC(name2Var.get(r), 0));
			} else {
				boolean isUsed = false;
				for (GrammarEdgeType e : g.incomingEdgesOf(r))
					if (e.isUse()) {
						isUsed = true;
						break;
					}

				if (isUsed) {
					List<IntVar> vars = new ArrayList<IntVar>();
					// this node is used; require that it or any other of its 'INCLUDE'
					// descendants is a metavariable
					vars.add(name2Var.get(r));
					for (GrammarEdgeType e : g.outgoingEdgesOf(r)) {
						if (e.isUse())
							continue;
						String tgt = g.getEdgeTarget(e);
						vars.add(name2Var.get(tgt));
					}

					IntVar sum = new IntVar(store, 0, nVertices);

					store.impose(new SumInt(vars, "==", sum));
					store.impose(new XgtC(sum, 0));
				}
			}
		}

		// store.print();

		IntVar[] allVarsArray = allVars.toArray(new IntVar[allVars.size()]);

		Search<IntVar> search = new DepthFirstSearch<IntVar>();
		SelectChoicePoint<IntVar> select =
			new SimpleSelect<IntVar>(allVarsArray, null, new IndomainMin<IntVar>());

		boolean result = search.labeling(store, select, allSum);


		Set<String> ret = new HashSet<String>();

		if (result) {
			for (IntVar v : allVarsArray) {
				if (v.value() == 1) {
					ret.add(v.id());
				}
			}
		}
		return ret;
	}
}
