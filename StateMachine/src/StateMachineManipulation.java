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
import SimplStateMachine.impl.BooleanDataImpl;
import SimplStateMachine.impl.DataImpl;
import SimplStateMachine.impl.IntegerDataImpl;

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
			else  {
				computeOperations(s.getOperation());
				s = null;
			}
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
	
	public Transition getTriggerableTransition(String evt, StateMachine sm) {
		boolean fini = false;
		State activeState = this.getLeafActiveState(sm);
		Transition trans = null;
		while(!fini) {
			for (Transition t : sm.getTransitions())
				if (t.getEvent().getName().equals(evt) && (activeState == t.getSource())) {
					System.out.println("In Guard");
					if (t.getGuard() == null || ((BooleanData)computeExpression(t.getGuard())).isValue()) {
						trans = t;
						fini = true;
					}
					System.out.println("out Guard");
				}
			if (!fini) {
				activeState = activeState.getContainer();
				if (activeState == null) 
					fini = true;
			}
		}
		return trans;
	}
	
	public Data computeExpression(ExpressionElement e) {
		System.out.println(e.getClass().getName());
		
		if (e instanceof VariableReference) return ((VariableReference) e).getVariable().getValue();
		if (e instanceof Data) return (Data) e;
		
		Expression exp = (Expression) e;
		Data left = computeExpression(exp.getLeft());
		Data right = computeExpression(exp.getRight());
		Data result = null;
		
		switch(exp.getOperator()) {
			case ADD :
				result = new IntegerDataImpl();
				((IntegerData)result).setValue(((IntegerData) left).getValue() + ((IntegerData) right).getValue());
				break;
			case SUB :
				result = new IntegerDataImpl();
				((IntegerData)result).setValue(((IntegerData) left).getValue() - ((IntegerData) right).getValue());
				break;
			case MUL :
				result = new IntegerDataImpl();
				((IntegerData)result).setValue(((IntegerData) left).getValue() * ((IntegerData) right).getValue());
				break;
			case DIV :
				result = new IntegerDataImpl();
				((IntegerData)result).setValue(((IntegerData) left).getValue() / ((IntegerData) right).getValue());
				break;
			case EQ :
				result = new BooleanDataImpl();
				if (left instanceof IntegerData)
					((BooleanData)result).setValue(((IntegerData) left).getValue() == ((IntegerData) right).getValue());
				else 
					((BooleanData)result).setValue(((BooleanData) left).isValue() == ((BooleanData) right).isValue());
				break;
			case GT :
				result = new BooleanDataImpl();
				((BooleanData)result).setValue(((IntegerData) left).getValue() > ((IntegerData) right).getValue());
				break;
			case LT :
				result = new BooleanDataImpl();
				((BooleanData)result).setValue(((IntegerData) left).getValue() < ((IntegerData) right).getValue());
				break;
			case GTE :
				result = new BooleanDataImpl();
				((BooleanData)result).setValue(((IntegerData) left).getValue() >= ((IntegerData) right).getValue());
				break;
			case LTE :
				result = new BooleanDataImpl();
				((BooleanData)result).setValue(((IntegerData) left).getValue() <= ((IntegerData) right).getValue());
				break;
			case AND :
				result = new BooleanDataImpl();
				((BooleanData)result).setValue(((BooleanData) left).isValue() && ((BooleanData) right).isValue());
				break;
			case OR :
				result = new BooleanDataImpl();
				((BooleanData)result).setValue(((BooleanData) left).isValue() || ((BooleanData) right).isValue());
				break;
			case NEQ :
				result = new BooleanDataImpl();
				if (left instanceof IntegerData)
					((BooleanData)result).setValue(((IntegerData) left).getValue() != ((IntegerData) right).getValue());
				else 
					((BooleanData)result).setValue(((BooleanData) left).isValue() != ((BooleanData) right).isValue());
				break;
			case NOT :
				result = new BooleanDataImpl();
				((BooleanData)result).setValue(!((BooleanData) left).isValue());
				break;
		}
		return result;
	}

	public void computeOperations(Operation op) {
		if (op != null) {
			for (Assignment a : op.getContents()) {
				System.out.println("In Assignement");
				a.getVariable().setValue(computeExpression(a.getExpression()));
				System.out.println("Out Assignement");
			}
		}
	}
	
	public void processEvent(StateMachine sm, String event) {
		Transition trans = this.getTriggerableTransition(event, sm);
		if (trans != null) {
			Expression guard = trans.getGuard();
			//if (guard != null) { // has guard
				this.unactiveStateHierarchy(trans.getSource());
				State target = trans.getTarget();
				this.activeStateHierarchy(target);
				if (target instanceof CompositeState)
					computeOperations(getLeafActiveState((CompositeState)target).getOperation());
				else
					computeOperations(target.getOperation());
			//}
		} else {
			System.out.println("trans is null");
			
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
            System.out.println();
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