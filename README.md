# 2110A7
Cornell CS 2110 Assignment 7 (Final Project)

Work distribution: I wrote the initial algorithm for the first part (huntOrb), while Brendan Sullivan provided an optimization. 

Under src/app, you can see the files we wrote.  Heap and Path are implementations of a max/min heap and of Djikstra's algorithm, respectively.  Pollack is where the main algorithm is located.  The first part of the algorithm is concerned with locating the exit to a maze as quickly as possible.  The whole layout of the maze is not known, merely the taxicab distances to the exit ("orb") of the current tile and the four tiles immediately surrounding it.  Our greedy algorithm includes a slight modification in order to slightly favor continuing in the same direction when confronted with two options, as this will generally minimize the pythagorean distance to the orb.  

The second part is concerned with collecting as much gold as possible before exiting the maze.  Now the tiles are nodes whose edges have different weights, and each tile contains an amount of gold.  Since exiting the maze on time is an absolute priority in the rules of the project, we begin by finding the shortest path between our current spot and the exit.  We then search every node, looking for the detour (shortest path from the start to that node, then shortest path from that node to the exit) that will provide the most additional gold.  We then split the path in two, breaking at the detour node, and apply the algorithm recursively until no additional gold can be found without breaking the step limit.  
