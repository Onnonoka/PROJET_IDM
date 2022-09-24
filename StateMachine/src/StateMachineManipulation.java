import java.util.Scanner;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLMapImpl;

import SimplStateMachine.*;

public class StateMachineManipulation {

	public void sauverModele(String uri, EObject root) {
		Resource resource = null;
		try {
			URI uriUri = URI.createURI(uri);
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
			resource = (new ResourceSetImpl()).createResource(uriUri);
			resource.getContents().add(root);
			resource.save(null);
		} catch (Exception e) {
			System.err.println("ERREUR sauvegarde du mod�le : "+e);
			e.printStackTrace();
		}
	}

	public Resource chargerModele(String uri, EPackage pack) {
		Resource resource = null;
		try {
			URI uriUri = URI.createURI(uri);
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
			resource = (new ResourceSetImpl()).createResource(uriUri);
			XMLResource.XMLMap xmlMap = new XMLMapImpl();
			xmlMap.setNoNamespacePackage(pack);
			java.util.Map options = new java.util.HashMap();
			options.put(XMLResource.OPTION_XML_MAP, xmlMap);
			resource.load(options);
		}
		catch(Exception e) {
			System.err.println("ERREUR chargement du mod�le : "+e);
			e.printStackTrace();
		}
		return resource;
	}

	public StateMachine getModelBase(String modelFile) {
		Resource resource = chargerModele(modelFile, SimplStateMachinePackage.eINSTANCE);
		if (resource == null) {
			System.err.println(" Erreur de chargement du mod�le");
			return null;
		}

		TreeIterator<EObject> it = resource.getAllContents();

		StateMachine sm = null;
		while(it.hasNext()) {
			EObject obj = (EObject) it.next();
			if (obj instanceof StateMachine) {
				sm = (StateMachine) obj;
				break;
			}
		}
		return sm;
	}


	public void initStateMachine(StateMachine sm) {
		sm.setIsActive(true);
		State s = sm.getInitialState().getReferencedState();
		while (s != null) {
			s.setIsActive(true);
			if (s instanceof CompositeState)
				s = ((CompositeState) s).getInitialState().getReferencedState();
			else 
				s = null;
		}
	}
	
	public void unactiveUpStateHierarchy(State s) {
		State up = s.getContainer();
		while (up != null) {
			up.setIsActive(false);
			up = up.getContainer();
		}
	}
	
	public void unactiveDownStateHierarchy(State s) {
		if (s instanceof CompositeState) {
			for (State down : ((CompositeState)s).getStates()) {
				down.setIsActive(false);
				if (down instanceof CompositeState) {
					this.unactiveDownStateHierarchy(down);
				}
			}
		}
	}
	
	public void unactiveStateHierarchy(State s) {
		s.setIsActive(false);
		this.unactiveUpStateHierarchy(s);
		this.unactiveDownStateHierarchy(s);
	}
	
	public void activeUpStateHierarchy(State s) {
		State up = s.getContainer();
		while (up != null) {
			up.setIsActive(true);
			up = up.getContainer();
		}
	}
	
	public void activeDownStateHierarchy(State s) {
		if (s instanceof CompositeState) {
				State init = ((CompositeState)s).getInitialState().getReferencedState();
				init.setIsActive(true);
				if (init instanceof CompositeState) 
					this.unactiveDownStateHierarchy(init);			
		}
	}
	
	public void activeStateHierarchy(State s) {
		s.setIsActive(true);
		this.activeUpStateHierarchy(s);
		this.activeDownStateHierarchy(s);
	}
	
	public State getLeafActiveState(CompositeState comp) {
		for (State s: comp.getStates())
			if (s.isIsActive())
				if (s instanceof CompositeState)
					return this.getLeafActiveState((CompositeState)s);
				else return s;
		return null;
	}
	
	//public 
	
	public Transition getTriggerableTransition(String evt, StateMachine sm) {
		boolean fini = false;
		State activeState = this.getLeafActiveState(sm);
		Transition trans = null;
		while(!fini) {
			for (Transition t : sm.getTransitions())
				if (t.getEvent().getName().equals(evt) && (activeState == t.getSource())) {
					trans = t;
					fini = true;
				}
			if (!fini) {
				activeState = activeState.getContainer();
				if (activeState == null) 
					fini = true;
			}
		}
		return trans;
	}
	/*
	public Data computeExpression(ExpressionElement e) {
		if (e instanceof Variable) return ((Variable) e).getValue();
		if (e instanceof Data) return (Data) e;
		Expression exp = (Expression) e;
		Data left = computeExpression(exp.getLeft());
		Data right = computeExpression(exp.getRight());
		Data result;
		switch(exp.getOperator()) {
			case ADD :
				int result = ((IntegerData) left).getValue() + ((IntegerData) right).getValue();
			case SUB :
				int result = ((IntegerData) left).getValue() - ((IntegerData) right).getValue();
			case MUL :
				int result = ((IntegerData) left).getValue() * ((IntegerData) right).getValue();
			case DIV :
				int result = ((IntegerData) left).getValue() / ((IntegerData) right).getValue();
			case EQ :
				boolean result = ((IntegerData) left).getValue() == ((IntegerData) right).getValue();
			case GT :
				boolean result = ((IntegerData) left).getValue() > ((IntegerData) right).getValue();
			case LT :
				return computeNumberExpression(exp.getLeft()) < computeNumberExpression(exp.getRight());
			case GTE :
				return computeNumberExpression(exp.getLeft()) >= computeNumberExpression(exp.getRight());
			case LTE :
				return computeNumberExpression(exp.getLeft()) <= computeNumberExpression(exp.getRight());
			case AND :
				return computeBooleanExpression(exp.getLeft()) && computeBooleanExpression(exp.getRight());
			case OR :
				return computeBooleanExpression(exp.getLeft()) || computeBooleanExpression(exp.getRight());
			case NEQ :
				return computeBooleanExpression(exp.getLeft()) != computeBooleanExpression(exp.getRight());
			case NOT :
				return !computeBooleanExpression(exp.getLeft());
			default :
				return null;
		}
	}*/
	
	public int computeNumberExpression(ExpressionElement e) {
		if (e instanceof IntegerVariable) return ((IntegerData) ((Variable) e).getValue() ).getValue();
		if (e instanceof IntegerData) return ((IntegerData) e).getValue();
		Expression exp = (Expression) e;
		switch(exp.getOperator()) {
			case ADD :
				return computeNumberExpression(exp.getLeft()) + computeNumberExpression(exp.getRight());
			case SUB :
				return computeNumberExpression(exp.getLeft()) - computeNumberExpression(exp.getRight());
			case MUL :
				return computeNumberExpression(exp.getLeft()) * computeNumberExpression(exp.getRight());
			case DIV :
				return computeNumberExpression(exp.getLeft()) / computeNumberExpression(exp.getRight());
			default :
				return -1;
		}
	}
	
	public boolean isNumberExpression(ExpressionElement e) {
		if (e instanceof Variable) return e instanceof IntegerVariable;
		if (e instanceof Data) return e instanceof IntegerData;
		switch(((Expression) e).getOperator()) {
			case ADD :
			case SUB :
			case MUL :
			case DIV :
				return true;
			default :
				return false;
		}
	}
	
	public boolean computeBooleanExpression(ExpressionElement e) {
		if (e instanceof BooleanVariable) return ((BooleanData) ((Variable) e).getValue() ).isValue();
		if (e instanceof BooleanData) return ((BooleanData) e).isValue();
		Expression exp = (Expression) e;
		switch(exp.getOperator()) {
			case EQ :
				if (isNumberExpression(exp))
					return computeNumberExpression(exp.getLeft()) == computeNumberExpression(exp.getRight());
				else 
					return computeBooleanExpression(exp.getLeft()) == computeBooleanExpression(exp.getRight());
			case GT :
				return computeNumberExpression(exp.getLeft()) > computeNumberExpression(exp.getRight());
			case LT :
				return computeNumberExpression(exp.getLeft()) < computeNumberExpression(exp.getRight());
			case GTE :
				return computeNumberExpression(exp.getLeft()) >= computeNumberExpression(exp.getRight());
			case LTE :
				return computeNumberExpression(exp.getLeft()) <= computeNumberExpression(exp.getRight());
			case AND :
				return computeBooleanExpression(exp.getLeft()) && computeBooleanExpression(exp.getRight());
			case OR :
				return computeBooleanExpression(exp.getLeft()) || computeBooleanExpression(exp.getRight());
			case NEQ :
				if (isNumberExpression(exp))
					return computeNumberExpression(exp.getLeft()) != computeNumberExpression(exp.getRight());
				else 
					return computeBooleanExpression(exp.getLeft()) != computeBooleanExpression(exp.getRight());
			case NOT :
				return !computeBooleanExpression(exp.getLeft());
			default :
				return false;
		}
	}
	
	public void computeAssignment(Assignment a) {
		if (a.getVariable() instanceof IntegerVariable)
			((IntegerData) ((IntegerVariable) a.getVariable()).getValue()).setValue(computeNumberExpression(a.getExpression()));
		else 
			((BooleanData) ((BooleanVariable) a.getVariable()).getValue()).setValue(computeBooleanExpression(a.getExpression()));
	}
	
	public void processEvent(StateMachine sm, String event) {
		Transition trans = this.getTriggerableTransition(event, sm);
		if (trans != null) {
			Expression guard = trans.getGuard();
			if (guard != null) { // has guard
				this.unactiveStateHierarchy(trans.getSource());
				State target = trans.getTarget();
				this.activeStateHierarchy(target);
			}
		}
	}
	
	public void executeStateMachine(StateMachine sm) {
		
		this.initStateMachine(sm);
		
		Scanner scan = new Scanner(System.in);
        String event = "";
       
        while (!event.equals("end")) {
        	System.out.println("Les variables ont pour valeur : ");
        	for (Variable v : sm.getVariables()) {
            	System.out.println(v.getName() + " = " + v.getValue());
        	}
        	System.out.print("Entrez le nom d'un �v�nement (\"end\" pour terminer) : ");
        	
            event = scan.nextLine();
            if (!event.equals("end")) 
            	processEvent(sm,event);
        }
		scan.close();
	}
	
	public static void main(String argv[]) {

		StateMachineManipulation util = new StateMachineManipulation();

		System.out.println(" Chargement du mod�le");
		StateMachine sm = util.getModelBase("models/Voiture.xmi");
		System.out.println(" Mod�le charg�");

		util.executeStateMachine(sm);
	}
}