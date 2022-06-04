# CCC #1204: Trail
This a solution for cloudflight catcoder training game #1204: Trail

This training game can be found here: catcoder.codingcontest.org/training/1204 (Login might be required)

## Functions
* drawing a closed polygon by reading movement instructions in a specific format
* calculating the inside area of the resulting polygon
* caclulating "pockets" at the outside of the polygon

## Movement instructions format

starts with an integer "L" (number of instruction).  
then follows "L" instructions the "S T" format: 
* "S" is a string consisting of 'L', 'R', and 'F' characters, and  
* "T" is an integer indicating how many times "S" is repeated.

The symbols 'L', 'R', and 'F' indicate the following:
* 'L' means to turn left 90 degrees
* 'R' means to turn right 90 degrees
* 'F' means to move forward one unit

Following these instructions will result in a closed polygon:
![Polygon](https://github.com/lionell-tyler/ccc_1204_trail/blob/main/polygon.png?raw=true)


## How to calculate the area
First, we need to sum up all rotations done by the movement instructions to get the "total rotation".  
The total rotation is always either 4 or -4 (360 or -360 degrees), depending on whether the polygon is drawn clockwise or counter-clockwise.

Secondly, for every line segment we draw, we cast a ray to the "inside" of the polygon.   
The "inside" is right on clockwise and left on counter-clockwise.

This image depicts the raycasting. Red a horizontal rays, and blue vertical rays.
![Raycasting](https://github.com/lionell-tyler/ccc_1204_trail/blob/main/raycasting.png?raw=true)  

The intersections of all rays are the centerpoints of all "fields" that are inside the polygon:
![Intersections](https://github.com/lionell-tyler/ccc_1204_trail/blob/main/intersections.png?raw=true)  

## Pockets
Pockets are defined to be "fields" outside the polygon that have boundaries 
either east and west, or north and south of it.

This is calculated by iterating through every field that's not inside the polygog 
and raycasting into each direction to determine if there are boundaries.  

![img_2.png](https://github.com/lionell-tyler/ccc_1204_trail/blob/main/output.png?raw=true)

## Development
This was developed in Kotlin, using IntelliJ IDEA. 
Also heavily using github co-pilot.

