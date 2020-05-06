package com.pixelcross.cavemanbrawl.levels;

import java.awt.Point;
import java.util.ArrayList;

import com.pixelcross.cavemanbrawl.entities.Entity;
import com.pixelcross.cavemanbrawl.gfx.Assets;
import com.pixelcross.cavemanbrawl.gfx.GameCamera;
import com.pixelcross.cavemanbrawl.levels.tiles.GroundTile;
import com.pixelcross.cavemanbrawl.levels.tiles.GroundTilePattern;
import com.pixelcross.cavemanbrawl.levels.tiles.NextRoomTile;
import com.pixelcross.cavemanbrawl.levels.tiles.WallTile;
import com.pixelcross.cavemanbrawl.levels.tiles.WallTilePattern;
import com.pixelcross.cavemanbrawl.levels.tiles.Tile;
import com.pixelcross.cavemanbrawl.levels.tiles.TileMap;
import com.pixelcross.cavemanbrawl.levels.tiles.TilePattern;

import javafx.scene.canvas.GraphicsContext;

/**
 * @author Justin Schreiber
 *
 * Defines a room in which the player can move around in
 */
public class Room {

	private int id;
	private Level currentLevel;
	private int width, height;
	private TileMap[] tileLayers;
	private RoomGenerator rg;
	private Point playerSpawn;
	private int[] connectedRoomIds;
	private boolean generated;
	private Point startPos;
	ArrayList<Entity> entities;
	
	/**
	 * Creates a Room with a given width and height
	 * 
	 * @param width
	 * @param height
	 */
	public Room(int id, Level currentLevel, int width, int height, int[] connectedRoomIds) {
		this.id = id;
		this.currentLevel = currentLevel;
		this.width = width;
		this.height = height;
		//Initialize all the tile maps 
		//Layers:[1 - Background, 2 - World, 3 - Spawner, 4 - Misc]
		this.tileLayers = new TileMap[4];
		for (int i = 0; i < tileLayers.length; i++) {
			tileLayers[i] = new TileMap(width+4, height+4);
		}
		this.connectedRoomIds = connectedRoomIds;
		generated = false;
		entities = new ArrayList<Entity>();
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void load(int lastRoomId) {
		int lastDoor = -1;
		if (lastRoomId > -1) {
			for (int i = 0; i < connectedRoomIds.length; i++) {
				if (connectedRoomIds[i] == lastRoomId) {
					lastDoor = i;
				}
			}
		}
		if (lastDoor == 0) {
			startPos = new Point(width/2*Tile.TILEWIDTH, Tile.TILEHEIGHT*2);
		} else if (lastDoor == 1) {
			startPos = new Point(Tile.TILEWIDTH*width, height/2*Tile.TILEHEIGHT);
		} else if (lastDoor == 2) {
			startPos = new Point(width/2*Tile.TILEWIDTH, Tile.TILEHEIGHT*height-8);
		} else if (lastDoor == 3) {
			startPos = new Point(Tile.TILEWIDTH*2, height/2*Tile.TILEHEIGHT);
		} else {
			startPos = new Point(0, 0);
		}
	}
	
	/**
	 * Get the tile on a layer at the x and y position defined
	 * 
	 * @param layer (TileMap layer)
	 * @param x
	 * @param y
	 * @return Tile
	 */
	public Tile getTile(int layer, int x, int y) {
		return tileLayers[layer].getTile(x, y);
	}
	
	/**
	 * Generates a room using the MapGenerator and the RoomGenerator
	 * 
	 * @param caveValue (The density of the cave generation)
	 */
	public void generateRoom(int caveValue) {
		boolean[] doors = new boolean[4];
		for (int i = 0; i < 4; i++)
			doors[i] = connectedRoomIds[i] > -1;
		//Creates a map generator with the given 
		//cave density with a size that matches the room
		MapGenerator mg = new MapGenerator(width, height, caveValue);
		//Creates a room generator using the map generated by the MapGenerator
		rg = new RoomGenerator(mg.generateMap(doors));
		//Generates the foreground for the room (Walls)
		int[][] foreground = rg.generateForeground();
		//Generates spawn positions for the room
		int[][] spawns = rg.generateSpawns(doors);
		//Generates the backgound tiles for the room
		generateBackground(new GroundTilePattern());
		//Generates the foreground (walls) using the generated foreground array
		generateForeground(foreground, new WallTilePattern());
		//Generates spawns using the generated spawns array
		generateSpawns(spawns);
		generated = true;
	}
	
	/**
	 * Generates the background tiles
	 */
	private void generateBackground(TilePattern pattern) {
		for (int x = 2; x < width+2; x ++) {
			for (int y = 2; y < height+2; y ++) {
				tileLayers[0].setTile(x, y, new GroundTile(pattern.getTileTexture(new int[]{0,0,0,0,0,0,0,0,0}), 0));
			}
		}
	}
	
	/**
	 * Generates the foreground (wall) tiles based on the foreground array
	 * 
	 * @param foreground
	 */
	private void generateForeground(int[][] foreground, TilePattern pattern) {
		for (int x = 0; x < width+4; x ++) {
			for (int y = 0; y < height+4; y ++) {
				if (x < 2 || y < 2 || x >= width+2 || y >= height+2) {
					if (y == 1 && x > (width+4)/2-4 && x < (width+4)/2+4 && connectedRoomIds[0] > -1) {
						tileLayers[1].setTile(x, y, new NextRoomTile(currentLevel, connectedRoomIds[0]));
					} else if (x == (width+2) && y > (height+4)/2-4 && y < (height+4)/2+4 && connectedRoomIds[1] > -1) {
						tileLayers[1].setTile(x, y, new NextRoomTile(currentLevel, connectedRoomIds[1]));
					} else if (y == (height+2) && x > (width+4)/2-4 && x < (width+4)/2+4 && connectedRoomIds[2] > -1) {
						tileLayers[1].setTile(x, y, new NextRoomTile(currentLevel, connectedRoomIds[2]));
					} else if (x == 1 && y > (height+4)/2-4 && y < (height+4)/2+4 && connectedRoomIds[3] > -1) {
						tileLayers[1].setTile(x, y, new NextRoomTile(currentLevel, connectedRoomIds[3]));
					} else {
						tileLayers[1].setTile(x, y, new WallTile(Assets.walls[6], 0));
					}				
				} else if (foreground[x-2][y-2] == 1) {
					int[] tiles = getSurroundingTiles(x-2, y-2, foreground);
					tileLayers[1].setTile(x, y, new WallTile(pattern.getTileTexture(tiles), 0));
				}
			}
		}
	}
	
	/**
	 * Generates the spawn tiles based on the spawns array
	 * 
	 * @param spawns
	 */
	private void generateSpawns(int[][] spawns) {
		for (int x = 0; x < width; x ++) {
			for (int y = 0; y < height; y ++) {
				if (spawns[x][y] == 1) {
					playerSpawn = new Point(x+2, y+2);
				}
			}
		}
	}
	
	/**
	 * Updates the room
	 */
	public void update() {
		
	}
	
	/**
	 * Renders the room to the screen using the camera
	 * 
	 * @param gc (GraphicsContext object used to draw to the canvas)
	 * @param interpolation (Frame adjustment)
	 * @param camera (Camera used for displaying)
	 */
	public void render(GraphicsContext gc, double interpolation, GameCamera camera) {
		int xStart = (int) Math.max(0, camera.getxOffset() / Tile.TILEWIDTH);
		int xEnd = (int) Math.min(width+4, (camera.getxOffset() + camera.getScreenWidth()) / Tile.TILEWIDTH + 1);
		int yStart = (int) Math.max(0, camera.getyOffset() / Tile.TILEHEIGHT);
		int yEnd = (int) Math.min(height+4, (camera.getyOffset() + camera.getScreenWidth()) / Tile.TILEHEIGHT + 1);
		
		for (int layer = 0; layer < 4; layer++) {
			for(int y = yStart; y < yEnd;y++) {
				for(int x = xStart; x < xEnd;x++) {
					Tile tile = getTile(layer, x, y);
					if (tile != null) {
						tile.render(gc, (int) (x * Tile.TILEWIDTH - camera.getxOffset()), (int) (y * Tile.TILEHEIGHT - camera.getyOffset()));
					}
				}
			}
		}
	}

	public Point getPlayerSpawn() {
		return playerSpawn;
	}

	private int[] getSurroundingTiles(int gridX, int gridY, int[][] map) {
		int[] surroundingWalls = new int[9];
		int indexCounter = 0;
		for (int neighbourY = gridY - 1; neighbourY <= gridY + 1; neighbourY ++) {
			int indexY = neighbourY - gridY + 1;
			for (int neighbourX = gridX - 1; neighbourX <= gridX + 1; neighbourX ++) {
				int indexX = neighbourX - gridX + 1;
				if (neighbourX >= 0 && neighbourX < width && neighbourY >= 0 && neighbourY < height) {
					surroundingWalls[indexY * 3 + indexX] = map[neighbourX][neighbourY];
				}
				else {
					surroundingWalls[indexY * 3 + indexX] = -1;
				}
			}
		}

		return surroundingWalls;
	}

	public boolean isGenerated() {
		return generated;
	}

	public int getId() {
		return id;
	}

	public Point getStartPos() {
		return startPos;
	}
}
