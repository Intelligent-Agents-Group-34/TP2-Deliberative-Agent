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
	private City agentPos;
	private int maxAgentWeight;
	private int costPerKm;
	private double cost;
	private List<Task> availableTasks;
	private List<Task> carriedTasks;
	
	private City planInitialCity;
	private List<Action> planActions;
	
	private int agentWeight = 0;
	
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
		
		int i = 0;
		
		// For each task currently carried by the agent, a possibility of transition is
		// to move to the delivery city and deliver the task. If there are tasks available
		// in the delivery city, one of them can also be picked up.
		while(i < carriedTasks.size()) {
			Task cTask = carriedTasks.get(i);
			double cost = this.cost
					+ this.costPerKm*this.agentPos.distanceTo(cTask.deliveryCity);
			
			// Also deliver all tasks with the same delivery city
			List<Task> taskToDeliver = new ArrayList<Task>();
			int j = i + 1;
			while(j < carriedTasks.size()) {
				Task task = carriedTasks.get(j);
				if(task.deliveryCity == cTask.deliveryCity) {
					taskToDeliver.add(task);
				}
				j++;
			}
			// Remove them from the main list so the same transition isn't created
			// multiple times
			carriedTasks.removeAll(taskToDeliver);
			taskToDeliver.add(cTask);
			
			// For each available task
			for(Task aTask : availableTasks) {
				// Check if the available task is located at the delivery city
				if(aTask.pickupCity == cTask.deliveryCity) {
					// If so, create a state corresponding to this case
					State state = new State(cTask.deliveryCity, this.maxAgentWeight,
							this.costPerKm, cost, this.planInitialCity,
							this.planActions);
					
					// Add the actions to the plan required to get into this new state
					for(City c : this.agentPos.pathTo(cTask.deliveryCity)) {
						state.planActions.add(new Move(c));						
					}
					for(Task t : taskToDeliver) {
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
						if(!taskToDeliver.contains(t)) {
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
			for(Task t : taskToDeliver) {
				state.planActions.add(new Delivery(t));
			}
			
			// Add all tasks to the new state except the ones delivered.
			for(Task t : this.availableTasks) {
				state.addAvailableTask(t);
			}
			for(Task t : this.carriedTasks) {
				if(!taskToDeliver.contains(t)) {
					state.addCarriedTask(t);
				}
			}
			
			// Add the state to the list
			nextStatesList.add(state);
			
			availableTasks.removeAll(pickedUpAvailableTasks);
			i++;
		}
		
		// Also add a state corresponding to moving and picking up one of the remaining
		// available tasks
		for(Task task : availableTasks) {
			double cost = this.cost
					+ this.costPerKm*this.agentPos.distanceTo(task.pickupCity);
			State state = new State(task.pickupCity, this.maxAgentWeight,
					this.costPerKm, cost, this.planInitialCity, this.planActions);
			
			// Add the actions to the plan required to get into this new state
			for(City c : this.agentPos.pathTo(task.pickupCity)) {
				state.planActions.add(new Move(c));						
			}
			state.planActions.add(new Pickup(task));
			
			// Add all tasks to the new state. The one which has been picked up must be modified.
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

		nextStatesList.sort(null);
		return nextStatesList;
	}
	
	public boolean isFinalState() {
		return this.agentWeight == 0 && this.availableTasks.isEmpty()
				&& this.carriedTasks.isEmpty();
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
	
	@Override
	public int compareTo(State o) {
		double c1 = this.cost;
		double c2 = o.cost;
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
		return cost;
	}
	
	public Plan getPlan( ) {
		return new Plan(this.planInitialCity, planActions);
	}
}