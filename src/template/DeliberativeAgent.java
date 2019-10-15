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
			plan = optimalBFSPlan(state);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}
		
		// Compare the time required to compute BFS or A*
		compareAlgorithms(state, 10);
		
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
			// Should never happen, or it means that they aren't any solution
			if(Q.isEmpty())
				return Plan.EMPTY;
			
			// Get the first element of Q and remove it
			n = Q.remove(0);
			
			// If n is a final state we are done
			if(n.isFinalState()) {
				break;
			}
			
			// Add all next states of n at the end of Q
			Q.addAll(n.getNextStates());
		}
		
		return n.getPlan();
	}
	
	// Compute a plan according to the Breadth-First Search algorithm. The algorithm
	// has been a little bit modified in order to return the optimal plan in terms of
	// cost instead of number of actions.
	private Plan optimalBFSPlan(State initState) {
		List<State> Q = new ArrayList<State>();
		Q.add(initState);
		
		State n = null;
		State best = null;
		
		while(true) {
			if(Q.isEmpty())
			{
				// Should never happen, or it means that they aren't any solution
				if(best == null)
					return Plan.EMPTY;
				else
					return best.getPlan();
			}
			
			// Extract the first element of Q
			n = Q.remove(0);
			
			// If its cost is greater than the best solution found so far, discard it
			if(best != null && n.getCost() >= best.getCost())
				continue;
			
			// If n is a final state
			if(n.isFinalState()) {
				// n is then the best final state so far
				best = n;
			}
			else {
				// Add all next states of n at the end of Q
				Q.addAll(n.getNextStates());
			}
		}
	}
	
	// Compute a plan according to the A* algorithm
	private Plan AStarPlan(State initState) {
		List<State> Q = new ArrayList<State>(), C = new ArrayList<State>();
		Q.add(initState);
		
		State n = null;
		
		while(true) {
			// Should never happen, or it means that they aren't any solution
			if(Q.isEmpty())
				return Plan.EMPTY;
			
			// Extract the first element of Q
			n = Q.remove(0);
			
			// If n is a final state we are done
			if(n.isFinalState()) {
				break;
			}
			
			// Check if n has a copy with a lower cost in C
			boolean contained = false;
			State copyToRemove = null;
			for(State s : C) {
				if(n.isStateEqual(s)) {
					if(n.getCost() < s.getCost()) {
						contained = false;
						copyToRemove = s;
					}
					else {
						contained = true;
					}
					break;
				}
			}
			
			// If it has no copy, or a copy with a higher cost
			if(!contained) {
				// Remove the copy and add n instead
				C.remove(copyToRemove);
				C.add(n);
				
				// Add the next states to Q
				Q.addAll(n.getNextStates());
				// Sort Q based on the cost of the states
				Q.sort(null);
			}
			
		}
		
		return n.getPlan();
	}
	
	// Compute the mean execution time of BFS and A* algorithm. The mean is done over
	// n samples.
	public void compareAlgorithms(State initState, int n) {
		double startTime;
		
		startTime = System.currentTimeMillis();
		for(int i = 0; i < n; i++) {
			AStarPlan(initState);
		}
		System.out.println("Mean time elapsed for A*: "
				+ (System.currentTimeMillis() - startTime)/n + " ms.");
		
		startTime = System.currentTimeMillis();
		for(int i = 0; i < n; i++) {
			optimalBFSPlan(initState);
		}
		System.out.println("Mean time elapsed for BFS: "
				+ (System.currentTimeMillis() - startTime)/n + " ms.");
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
