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
import org.jastadd.jastaddparser.ast.Grammar;
import org.jastadd.jastaddparser.ast.Rule;

public class MetaRuleSolver {
	private Grammar g;
	public MetaRuleSolver(Grammar g) {
		this.g = g;
	}

	public Set<Rule> solve() {
		Store store = new Store();
		Map<Rule, IntVar> name2Var = new HashMap<Rule, IntVar>();

		int nVertices = g.rules().size();

		List<IntVar> allVars = new ArrayList<IntVar>();
		for (Rule r : g.rules()) {
			String ruleName = r.getIdDecl().getID();
			IntVar bv = new IntVar(store, ruleName, 0, 1);
			name2Var.put(r, bv);
			allVars.add(bv);
		}

		IntVar allSum = new IntVar(store, 0, nVertices);
		store.impose(new SumInt(allVars, "==", allSum));

		for (Rule r : g.rules()) {
			if (r.hasNonTrivialUses()) {
				List<IntVar> vars = new ArrayList<IntVar>();
				// this node is used; require that it or any other of its 'INCLUDE'
				// descendants is a metavariable
				vars.add(name2Var.get(r));
				for (Rule c : r.chainRules()) {
					vars.add(name2Var.get(c));
				}
				IntVar sum = new IntVar(store, 0, nVertices);
				store.impose(new SumInt(vars, "==", sum));
				store.impose(new XgtC(sum, 0));
			}
		}

		System.out.println(store);

		IntVar[] allVarsArray = allVars.toArray(new IntVar[allVars.size()]);

		Search<IntVar> search = new DepthFirstSearch<IntVar>();
		SelectChoicePoint<IntVar> select =
			new SimpleSelect<IntVar>(allVarsArray, null, new IndomainMin<IntVar>());

		boolean result = search.labeling(store, select, allSum);

		Set<Rule> ret = new HashSet<Rule>();

		if (result) {
			for (IntVar v : allVarsArray) {
				if (v.value() == 1) {
					ret.add(g.ruleByName().get(v.id()));
				}
			}
		}
		return ret;
	}
}
