package javachallenge.server;

import javachallenge.units.Unit;
import javachallenge.message.Action;
import javachallenge.message.ActionType;
import javachallenge.message.Delta;
import javachallenge.message.DeltaType;
import javachallenge.util.*;
import javachallenge.util.Map;

import java.util.*;

/**
 * Created by mohammad on 2/5/14.
 */


public class Game {
    private boolean ended;
    private Map map;
    //private ArrayList<Edge> wallsUnderConstruction = new ArrayList<Edge>();
    //private ArrayList<Edge> wallsUnderDestruction = new ArrayList<Edge>();
    private ArrayList<Unit>[][] tempOtherMoves;
    private ArrayList<Unit>[][] tempWallieMoves;
    //private ArrayList<Delta> attackDeltas = new ArrayList<Delta>();
    private ArrayList<Delta> wallDeltas = new ArrayList<Delta>();
    private ArrayList<Delta> moveDeltas = new ArrayList<Delta>();
    private ArrayList<Delta> otherDeltas = new ArrayList<Delta>();
    private static final int MINE_RATE = 4;
    private static final int COST_WALL = 15;
    private static final int GAME_LENGTH = 700;
    //private static final int ATTACKER_SPAWN_RATE = 2;
    //private static final int BOMBER_SPAWN_RATE = 3;
    private static final int CE_SPAWN_RATE = 1;
    public static final int INITIAL_RESOURCE = 10000;
    private int[] resources = new int[2];
    //private ArrayList<UnitWallie> busyWallies = new ArrayList<UnitWallie>();
    private int turn;
    //private Point[] attackerSpawnLocation = new Point[2];
    //private Point[] bomberSpawnLocation = new Point[2];
    //private Point[] ceSpawnLocation = new Point[2];
    //private Point[] destinations = new Point[2];
    private Team CETeam;
    private Team EETeam;
    private int numberOfEEers = 0;

    public boolean isEnded() {
        return ended;
    }

    public Game (Map map) {
        this.map = map;
        tempOtherMoves = new ArrayList[map.getSizeX() + 1][map.getSizeY() + 1];
        CETeam = new Team(0, INITIAL_RESOURCE);
        EETeam = new Team(1, 0);
        //tempWallieMoves = new ArrayList[(map.getSizeX() + 1) * 2][map.getSizeY() + 1];
        for (int i = 0; i < map.getSizeX() + 1; i++)
            for (int j = 0; j < map.getSizeY() + 1; j++)
                tempOtherMoves[i][j] = new ArrayList<Unit>();
    }

    public Map getMap() {
        return map;
    }

    public void handleActions(ArrayList<Action> actions) {
        //ArrayList<Action> attacks = new ArrayList<Action>();
        //ArrayList<Action> constructionDestructionWalls = new ArrayList<Action>();
        ArrayList<Action> moves = new ArrayList<Action>();
        ArrayList<Action> walls = new ArrayList<Action>();
        for (int i = 0; i < actions.size(); i++) {
            /*
            if (actions.get(i).getType() == ActionType.ATTACK) {
                attacks.add(actions.get(i));
            } else if (actions.get(i).getType() == ActionType.MAKE_WALL || actions.get(i).getType() == ActionType.DESTROY_WALL) {
                constructionDestructionWalls.add(actions.get(i));
            } else */
            if (actions.get(i).getType() == ActionType.MOVE)
                moves.add(actions.get(i));
            else if(actions.get(i).getType() == ActionType.MAKE_WALL)
                walls.add(actions.get(i));
        }
        //handleAttacks(attacks);
        //map.updateMap(attackDeltas);

        //handleConstructionDestructionWalls(constructionDestructionWalls);
        handleMakeWalls(walls);
        map.updateMap(this.getWallDeltasList());
        handleMoves(moves);
        map.updateMap(this.getMoveDeltasList());
    }

    public void handleMakeWalls(ArrayList<Action> walls){
        Collections.shuffle(walls);
        ArrayList<Edge> wallsWantMake = new ArrayList<Edge>();
        for (Action wall : walls) {
            if (!map.isNodeInMap(wall.getPosition()))
                continue;
            Point point1 = new Point(wall.getPosition().getX(), wall.getPosition().getY());
            Node node1 = map.getNodeAt(point1.getX(), point1.getY());
            Node node2 = map.getNeighborNode(node1, wall.getNodeDirection());
            Point point2 = new Point(node2.getX(), node2.getY());
            Edge edge = node1.getEdge(wall.getNodeDirection());
            if (CETeam.getResources() >= COST_WALL && wall.getType() == ActionType.MAKE_WALL &&
                    edge.getType() == EdgeType.OPEN) {
                wallsWantMake.add(edge);
                if (isTherePathAfterThisEdges(map.getSpawnPoint(0), map.getDestinationPoint(0), wallsWantMake)) {
                    CETeam.decreaseResources(COST_WALL);
                    wallDeltas.add(new Delta(DeltaType.WALL_DRAW, point1, point2));
                    otherDeltas.add(new Delta(DeltaType.RESOURCE_CHANGE, 0, -COST_WALL));
                }
                else {
                    wallsWantMake.remove(edge);
                }
            }
        }
    }

    private void handleMoves(ArrayList<Action> moves) {
        ArrayDeque<Integer> xOfOverloadedCells = new ArrayDeque<Integer>();
        ArrayDeque<Integer> yOfOverloadedCells = new ArrayDeque<Integer>();

        // moves units to their destination blindly
        for (Action move : moves) {
            Unit unit = map.getCellAtPoint(move.getPosition()).getUnit();
            Cell source = unit.getCell();
            Cell destination = map.getNeighborCell(source, move.getDirection());
            if (destination.getType() != CellType.MOUNTAIN && destination.getType() != CellType.RIVER &&
                    destination.getType() != CellType.OUTOFMAP &&
                    source.getEdge(move.getDirection()).getType() == EdgeType.OPEN) {
                tempOtherMoves[source.getX()][source.getY()].remove(0);
                tempOtherMoves[destination.getX()][destination.getY()].add(unit);
            }
        }

        // find cells with multiple units inside
        for (int i = 0; i < tempOtherMoves.length; i++)
            for (int j = 0; j < tempOtherMoves[0].length; j++)
                if (tempOtherMoves[i][j].size() > 1) {
                    xOfOverloadedCells.add(i);
                    yOfOverloadedCells.add(j);
                }

        Random rand = new Random();
        while (!xOfOverloadedCells.isEmpty()) {
            int xTemp = xOfOverloadedCells.pop();
            int yTemp = yOfOverloadedCells.pop();
            int overloadedNumber = tempOtherMoves[xTemp][yTemp].size();

            if (overloadedNumber < 2)
                continue;

            // checks if a unit stays and some other want to move to its cell
            boolean isDestinationFull = false;
            int stayerId = -1;
            int zombieNum = 0;
            for (int i = 0; i < overloadedNumber; i++) {
                if (tempOtherMoves[xTemp][yTemp].get(i).getTeamId() == 1)
                    zombieNum++;
                Unit existent = tempOtherMoves[xTemp][yTemp].get(i);
                if (existent.getCell().getX() == xTemp && existent.getCell().getY() == yTemp) {
                    isDestinationFull = true;
                    stayerId = existent.getId();
                }
            }

            if (!isDestinationFull) {
                // only move "lasting" unit and others must stay
                int lasting = 0;
                if (zombieNum > 0) {
                    int zombieLasting = rand.nextInt(zombieNum);
                    for (int i = 0; i < overloadedNumber; i++) {
                        if (zombieLasting == 0 && tempOtherMoves[xTemp][yTemp].get(i).getTeamId() == 1)
                            lasting = i;
                        else if (tempOtherMoves[xTemp][yTemp].get(i).getTeamId() == 1)
                            zombieLasting--;
                    }
                } else {
                    lasting = rand.nextInt(overloadedNumber);
                }
                for (int i = overloadedNumber - 1; i >= 0; i--)
                    if (i != lasting) {
                        Unit goner = tempOtherMoves[xTemp][yTemp].get(i);
                        tempOtherMoves[goner.getCell().getX()][goner.getCell().getY()].add(goner);

                        // if some other unit wanted to move to previous location of this unit, they must go back
                        if (tempOtherMoves[goner.getCell().getX()][goner.getCell().getY()].size() > 1) {
                            xOfOverloadedCells.add(goner.getCell().getX());
                            yOfOverloadedCells.add(goner.getCell().getY());
                        }
                        tempOtherMoves[xTemp][yTemp].remove(i);
                    }
            } else {
                for (int i = overloadedNumber - 1; i >= 0; i--) {
                    Unit goner = tempOtherMoves[xTemp][yTemp].get(i);
                    // send everybody back, except the one who stayed in the cell
                    if (goner.getId() != stayerId) {
                        tempOtherMoves[goner.getCell().getX()][goner.getCell().getY()].add(goner);
                        if (tempOtherMoves[goner.getCell().getX()][goner.getCell().getY()].size() > 1) {
                            xOfOverloadedCells.add(goner.getCell().getX());
                            yOfOverloadedCells.add(goner.getCell().getY());
                        }
                        tempOtherMoves[xTemp][yTemp].remove(i);
                    }
                }
            }
        }

        for (int i = 0; i < tempOtherMoves.length; i++)
            for (int j = 0; j < tempOtherMoves[0].length; j++) {
                if (tempOtherMoves[i][j].size() == 0)
                    continue;
                Unit thisUnit = tempOtherMoves[i][j].get(0);
                Cell tempCell = thisUnit.getCell();
                Point sourcePoint = new Point (tempCell.getX(), tempCell.getY());
                // if this unit is moved, make delta
                if (thisUnit.getCell().getX() != i || thisUnit.getCell().getY() != j) {
                    Point destinationPoint = new Point(i, j);
                    moveDeltas.add(new Delta(DeltaType.CELL_MOVE, sourcePoint, destinationPoint));
                    if (destinationPoint.equals(map.getDestinationPoint(0))) {
                        otherDeltas.add(new Delta(DeltaType.AGENT_DISAPPEAR, destinationPoint));
                        CETeam.increaseArrivedNumber();
                    }
                // if this unit is stayed in mine
                } else if (thisUnit.getCell().getX() == i && thisUnit.getCell().getY() == j && thisUnit.getCell().getType() == CellType.MINE) {
                    MineCell mineCell = (MineCell) thisUnit.getCell();
                    if (mineCell.getAmount() >= MINE_RATE) {
                        resources[thisUnit.getTeamId()] += MINE_RATE;
                        otherDeltas.add(new Delta(DeltaType.MINE_CHANGE, sourcePoint, MINE_RATE));
                        otherDeltas.add(new Delta(DeltaType.RESOURCE_CHANGE, thisUnit.getTeamId(), MINE_RATE));
                    } else if (mineCell.getAmount() > 0) {
                        resources[thisUnit.getTeamId()] += mineCell.getAmount();
                        otherDeltas.add(new Delta(DeltaType.MINE_CHANGE, sourcePoint, mineCell.getAmount()));
                        otherDeltas.add(new Delta(DeltaType.RESOURCE_CHANGE, thisUnit.getTeamId(), mineCell.getAmount()));
                    }
                }
            }
    }

    private boolean isTherePathAfterThisEdges (Point sourceInput, Point destinationInput, ArrayList<Edge> barriers) {
        Cell source = map.getCellAt(sourceInput.getX(), sourceInput.getY());
        Cell destination = map. getCellAt(destinationInput.getX(), destinationInput.getY());
        boolean[][] flags = new boolean[map.getSizeX()][map.getSizeY()];
        Cell currentCell;
        Stack<Cell> dfs = new Stack<Cell>();
        dfs.add(source);
        Direction[] dir = Direction.values();
        while (!dfs.isEmpty()) {
            currentCell = dfs.pop();
            if (currentCell.equals(destination))
                return true;
            flags[currentCell.getX()][currentCell.getY()] = true;
            outer: for (int i = 0; i < 6; i++) {
                Cell neighborCell = map.getNeighborCell(currentCell, dir[i]);
                Edge neighborEdge = currentCell.getEdge(dir[i]);
                if (neighborCell != null && flags[neighborCell.getX()][neighborCell.getY()] == false &&
                        neighborEdge.getType() == EdgeType.OPEN &&
                        (neighborCell.getType() == CellType.TERRAIN ||
                        neighborCell.getType() == CellType.MINE ||
                        neighborCell.getType() == CellType.SPAWN ||
                        neighborCell.getType() == CellType.DESTINATION)) {
                    for (int j = 0; j < barriers.size(); j++)
                        if (neighborEdge.equals(barriers.get(j)))
                            continue outer;
                    dfs.add(neighborCell);
                }
            }
        }
        return false;
    }

    public void initTurn (int turn) {
        //attackDeltas = new ArrayList<Delta>();
        wallDeltas = new ArrayList<Delta>();
        moveDeltas = new ArrayList<Delta>();
        otherDeltas = new ArrayList<Delta>();
        tempOtherMoves = new ArrayList[map.getSizeX()][map.getSizeY()];
        for (int i = 0; i < map.getSizeX(); i++)
            for(int j = 0; j < map.getSizeY(); j++) {
                tempOtherMoves[i][j] = new ArrayList<Unit>();
                if (map.getCellAt(i, j).getUnit() != null)
                    tempOtherMoves[i][j].add(map.getCellAt(i, j).getUnit());
            }

        this.turn = turn;
        if (turn == GAME_LENGTH) {
            ended = true;
        }
    }

    public void endTurn() {
        /*
        if (turn % ATTACKER_SPAWN_RATE == 0) {
            otherDeltas.add(new Delta(DeltaType.SPAWN_ATTACKER, attackerSpawnLocation[0]));
            otherDeltas.add(new Delta(DeltaType.SPAWN_ATTACKER, attackerSpawnLocation[1]));
        }
        */
//        if (map.getCellAtPoint(map.getSpawnPoint(1)).getUnit() == null) {
//            otherDeltas.add(new Delta(DeltaType.SPAWN, map.getSpawnPoint(1), 1, numberOfEEers));
//            EETeam.addUnit();
//            numberOfEEers++;
//        }
        if (turn % CE_SPAWN_RATE == 0) {
            System.out.println("EndTurn Called");
            if (map.getCellAtPoint(map.getSpawnPoint(0)).getUnit() == null) {
                Unit newUnit = CETeam.addUnit();
                System.out.println("Generating a SpawnDelta with id = " + newUnit.getId());
                otherDeltas.add(new Delta(DeltaType.SPAWN, map.getSpawnPoint(0), 0, newUnit.getId()));
            }
        }
    }

    public ArrayList<Delta> getWallDeltasList() {
        return wallDeltas;
    }

    public ArrayList<Delta> getMoveDeltasList() {
        return moveDeltas;
    }

    public ArrayList<Delta> getOtherDeltasList() {
        return otherDeltas;
    }
}
