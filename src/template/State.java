package template;

import java.util.ArrayList;
import java.util.List;

import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.plan.Action.Delivery;
import logist.plan.Plan;
import logist.task.Task;
import logist.topology.Topology.City;

public class State implements Comparable<State> {
	private City agentPos; // Current position of the agent
	private int maxAgentWeight; // Maximum weight that the agent's vehicle can carry
	private int costPerKm; // Cost per km of the agent's vehicle
	private double cost; // Cost of the state, without the heuristic value
	private List<Task> availableTasks; // Tasks available on world (not yet picked up)
	private List<Task> carriedTasks; // Tasks currently carried by the agent
	
	private City planInitialCity; // Initial city of the plan
	private List<Action> planActions; // List of actions from the initial state to get into this one
	
	private int agentWeight = 0; // Current weight of the agent
	
	public State(City agentPos, int maxAgentWeight, int costPerKm, double cost,
			List<Task> availableTasks, List<Task> carriedTasks, City planInitialCity,
			List<Action> planActions) {
		this.agentPos = agentPos;
		this.maxAgentWeight = maxAgentWeight;
		this.costPerKm = costPerKm;
		this.cost = cost;
		
		this.availableTasks = new ArrayList<Task>(availableTasks);
		this.carriedTasks = new ArrayList<Task>(carriedTasks);
		
		this.planInitialCity = planInitialCity;
		this.planActions = new ArrayList<Action>(planActions);
		
		for(Task t : carriedTasks) {
			if(canPickUp(t)) {
				this.agentWeight += t.weight;
			}
			else
			{
				System.out.println("Error: trying to create an impossible state:");
				System.out.println(this.toString());
			}
		}
	}
	
	public State(City agentPos, int maxAgentWeight, int costPerKm, double cost,
			List<Task> availableTasks, List<Task> carriedTasks, City planInitialCity) {
		this(agentPos, maxAgentWeight, costPerKm, cost, availableTasks, carriedTasks,
				planInitialCity, new ArrayList<Action>());
	}
	
	public State(City agentPos, int maxAgentWeight, int costPerKm, double cost,
			List<Task> availableTasks, List<Task> carriedTasks) {
		this(agentPos, maxAgentWeight, costPerKm, cost, availableTasks, carriedTasks,
				agentPos);
	}
	
	public State(City agentPos, int maxAgentWeight, int costPerKm, double cost,
			City planInitialCity, List<Action> planActions) {
		this(agentPos, maxAgentWeight, costPerKm, cost, new ArrayList<Task>(),
				new ArrayList<Task>(), planInitialCity, planActions);
	}
	
	public State(City agentPos, int maxAgentWeight, int costPerKm, double cost,
			City planInitialCity) {
		this(agentPos, maxAgentWeight, costPerKm, cost, planInitialCity,
				new ArrayList<Action>());
	}
	
	public State(City agentPos, int maxAgentWeight, int costPerKm, double cost) {
		this(agentPos, maxAgentWeight, costPerKm, cost, agentPos);
	}
	
	public void addAvailableTask(Task task) {
		this.availableTasks.add(task);
	}
	
	public boolean addCarriedTask(Task task) {
		if(this.canPickUp(task)) {
			this.carriedTasks.add(task);
			this.agentWeight += task.weight;
			return true;
		}
		
		return false;
	}
	
	public boolean canPickUp(int weight) {
		return this.agentWeight + weight <= this.maxAgentWeight; 
	}
	
	public boolean canPickUp(Task task) {
		return this.canPickUp(task.weight);
	}
	
	public boolean canDeliverAndPickUp(List<Task> tasksToDeliver, Task taskToPickUp) {
		int weight = taskToPickUp.weight;
		for(Task t : tasksToDeliver) {
			weight -= t.weight;
		}
		
		return this.canPickUp(weight);
	}
	
	// Return the list of states that can be reached.
	// Only useful states are taken into consideration, which means the transition must
	// achieve at least one of the following:
	//     - Move and deliver a task previously picked up
	//     - Move and pick up an available task
	public List<State> getNextStates(){
		List<State> nextStatesList = new ArrayList<State>();
		
		List<Task> availableTasks = new ArrayList<Task>(this.availableTasks);
		List<Task> carriedTasks = new ArrayList<Task>(this.carriedTasks);
		List<Task> pickedUpAvailableTasks = new ArrayList<Task>();
		
		// For each task currently carried by the agent, a possibility of transition is
		// to move to the delivery city and deliver the task. If there are tasks available
		// in the delivery city, one of them can also be picked up.
		for(int i = 0; i < carriedTasks.size(); i++) {
			Task cTask = carriedTasks.get(i);
			double cost = this.cost
					+ this.costPerKm*this.agentPos.distanceTo(cTask.deliveryCity);
			
			// Also deliver all tasks with the same delivery city
			List<Task> tasksToDeliver = new ArrayList<Task>();
			for(int j = i + 1; j < carriedTasks.size(); j++) {
				Task task = carriedTasks.get(j);
				if(task.deliveryCity == cTask.deliveryCity) {
					tasksToDeliver.add(task);
				}
			}
			// Remove them from the main list so the same transition isn't created
			// multiple times
			carriedTasks.removeAll(tasksToDeliver);
			tasksToDeliver.add(cTask);
			
			// For each available task
			for(Task aTask : availableTasks) {
				// Check if the available task is located at the delivery city
				if(aTask.pickupCity == cTask.deliveryCity) {
					// Check if the task can be picked up
					if(!this.canDeliverAndPickUp(tasksToDeliver, aTask))
						continue;
					
					// If so, create a state corresponding to this case
					State state = new State(cTask.deliveryCity, this.maxAgentWeight,
							this.costPerKm, cost, this.planInitialCity,
							this.planActions);
					
					// Add the actions to the plan required to get into this new state
					for(City c : this.agentPos.pathTo(cTask.deliveryCity)) {
						state.planActions.add(new Move(c));						
					}
					for(Task t : tasksToDeliver) {
						state.planActions.add(new Delivery(t));
					}
					state.planActions.add(new Pickup(aTask));
					
					// Add all tasks to the new state except the ones delivered. The one
					// which has been picked up must also be modified.
					for(Task t : this.availableTasks) {
						if(t == aTask) {
							state.addCarriedTask(t);
						}
						else {
							state.addAvailableTask(t);
						}
					}
					for(Task t : this.carriedTasks) {
						if(!tasksToDeliver.contains(t)) {
							state.addCarriedTask(t);
						}
					}
					
					// Add the state to the list
					nextStatesList.add(state);
					// Add the available task to the list of tasks which have been picked up
					pickedUpAvailableTasks.add(aTask);
				}
			}
			
			// Also create a state in which you don't pick up any task
			State state = new State(cTask.deliveryCity, this.maxAgentWeight,
					this.costPerKm, cost, this.planInitialCity, this.planActions);
			
			// Add the actions to the plan required to get into this new state
			for(City c : this.agentPos.pathTo(cTask.deliveryCity)) {
				state.planActions.add(new Move(c));						
			}
			for(Task t : tasksToDeliver) {
				state.planActions.add(new Delivery(t));
			}
			
			// Add all tasks to the new state except the ones delivered
			for(Task t : this.availableTasks) {
				state.addAvailableTask(t);
			}
			for(Task t : this.carriedTasks) {
				if(!tasksToDeliver.contains(t)) {
					state.addCarriedTask(t);
				}
			}
			
			// Add the state to the list
			nextStatesList.add(state);
			
			availableTasks.removeAll(pickedUpAvailableTasks);
		}
		
		// Also add a state corresponding to moving and picking up one of the remaining
		// available tasks
		for(Task task : availableTasks) {
			// Check if the task can be picked up
			if(!this.canPickUp(task))
				continue;
			
			double cost = this.cost
					+ this.costPerKm*this.agentPos.distanceTo(task.pickupCity);
			State state = new State(task.pickupCity, this.maxAgentWeight,
					this.costPerKm, cost, this.planInitialCity, this.planActions);
			
			// Add the actions to the plan required to get into this new state
			for(City c : this.agentPos.pathTo(task.pickupCity)) {
				state.planActions.add(new Move(c));						
			}
			state.planActions.add(new Pickup(task));
			
			// Add all tasks to the new state. The one which has been picked up must be modified
			for(Task t : this.availableTasks) {
				if(t == task) {
					state.addCarriedTask(t);
				}
				else {
					state.addAvailableTask(t);
				}
			}
			for(Task t : this.carriedTasks) {
				state.addCarriedTask(t);
			}
			
			// Add the state to the list
			nextStatesList.add(state);
		}

		return nextStatesList;
	}
	
	// Return whether this state is a final or not. State are considered to be final
	// if no task remains (carried and in world).
	public boolean isFinalState() {
		return this.availableTasks.isEmpty() && this.carriedTasks.isEmpty();
	}
	
	// Return whether the two states are cost independently equal.
	public boolean isStateEqual(State o) {
		boolean res = true;

		res &= o.agentPos == this.agentPos;
		res &= o.maxAgentWeight == this.maxAgentWeight;
		res &= o.costPerKm == this.costPerKm;
		
		res &= o.availableTasks.containsAll(this.availableTasks);
		res &= this.availableTasks.containsAll(o.availableTasks);
		
		res &= o.carriedTasks.containsAll(this.carriedTasks);
		res &= this.carriedTasks.containsAll(o.carriedTasks);
		
		res &= o.planInitialCity == this.planInitialCity;
		res &= o.agentWeight == this.agentWeight;
		
		return res;
	}
	
	// See report for details
	public double getHeuristicValue() {
		double maxDist = 0;
		
		for(Task t : this.availableTasks) {
			double dist = this.agentPos.distanceTo(t.pickupCity);
			dist += t.pickupCity.distanceTo(t.deliveryCity);
			if(dist > maxDist) {
				maxDist = dist;
			}
		}
		
		for(Task t : this.carriedTasks) {
			double dist = this.agentPos.distanceTo(t.deliveryCity);
			if(dist > maxDist) {
				maxDist = dist;
			}
		}
		
		return this.costPerKm*maxDist;
	}
	
	public String toString() {
		String name = "Agent at " + this.agentPos.name + " with " + this.agentWeight
				+ "/" + this.maxAgentWeight + " kg and cost of " + this.cost + ".\n";
		name += "List of tasks:";
		
		for(Task task : this.availableTasks) {
			name += "\n\tTo deliver : " + "From " + task.pickupCity.name + " to "
					+ task.deliveryCity.name + ", " + task.weight + " kg.";
		}
		
		for(Task task : this.carriedTasks) {
			name += "\n\tDelivering : " + "From " + task.pickupCity.name + " to "
					+ task.deliveryCity.name + ", " + task.weight + " kg.";
		}
		
		return name;
	}
	
	// States are compared relative to their cost
	@Override
	public int compareTo(State o) {
		double c1 = this.getCost();
		double c2 = o.getCost();
		return c1 > c2 ? 1 : (c1 < c2 ? -1 : 0);
	}


	public City getAgentPos() {
		return agentPos;
	}

	public int getAgentWeight() {
		return agentWeight;
	}

	public int getMaxAgentWeight() {
		return maxAgentWeight;
	}

	public int getCostPerKm() {
		return costPerKm;
	}

	public double getCost() {
		return this.cost + this.getHeuristicValue();
	}
	
	public Plan getPlan( ) {
		return new Plan(this.planInitialCity, this.planActions);
	}
}