package org.kframework.parser.concrete.disambiguate;

import org.kframework.kil.ASTNode;
import org.kframework.kil.Sentence;
import org.kframework.kil.Variable;
import org.kframework.kil.loader.DefinitionHelper;
import org.kframework.kil.visitors.BasicTransformer;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.kframework.utils.errorsystem.KException;
import org.kframework.utils.errorsystem.KException.ExceptionType;
import org.kframework.utils.errorsystem.KException.KExceptionGroup;
import org.kframework.utils.general.GlobalSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

public class VariableTypeInferenceFilter extends BasicTransformer {

	public VariableTypeInferenceFilter(DefinitionHelper definitionHelper) {
		super("Variable type inference", definitionHelper);
	}

	@Override
	public ASTNode transform(Sentence r) throws TransformerException {

		CollectVariablesVisitor vars = new CollectVariablesVisitor(definitionHelper);
		r.accept(vars);

		// for each variable name do checks or type inference
		for (Entry<String, java.util.List<Variable>> varEntry : vars.getVars().entrySet()) {
			java.util.List<Variable> varList = varEntry.getValue();

			// divide into locations
			Map<String, java.util.List<Variable>> varLoc = new HashMap<String, java.util.List<Variable>>();
			for (Variable var : varList) {
				if (varLoc.containsKey(var.getLocation()))
					varLoc.get(var.getLocation()).add(var);
				else {
					java.util.List<Variable> varss = new ArrayList<Variable>();
					varss.add(var);
					varLoc.put(var.getLocation(), varss);
				}
			}

			// get declarations (if any)
			java.util.Set<Variable> varDecl = new HashSet<Variable>();
			for (Variable var : varEntry.getValue()) {
				if (var.isUserTyped())
					varDecl.add(var);
			}

			// check to see if you have variable declarations with two different sorts
			if (varDecl.size() > 1) {
				for (Variable v1 : varDecl)
					for (Variable v2 : varDecl)
						if (v1 != v2)
							if (!v1.getSort(definitionHelper).equals(v2.getSort(definitionHelper))) {
								String msg = "Variable '" + v1.getName() + "' declared with two different sorts: " + v1.getSort(definitionHelper) + " and " + v2.getSort(definitionHelper);
								throw new TransformerException(new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, msg, getName(), v1.getFilename(), v1.getLocation()));
							}
			}

			// check to see if the declared type fits everywhere
			if (varDecl.size() == 1) {
				Map<String, String> varDeclMap = new HashMap<String, String>();
				Variable var = varDecl.iterator().next();
				varDeclMap.put(var.getName(), var.getSort(definitionHelper));

				try {
					r = (Sentence) r.accept(new VariableTypeFilter(varDeclMap, definitionHelper));
				} catch (TransformerException e) {
					e.report();
				}
			} else {
				// when there aren't any variable declarations
				java.util.Set<Variable> isect = new HashSet<Variable>();
				// find the intersection of varLoc
				for (Variable var1 : varLoc.entrySet().iterator().next().getValue()) {
					// iterate over the first variable location
					boolean inAll = true;
					for (Entry<String, java.util.List<Variable>> varLocEntry : varLoc.entrySet()) { // iterate over each location
						if (!varLocEntry.getValue().contains(var1)) {
							inAll = false;
							break;
						}
					}
					if (inAll)
						isect.add(var1);
				}
				Variable varr = varLoc.entrySet().iterator().next().getValue().get(0);
				if (isect.size() == 0) {
					String msg = "Could not infer a sort for variable '" + varr.getName() + "' to match every location.";
					throw new TransformerException(new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, msg, varr.getFilename(), varr.getLocation()));
				}

				Variable inferredVar = null;

				// if I have more than one possibility choose a maximum
				if (isect.size() > 1) {
					java.util.List<Variable> maxSorts = new ArrayList<Variable>();

					for (Variable vv1 : isect) {
						boolean maxSort = true;
						for (Variable vv2 : isect) {
							if (definitionHelper.isSubsorted(vv2.getSort(definitionHelper), vv1.getSort(definitionHelper)))
								maxSort = false;
						}
						if (maxSort)
							maxSorts.add(vv1);
					}

					if (maxSorts.size() > 1) {
						String msg = "Could not infer a unique sort for variable '" + varr.getName() + "'.";
						msg += " Possible sorts: ";
						for (Variable vv1 : maxSorts)
							msg += vv1.getSort(definitionHelper) + ", ";
						msg = msg.substring(0, msg.length() - 2);
						throw new TransformerException(new KException(ExceptionType.ERROR, KExceptionGroup.CRITICAL, msg, varr.getFilename(), varr.getLocation()));
					}

					if (maxSorts.size() == 1)
						inferredVar = maxSorts.get(0);
				} else if (isect.size() == 1)
					inferredVar = isect.iterator().next();

				if (inferredVar != null) {
					Map<String, String> varDeclMap = new HashMap<String, String>();
					varDeclMap.put(inferredVar.getName(), inferredVar.getSort(definitionHelper));

					try {
						r = (Sentence) r.accept(new VariableTypeFilter(varDeclMap, definitionHelper));
					} catch (TransformerException e) {
						e.report();
					}

					String msg = "Variable '" + inferredVar.getName() + "' was not declared. Assuming sort " + inferredVar.getSort(definitionHelper);
					GlobalSettings.kem.register(new KException(ExceptionType.HIDDENWARNING, KExceptionGroup.COMPILER, msg, varr.getFilename(), varr.getLocation()));
				}
			}
			// type inference and error reporting
			// -- Error: type mismatch for variable... (when the declared variable doesn't fit everywhere)
			// -- Error: could not infer a sort for variable... (when the intersection is void)
			// -- Error: could not infer a unique sort for variable... (when there isn't a maximum sort in the intersection)
			// -- Warning: untyped variable, assuming sort...
		}

		return r;
	}
}
