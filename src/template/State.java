package template;

import java.util.ArrayList;
import java.util.List;

import logist.plan.Plan;
import logist.topology.Topology.City;

public class State implements Comparable<State> {
	private City agentPos;
	private List<Task> taskSet;
	private int agentWeight = 0;
	private int maxAgentWeight;
	private int costPerKm;
	private double cost;
	private Plan plan;
	
	public State(City agentPos, int maxAgentWeight, int costPerKm, double cost) {
		this.agentPos = agentPos;
		this.maxAgentWeight = maxAgentWeight;
		this.costPerKm = costPerKm;
		this.cost = cost;
		
		this.plan = new Plan(agentPos);
		
		this.taskSet = new ArrayList<Task>();
	}
	
	public State(City agentPos, int maxAgentWeight, int costPerKm, double cost, Plan plan) {
		this(agentPos, maxAgentWeight, costPerKm, cost);
//		this.plan =
	}
	
	private State(City agentPos, int maxAgentWeight, int costPerKm, double cost, List<Task> taskSet) {
		this(agentPos, maxAgentWeight, costPerKm, cost);
		for(Task task : taskSet) {
			this.taskSet.add(task);
		}
	}
	
	public boolean addTask(City pickupCity, City deliveryCity, int weight, boolean pickedUp) {
		if(pickedUp && this.agentWeight + weight > this.maxAgentWeight) {
			return false;
		}
		else {
			Task task = new Task(pickupCity, deliveryCity, weight, pickedUp);
			this.agentWeight += pickedUp ? weight : 0;
			this.taskSet.add(task);
			return true;
		}
	}
	
	private void addTask(Task task) {
		this.agentWeight += task.pickedUp ? task.weight : 0;
		this.taskSet.add(task);
	}
	
	// Return the list of states that can be reached.
	// Only useful states are taken in consideration, which means the transition must
	// achieve at least one of the following:
	//     - Move and deliver a task previously picked up
	//     - Move and pick up an available task
	public List<State> getNextStates(){
		List<State> nextStatesList = new ArrayList<State>();
		
		List<Task> availableTasks = new ArrayList<Task>();
		List<Task> pickedUpTasks = new ArrayList<Task>();
		List<Task> pickedUpAvailableTasks = new ArrayList<Task>();
		
		for(Task task : this.taskSet) {
			if(task.pickedUp) {
				pickedUpTasks.add(task);
			}
			else {
				availableTasks.add(task);
			}
		}
		
		int i = 0;
		
		// For each task currently carried by the agent, a possibility of transition is
		// to move to the delivery city and deliver the task. If there are tasks available
		// in the delivery city, one of them can also be picked up.
		while(i < pickedUpTasks.size()) {
			Task pTask = pickedUpTasks.get(i);
			double cost = this.cost + this.costPerKm*this.agentPos.distanceTo(pTask.deliveryCity);
			
			// Also deliver all tasks with the same delivery city
			List<Task> taskToDeliver = new ArrayList<Task>();
			int j = i + 1;
			while(j < pickedUpTasks.size()) {
				Task task = pickedUpTasks.get(j);
				if(task.deliveryCity == pTask.deliveryCity) {
					taskToDeliver.add(task);
				}
				j++;
			}
			// Remove them from the main list so the same transition isn't created
			// multiple times
			pickedUpTasks.removeAll(taskToDeliver);
			taskToDeliver.add(pTask);
			
			// For each available task
			for(Task aTask : availableTasks) {
				// Check if the available task is located at the delivery city
				if(aTask.pickupCity == pTask.deliveryCity) {
					// If so, create a state corresponding to this case
					State state = new State(pTask.deliveryCity, this.maxAgentWeight, this.costPerKm, cost);
					
					// Add all tasks to the new state except the ones delivered. The one
					// which has been picked up must also be modified.
					for(Task t : this.taskSet) {
						if(!taskToDeliver.contains(t)) {
							if(t == aTask) {
								state.addTask(t.pickupCity, t.deliveryCity, t.weight, true);
							}
							else {
								state.addTask(t);
							}
						}
					}
					
					// Add the state to the list
					nextStatesList.add(state);
					// Add the available task to the list of tasks which have been picked up
					pickedUpAvailableTasks.add(aTask);
				}
			}
			
			// Also create a state in which you don't pick up any task
			State state = new State(pTask.deliveryCity, this.maxAgentWeight, this.costPerKm, cost);
			
			// Add all tasks to the new state except the ones delivered.
			for(Task t : this.taskSet) {
				if(!taskToDeliver.contains(t)) {
					state.addTask(t);
				}
			}
			
			// Add the state to the list
			nextStatesList.add(state);
			
			availableTasks.removeAll(pickedUpAvailableTasks);
			i++;
		}
		
		// Also add a state corresponding to moving and picking up one of the remaining
		// available task
		for(Task task : availableTasks) {
			double cost = this.cost + this.costPerKm*this.agentPos.distanceTo(task.pickupCity);
			State state = new State(task.pickupCity, this.maxAgentWeight, this.costPerKm, cost);
			
			// Add all tasks to the new state. The one which has been picked up must be modified.
			for(Task t : this.taskSet) {
				if(t == task) {
					state.addTask(t.pickupCity, t.deliveryCity, t.weight, true);
				}
				else {
					state.addTask(t);
				}
			}
			
			// Add the state to the list
			nextStatesList.add(state);
		}

		nextStatesList.sort(null);
		return nextStatesList;
	}
	
	public boolean isFinalState() {
		return this.agentWeight == 0 && this.taskSet.isEmpty();
	}
	
	public String toString() {
		String name = "Agent at " + this.agentPos.name + " with " + this.agentWeight
				+ "/" + this.maxAgentWeight + " kg and cost of " + this.cost + ".\n";
		name += "List of tasks:";
		for(Task task : this.taskSet) {
			String taskName = "\n\t";
			taskName += task.pickedUp ? "Delivering: " : "To deliver : ";
			taskName += "From " + task.pickupCity.name + " to " + task.deliveryCity.name;
			taskName += ", " + task.weight + " kg.";
			name += taskName;
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


	private class Task {
		private final City pickupCity;
		private final City deliveryCity;
		private final int weight;
		private final boolean pickedUp;
		
		public Task(City pickupCity, City deliveryCity, int weight, boolean pickedUp) {
			this.pickupCity = pickupCity;
			this.deliveryCity = deliveryCity;
			this.weight = weight;
			this.pickedUp = pickedUp;
		}
	}
}