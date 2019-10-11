package template;

/* import table */
import logist.simulation.Vehicle;

import java.util.ArrayList;
import java.util.List;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeAgent implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR }
	
	/* Environment */
	Topology topology;
	TaskDistribution td;
	
	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		
		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		
		// ...
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;
		
		// Create a state corresponding to the current situation
		State state = new State(vehicle.getCurrentCity(), vehicle.capacity(),
				vehicle.costPerKm(), 0d);
		
		for(Task task : tasks) {
			state.addAvailableTask(task);
		}
		for(Task task : vehicle.getCurrentTasks()) {
			state.addCarriedTask(task);
		}		
		
		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			plan = AStarPlan(state);
			break;
		case BFS:
			plan = BFSPlan(state);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}
		
		System.out.println("Agent " + this.agent.id() + " has computed plan:");
		System.out.println(plan.toString());
		System.out.println("Plan total distance: " + plan.totalDistance() + " km.");
		
		return plan;
	}
	
	// Compute a plan according to the Breadth-First Search algorithm
	private Plan BFSPlan(State initState) {
		List<State> Q = new ArrayList<State>();
		Q.add(initState);
		
		State n = null;
		
		while(true) {
			if(Q.isEmpty())
				return null;
			
			n = Q.remove(0);
			
			if(n.isFinalState()) {
				break;
			}
			
			Q.addAll(n.getNextStates());
			Q.sort(null);
		}
		
		return n.getPlan();
	}
	
	// Compute a plan according to the A* algorithm
	private Plan AStarPlan(State initState) {
		List<State> Q = new ArrayList<State>(), C = new ArrayList<State>();
		Q.add(initState);
		
		State n = null;
		
		while(true) {
			if(Q.isEmpty())
				return null;
			
			n = Q.remove(0);
			
			if(n.isFinalState()) {
				break;
			}
			
			boolean contains = false;
			State stateToBeRemove = null;
			for(State s : C) {
				if(n.isStateEqual(s)) {
					if(n.getCost() < s.getCost()) {
						contains = false;
						stateToBeRemove = s;
					}
					else {
						contains = true;
					}
					break;
				}
			}
			
			if(!contains) {
				C.remove(stateToBeRemove);
				C.add(n);
				Q.addAll(n.getNextStates());
				Q.sort(null);
			}
			
		}
		
		return n.getPlan();
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		
		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}
}
