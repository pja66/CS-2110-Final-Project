package app;

import java.util.HashSet;
import java.util.LinkedList;

import game.HuntState;
import game.Hunter;
import game.Node;
import game.NodeStatus;
import game.ScramState;

/** A solution with huntOrb optimized and scram getting out as fast as possible. */
public class Pollack extends Hunter {

    /** Get to the orb in as few steps as possible. <br>
     * Once you get there, you must return from the function in order to pick it up. <br>
     * If you continue to move after finding the orb rather than returning, it will not count.<br>
     * If you return from this function while not standing on top of the orb, it will count as a
     * failure.
     *
     * There is no limit to how many steps you can take, but you will receive<br>
     * a score bonus multiplier for finding the orb in fewer steps.
     *
     * At every step, you know only your current tile's ID and the ID of all<br>
     * open neighbor tiles, as well as the distance to the orb at each of <br>
     * these tiles (ignoring walls and obstacles).
     *
     * In order to get information about the current state, use functions<br>
     * currentLocation(), neighbors(), and distanceToOrb() in HuntState.<br>
     * You know you are standing on the orb when distanceToOrb() is 0.
     *
     * Use function moveTo(long id) in HuntState to move to a neighboring<br>
     * tile by its ID. Doing this will change state to reflect your new position.
     *
     * A suggested first implementation that will always find the orb, but <br>
     * likely won't receive a large bonus multiplier, is a depth-first search. <br>
     * Some modification is necessary to make the search better, in general. */
    @Override
    public void huntOrb(HuntState state) {
        // TODO 1: Get the orb
        dfsWalk(state, new HashSet<Long>(), true); // calls dfs
    }

    // Does a dfsWalk of the maze
    public void dfsWalk(HuntState state, HashSet<Long> visited, boolean horizontal) {

        Heap<NodeStatus> heapNodeSt= new Heap<>(false); // Minheap
        long current= state.currentLocation();
        visited.add(current); // keeps track of visited Nodes

        // Organizes neighbors by shortest distance to the orb
        for (NodeStatus n : state.neighbors()) {
            // Preference is given to going same direction
            // This will generally pick tile w lower pythagorean distance provided 2 taxicab equal
            // options
            double a= n.getDistanceToTarget();
            if (Math.abs(current - n.getId()) < 2 == horizontal) { a= a - 0.5; }
            heapNodeSt.add(n, a);
        }

        // dfs
        while (heapNodeSt.size() != 0) {
            NodeStatus n= heapNodeSt.poll();
            if (!visited.contains(n.getId()) && state.distanceToOrb() != 0) {
                horizontal= Math.abs(current - n.getId()) < 2;
                state.moveTo(n.getId());
                dfsWalk(state, visited, horizontal);
                if (state.distanceToOrb() != 0)
                    state.moveTo(current);
            }
        }
    }

    /** Get out the cavern before the ceiling collapses, trying to collect as <br>
     * much gold as possible along the way. Your solution must ALWAYS get out <br>
     * before time runs out, and this should be prioritized above collecting gold.
     *
     * You now have access to the entire underlying graph, which can be accessed <br>
     * through ScramState. <br>
     * currentNode() and getExit() will return Node objects of interest, and <br>
     * getNodes() will return a collection of all nodes on the graph.
     *
     * Note that the cavern will collapse in the number of steps given by <br>
     * getStepsRemaining(), and for each step this number is decremented by the <br>
     * weight of the edge taken. <br>
     * Use getStepsRemaining() to get the time still remaining, <br>
     * pickUpGold() to pick up any gold on your current tile <br>
     * (this will fail if no such gold exists), and <br>
     * moveTo() to move to a destination node adjacent to your current node.
     *
     * You must return from this function while standing at the exit. <br>
     * Failing to do so before time runs out or returning from the wrong <br>
     * location will be considered a failed run.
     *
     * You will always have enough time to scram using the shortest path from the <br>
     * starting position to the exit, although this will not collect much gold. <br>
     * For this reason, using Dijkstra's to plot the shortest path to the exit <br>
     * is a good starting solution */
    @Override
    public void scram(ScramState state) {
        LinkedList<Node> path= (LinkedList<Node>) Path.shortest(state.currentNode(),
            state.getExit());// Start with shortest path
        HashSet<Node> remainingGoldTiles= new HashSet<>();// Create set of remaining tiles with gold
                                                          // on them
        for (Node a : state.allNodes()) {
            int tileGold= a.getTile().gold();
            if (a.getTile().gold() > 0 && !path.contains(a)) {
                remainingGoldTiles.add(a);
            }
        }
        path= optimize(path, state.stepsLeft(), remainingGoldTiles);// Optimize path
        takePath(path, state);// Take path
    }

    private LinkedList<Node> optimize(LinkedList<Node> path, int moves, HashSet<Node> remaining) {
        double pathGold= sumGold(path);
        Heap<DetourData> bestDetours= new Heap<>(true);
        for (Node target : remaining) {
            DetourData detour= new DetourData(path, target);
            if (detour.totalEdges < moves) {// Need to get to exit on time
                double goldDiff= detour.totalGold - pathGold;// Use extra gold to determine which is
                                                             // best
                try {
                    bestDetours.add(detour, goldDiff);
                } catch (IllegalArgumentException IAE) {// Needed in case already in heap

                }
            }
        }
        if (bestDetours.size() == 0) { return path; }// If no better detours we return the current
                                                     // path, this is the end of the recursion
        DetourData best= bestDetours.poll();// Otherwise take the best option as specified by factor
        HashSet<Node> newRemaining= new HashSet<>();
        for (Node n : remaining) {// Remove nodes in path since we collect their gold
            if (!(best.first.contains(n) || best.second.contains(n))) {
                newRemaining.add(n);
            }
        }// Recursively go on first path and second path then combine
        LinkedList<Node> bestFirst= optimize(best.first, (int) (best.firstWeight * moves),
            newRemaining);
        LinkedList<Node> bestSecond= optimize(best.second, (int) (best.secondWeight * moves),
            newRemaining);
        return combine(bestFirst, bestSecond);

    }

    private LinkedList<Node> combine(LinkedList<Node> first, LinkedList<Node> second) {
        assert first.getLast() == second.getFirst();
        second.removeFirst();
        for (Node n : second) {
            first.addLast(n);
        }
        return first;
    }

    private class DetourData {
        public LinkedList<Node> first;
        public LinkedList<Node> second;
        public double totalGold;
        public double totalEdges;
        public double firstWeight;
        public double secondWeight;
        public Node t;

        public DetourData(LinkedList<Node> path, Node target) {// This is where detour along with
                                                               // amount of gold is calculated
            first= (LinkedList<Node>) Path.shortest(path.getFirst(), target);
            second= (LinkedList<Node>) Path.shortest(target, path.getLast());
            double firstSum= sumEdges(first);
            totalGold= sumGold(first) + sumGold(second);
            totalEdges= firstSum + sumEdges(second);
            firstWeight= firstSum / totalEdges;// Weight is how we determine the move cap
            secondWeight= 1 - firstWeight;
            t= target;
        }
    }

    // Returns the length of a particular path
    public double sumEdges(LinkedList<Node> nodes) {
        return Path.pathSum(nodes);
    }

    // Returns the amount of gold in a particular path
    public double sumGold(LinkedList<Node> nodes) {
        int sum= 0;
        for (Node n : nodes) {
            sum+= n.getTile().gold();
        }
        return sum;
    }

    // moves the character along the given path
    public void takePath(LinkedList<Node> path, ScramState state) {
        // path.pop();
        while (!path.isEmpty()) {
            Node n= path.pop();
            if (n != state.currentNode()) { state.moveTo(n); }
        }
    }

}
