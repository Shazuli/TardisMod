package tardis.tileents;

import tardis.TardisMod;
import tardis.api.IActivatable;
import tardis.core.Helper;
import tardis.core.TardisConfigFile;
import tardis.core.TardisOutput;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class TardisCoreTileEntity extends TardisAbstractTileEntity implements IActivatable
{
	private static TardisConfigFile config;
	private int exteriorWorld;
	private int exteriorX;
	private int exteriorY;
	private int exteriorZ;
	
	private int tickCount = 0;
	
	private double speed = 4;
	private static double maxSpeed;
	
	private int energy;
	private int maxEnergy;
	
	private boolean inFlight = false;
	private int flightTimer = 0;
	private int inFlightTimer = 0;
	private int totalFlightTimer = 0;
	
	private int numRooms = 0;
	private int maxNumRooms = 30;
	
	private int shields;
	private int maxShields;
	private int hull;
	private int maxHull;
	
	private int desDim = 0;
	private int desX = 0;
	private int desY = 0;
	private int desZ = 0;
	private boolean desLandGround = false;
	private String[] desStrs = null;
	
	private boolean landingPlayed;
	
	private String ownerName;
	
	{
		config = TardisMod.configHandler.getConfigFile("tardisCore");
		maxSpeed = config.getDouble("maxSpeed", 8);
		maxEnergy = config.getInt("maxEnergy", 1000);
		maxNumRooms = config.getInt("maxRooms", 30);
		maxShields  = config.getInt("maxShields", 1000);
		maxHull		= config.getInt("maxHull", 1000);
		shields		= maxShields;
		hull		= maxHull;
		energy = 100;
	}
	
	public boolean activate(EntityPlayer player, int side)
	{
		if(ownerName != null)
			leaveTardis(player);
		return true;
	}
	
	public void enterTardis(EntityPlayer player)
	{
		Helper.teleportEntity(player, worldObj.provider.dimensionId, 9, 29, 0);
	}
	
	public void leaveTardis(EntityPlayer player)
	{
		if(!inFlight)
		{
			World ext = Helper.getWorld(exteriorWorld);
			if(ext != null)
			{
				if(ext.isRemote)
					return;
				int facing = ext.getBlockMetadata(exteriorX, exteriorY, exteriorZ);
				int dx = 0;
				int dz = 0;
				switch(facing)
				{
					case 0:dz = -1; break;
					case 1:dx =  1; break;
					case 2:dz =  1; break;
					case 3:dx = -1; break;
				}
				
				if(ext.isAirBlock(exteriorX+dx, exteriorY, exteriorZ+dz) && ext.isAirBlock(exteriorX+dx, exteriorY, exteriorZ+dz))
				{
					Helper.teleportEntity(player, exteriorWorld, exteriorX+dx, exteriorY+1, exteriorZ+dz);
				}
				else
				{
					player.addChatMessage("The door is obstructed");
				}
			}
			else
				player.addChatMessage("The door refuses to open");
		}
		else
		{
			player.addChatMessage("The TARDIS is in flight");
		}
	}
	
	public void takeOff()
	{
		if(!inFlight)
		{
			inFlight = true;
			inFlightTimer = 0;
			flightTimer = 0;
			sendUpdate();
		}
	}
	
	private void placeBox()
	{
		if(worldObj.isRemote)
			return;
		
		TardisConsoleTileEntity con = getConsole();
		int dX = con.getXFromControls();
		int dY = con.getYFromControls();
		int dZ = con.getZFromControls();
		int facing = con.getFacingFromControls();
		World w = MinecraftServer.getServer().worldServerForDimension(con.getDimFromControls());
		boolean landOnGround = con.getLandFromControls();
		if(landOnGround)
		{
			int offset = 1;
			while(dY - offset > 0 && w.isAirBlock(dX, dY-offset, dZ))
				offset++;
			dY = dY + 1 - offset;
		}
		w.setBlock(dX, dY, dZ, TardisMod.tardisBlock.blockID, facing, 3);
		w.setBlock(dX, dY+1, dZ, TardisMod.tardisTopBlock.blockID, facing, 3);
		
		setExterior(w,dX,dY,dZ);
		TileEntity te = w.getBlockTileEntity(dX,dY,dZ);
		if(te != null && te instanceof TardisTileEntity)
		{
			TardisTileEntity tardis = (TardisTileEntity) te;
			tardis.linkedCore = this;
			tardis.linkedDimension = worldObj.provider.dimensionId;
			tardis.land();
		}
	}
	
	public void land()
	{
		if(inFlight)
		{
			inFlight = false;
			sendUpdate();
			worldObj.playSound(xCoord, yCoord, zCoord, "tardismod:engineDrum", 0.75F, 1, true);
			TardisTileEntity ext = getExterior();
			if(ext != null)
				ext.forceLand();
			TardisConsoleTileEntity con = getConsole();
			if(con != null)
				con.land();
		}
	}
	
	@Override
	public void updateEntity()
	{
		tickCount++;
		
		if(inFlight)
		{
			if(inFlightTimer == 0)
				worldObj.playSound(xCoord, yCoord, zCoord, "tardismod:takeoff", 0.75F, 1, true);
			totalFlightTimer++;
			inFlightTimer++;
			int timeTillTakenOff = (20 * 11);
			int timeTillLanding = timeTillTakenOff +  (int) ((14 - getSpeed()) * 69);
			int timeTillLandingInt = timeTillLanding + (20 * 7);
			int timeTillLanded  = timeTillLanding + (20 * 11);
			if(inFlightTimer >= timeTillTakenOff)//Taken off
			{
				if(inFlightTimer == (20 * 22))// remove old tardis
				{
					World w = Helper.getWorld(exteriorWorld);
					if(w != null)
					{
						if(w.getBlockId(exteriorX,exteriorY,exteriorZ) == TardisMod.tardisBlock.blockID)
						{
							w.setBlockToAir(exteriorX, exteriorY, exteriorZ);
							w.setBlockToAir(exteriorX, exteriorY+1, exteriorZ);
							TardisOutput.print("TCTE", "Blanking exterior");
							exteriorWorld = 10000;
							exteriorX = 0;
							exteriorY = 0;
							exteriorZ = 0;
						}
					}
				}
				if(inFlightTimer < timeTillLanding)
				{
					if(flightTimer % 69 == 0 && inFlight)
						worldObj.playSound(xCoord, yCoord, zCoord, "tardismod:engines", 0.75F, 1, true);
					flightTimer++;
				}
				else
				{
					if(flightTimer % 69 == 0 && inFlightTimer < timeTillLandingInt)
						worldObj.playSound(xCoord, yCoord, zCoord, "tardismod:engines", 0.75F, 1, true);
					flightTimer++;
					
					if(inFlightTimer == timeTillLanding)
						placeBox();
					
					if(inFlightTimer == timeTillLandingInt)
						worldObj.playSound(xCoord, yCoord, zCoord, "tardismod:landingInt", 0.75F, 1, true);
					
					if(inFlightTimer >= timeTillLanded)
						land();
				}
			}
		}
		
		if(!worldObj.isRemote)
			if(tickCount % 10 == 0)
				worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
	}
	
	private void sendUpdate()
	{
		worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
	}
	
	////////////////////////////////////////////////
	//////////////DATA STUFF////////////////////////
	////////////////////////////////////////////////
	
	public boolean inCoordinatedFlight()
	{
		return inFlight;
	}
	
	public boolean inFlight()
	{
		return inFlight;
	}
	
	public float getProximity()
	{
		if(inFlight)
		{
			int rate = 40;
			double val = Math.abs((tickCount % rate) - (rate / 2));
			double max = 0.4;
			return (float) (max * 2 * (val / rate));
		}
		else
		{
			return 0;
		}
	}
	
	public void setOwner(String name)
	{
		ownerName = name;
	}
	
	public void setExterior(World w, int x, int y, int z)
	{
		exteriorWorld = w.provider.dimensionId;
		exteriorX = x;
		exteriorY = y;
		exteriorZ = z;
		TardisOutput.print("TCTE", "Exterior placed @ " + x + ","+ y +","+z+","+exteriorWorld +","+worldObj.isRemote);
		worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
	}
	
	public TardisTileEntity getExterior()
	{
		World w = Helper.getWorld(exteriorWorld);
		if(w != null)
		{
			TileEntity te = w.getBlockTileEntity(exteriorX,exteriorY,exteriorZ);
			if(te != null && te instanceof TardisTileEntity)
				return (TardisTileEntity) te;
		}
		return null;
	}
	
	public String getOwner()
	{
		return ownerName;
	}
	
	public TardisConsoleTileEntity getConsole()
	{
		TileEntity te = worldObj.getBlockTileEntity(xCoord, yCoord - 2, zCoord);
		if(te != null)
		{
			if(te instanceof TardisConsoleTileEntity)
				return (TardisConsoleTileEntity)te;
		}
		return null;
	}
	
	public double getMaxSpeed()
	{
		return maxSpeed;
	}
	
	public double getSpeed()
	{
		return speed;
	}
	
	public double addSpeed(double a)
	{
		if(!inFlight)
			speed = speed + a;
		speed = Helper.clamp(speed, 0, getMaxSpeed());
		return speed;
	}
	
	public int getNumRooms()
	{
		return numRooms;
	}
	
	public int getMaxNumRooms()
	{
		return maxNumRooms;
	}
	
	public boolean addRoom(boolean sub)
	{
		if(sub && numRooms > 0)
		{
			numRooms --;
			return true;
		}
		
		if(!sub && numRooms < maxNumRooms)
		{
			numRooms++;
			return true;
		}
		
		return false;
	}
	
	public int getMaxEnergy()
	{
		return maxEnergy;
	}
	
	public int getEnergy()
	{
		return energy;
	}
	
	public boolean takeEnergy(int amount, boolean sim)
	{
		return true;
	}
	
	public int getShields()
	{
		return shields;
	}
	
	public int getMaxShields()
	{
		return maxShields;
	}
	
	public int getHull()
	{
		return hull;
	}
	
	public int getMaxHull()
	{
		return maxHull;
	}
	
	public void sendDestinationStrings(EntityPlayer pl)
	{
		TardisConsoleTileEntity console = getConsole();
		if(console != null)
		{
			int dD = console.getDimFromControls();
			int dX = console.getXFromControls();
			int dY = console.getYFromControls();
			int dZ = console.getZFromControls();
			TardisOutput.print("TCTE","Dest:" + dD +","+dX+","+dY+","+dZ);
			if(dD==desDim&&dX==desX&&dY==desY&&dZ==desZ&&desStrs!=null)
				for(String s:desStrs)
					pl.addChatMessage(s);
			else
			{
				int instability = 20;
				desDim = dD;
				desX = dX;
				desY = dY;
				desZ = dZ;
				String[] send = new String[4];
				send[0] = "The TARDIS will materialize in dimension " + dD + " near:";
				send[1] = "x = " + (dX + (rand.nextInt(2 * instability) - instability)); 
				send[2] = "y = " + (dY + (rand.nextInt(2 * instability) - instability)); 
				send[3] = "z = " + (dZ + (rand.nextInt(2 * instability) - instability)); 
				desStrs = send;
				for(String s:desStrs)
					pl.addChatMessage(s);
			}
		}
	}
	
	//////////////////////////////
	//////NBT DATA////////////////
	//////////////////////////////
	
	public void repair(String newO, int numRoom, int en)
	{
		energy = en;
		numRooms = numRoom;
		maxNumRooms = config.getInt("maxRooms", 30);
		setOwner(newO);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		if(nbt.hasKey("ownerName"))
		{
			ownerName = nbt.getString("ownerName");
			exteriorWorld = nbt.getInteger("extWorld");
			exteriorX = nbt.getInteger("extX");
			exteriorY = nbt.getInteger("extY");
			exteriorZ = nbt.getInteger("extZ");
			
			energy = nbt.getInteger("energy");
			
			flightTimer = nbt.getInteger("flightTimer");
			inFlightTimer = nbt.getInteger("inFlightTimer");
			totalFlightTimer = nbt.getInteger("totalFlightTimer");
			inFlight = nbt.getBoolean("inFlight");
			numRooms = nbt.getInteger("numRooms");
			speed = nbt.getDouble("speed");
			
			shields  = nbt.getInteger("shields");
			hull     = nbt.getInteger("hull");
		}
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		if(ownerName != null)
		{
			nbt.setString("ownerName", ownerName);
			nbt.setInteger("extWorld", exteriorWorld);
			nbt.setInteger("extX", exteriorX);
			nbt.setInteger("extY", exteriorY);
			nbt.setInteger("extZ", exteriorZ);
			
			nbt.setInteger("energy",energy);
			
			nbt.setBoolean("inFlight", inFlight);
			nbt.setInteger("flightTimer", flightTimer);
			nbt.setInteger("totalFlightTimer", totalFlightTimer);
			nbt.setInteger("inFlightTimer", inFlightTimer);
			nbt.setInteger("numRooms", numRooms);
			nbt.setDouble("speed", speed);
			
			nbt.setInteger("shields",shields);
			nbt.setInteger("hull",hull);
		}
	}

}
