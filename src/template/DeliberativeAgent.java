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
		
		State state = new State(vehicle.getCurrentCity(), vehicle.capacity(), vehicle.costPerKm(), 0d);
		for(Task task : tasks) {
			state.addTask(task.pickupCity, task.deliveryCity, task.weight, false);
		}
		
		for(Task task : vehicle.getCurrentTasks()) {
			state.addTask(task.pickupCity, task.deliveryCity, task.weight, true);
		}
		
		List<State> nextStates = state.getNextStates();
		System.out.println("For current state:");
		System.out.println(state.toString());
		System.out.println("The next possible states are:");
		for(State s : nextStates) {
			System.out.println(s.toString());
		}
		
		BFSPlan(state);
		
		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = naivePlan(vehicle, tasks);
			break;
		case BFS:
			// ...
			plan = naivePlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		return plan;
	}
	
	private Plan BFSPlan(State initState) {
		Plan plan = new Plan(initState.getAgentPos());
		
		List<State> Q = new ArrayList<State>();
		Q.add(initState);
		
		State n = null;
		
		while(!Q.isEmpty()) {
			n = Q.remove(0);
			
			if(n.isFinalState()) {
				break;
			}
			
			Q.addAll(n.getNextStates());
		}
		
		System.out.println("n:");
		System.out.println(n.toString());
		
		return plan;
	}
	
	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
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