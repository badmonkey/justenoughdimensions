package fi.dy.masa.justenoughdimensions.command;

import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.DimensionManager;
import fi.dy.masa.justenoughdimensions.JustEnoughDimensions;
import fi.dy.masa.justenoughdimensions.command.utils.CommandJEDDefaultGameType;
import fi.dy.masa.justenoughdimensions.command.utils.CommandJEDDifficulty;
import fi.dy.masa.justenoughdimensions.command.utils.CommandJEDGameRule;
import fi.dy.masa.justenoughdimensions.command.utils.CommandJEDTime;
import fi.dy.masa.justenoughdimensions.command.utils.CommandJEDWeather;
import fi.dy.masa.justenoughdimensions.command.utils.CommandJEDWorldBorder;
import fi.dy.masa.justenoughdimensions.config.DimensionConfig;

public class CommandJED extends CommandBase
{

    @Override
    public String getName()
    {
        return "jed";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return  "jed.commands.usage.generic";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos)
    {
        if (args.length == 1)
        {
            return getListOfStringsMatchingLastWord(args,
                    "debug",
                    "defaultgametype",
                    "difficulty",
                    "gamerule",
                    "register",
                    "reload",
                    "seed",
                    "time",
                    "unregister",
                    "unregister-remove",
                    "weather",
                    "worldborder"
                );
        }
        else if (args.length >= 2)
        {
            String cmd = args[0];
            int trim = 2;
            int len = args.length;

            try { parseInt(args[1]); }
            catch (NumberInvalidException e)
            {
                if (sender.getCommandSenderEntity() == null)
                {
                    return super.getTabCompletions(server, sender, args, pos);
                }
                trim = 1;
            }
            len -= trim;

            if (cmd.equals("weather"))
            {
                return getListOfStringsMatchingLastWord(args, "clear", "rain", "thunder");
            }
            else if (cmd.equals("time"))
            {
                if (len == 1)
                {
                    return getListOfStringsMatchingLastWord(args, "add", "set", "query");
                }
                else if (len == 2 && args[args.length - 2].equals("set"))
                {
                    return getListOfStringsMatchingLastWord(args, "day", "night");
                }
                else if (len == 2 && args[args.length - 2].equals("query"))
                {
                    return getListOfStringsMatchingLastWord(args, "day", "daytime", "gametime");
                }
            }
            else if (cmd.equals("defaultgametype") && len == 1)
            {
                return getListOfStringsMatchingLastWord(args, "survival", "creative", "adventure", "spectator");
            }
            else if (cmd.equals("difficulty") && len == 1)
            {
                return getListOfStringsMatchingLastWord(args, "peaceful", "easy", "normal", "hard");
            }
            else if (cmd.equals("gamerule"))
            {
                if (len == 1)
                {
                    return getListOfStringsMatchingLastWord(args, this.getOverWorldGameRules(server).getRules());
                }
                else if (len == 2 && this.getOverWorldGameRules(server).areSameType(args[args.length - 2], GameRules.ValueType.BOOLEAN_VALUE))
                {
                    return getListOfStringsMatchingLastWord(args, "true", "false");
                }
            }
            else if (cmd.equals("worldborder"))
            {
                if (len == 1)
                {
                    return getListOfStringsMatchingLastWord(args, "set", "center", "damage", "warning", "add", "get");
                }
                else if (len >= 2)
                {
                    if (args[args.length - 2].equals("damage"))
                    {
                        return getListOfStringsMatchingLastWord(args, "buffer", "amount");
                    }
                    else if (args[args.length - 2].equals("warning"))
                    {
                        return getListOfStringsMatchingLastWord(args, "time", "distance");
                    }
                    else if (args[args.length - len].equals("center") && len <= 3)
                    {
                        return getTabCompletionCoordinateXZ(args, trim + 1, pos);
                    }
                }
            }
        }

        return super.getTabCompletions(server, sender, args, pos);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length == 0)
        {
            throw new WrongUsageException(this.getUsage(sender));
        }

        String cmd = args[0];

        if (cmd.equals("reload"))
        {
            DimensionConfig.instance().readDimensionConfig();
            DimensionConfig.instance().registerDimensions();
            notifyCommandListener(sender, this, "jed.commands.reloaded");
        }
        else if (cmd.equals("unregister"))
        {
            if (args.length == 2)
            {
                int dimension = parseInt(args[1]);
                this.unregister(dimension);
                notifyCommandListener(sender, this, "jed.commands.unregister", Integer.valueOf(dimension));
            }
            else
            {
                throw new WrongUsageException("jed.commands.usage.unregister");
            }
        }
        else if (cmd.equals("unregister-remove"))
        {
            if (args.length == 2)
            {
                int dimension = parseInt(args[1]);
                this.unregister(dimension);
                DimensionConfig.instance().removeDimensionAndSaveConfig(dimension);
                notifyCommandListener(sender, this, "jed.commands.unregister.remove", Integer.valueOf(dimension));
            }
            else
            {
                throw new WrongUsageException("jed.commands.usage.unregister.remove");
            }
        }
        else if (cmd.equals("register"))
        {
            if (args.length == 2)
            {
                int dimension = parseInt(args[1]);
                DimensionConfig.instance().registerNewDimension(dimension);
                notifyCommandListener(sender, this, "jed.commands.register.default", Integer.valueOf(dimension));
            }
            else if (args.length == 6 || args.length == 7)
            {
                int dimension = parseInt(args[1]);
                boolean keepLoaded = Boolean.parseBoolean(args[4]);
                boolean override = args.length == 7 ? Boolean.parseBoolean(args[6]) : false;
                DimensionConfig.instance().registerNewDimension(dimension, args[2], args[3], keepLoaded, args[5], override);
                notifyCommandListener(sender, this, "jed.commands.register.custom", Integer.valueOf(dimension));
            }
            else
            {
                throw new WrongUsageException("jed.commands.usage.register");
            }
        }
        else if (cmd.equals("debug"))
        {
            World world = null;
            try { int dim = parseInt(args[1]); world = DimensionManager.getWorld(dim); }
            catch (Exception e) { Entity ent = sender.getCommandSenderEntity(); if (ent != null) world = ent.getEntityWorld(); }

            if (world != null)
            {
                IChunkProvider cp = world.getChunkProvider();
                JustEnoughDimensions.logger.info("============= JED DEBUG START ==========");
                JustEnoughDimensions.logger.info("DIM: {}", world.provider.getDimension());
                JustEnoughDimensions.logger.info("Seed: {}", world.getWorldInfo().getSeed());
                JustEnoughDimensions.logger.info("World {}", world.getClass().getName());
                WorldType type = world.getWorldInfo().getTerrainType();
                JustEnoughDimensions.logger.info("WorldType: {} - {}", type.getName(), type.getClass().getName());
                JustEnoughDimensions.logger.info("WorldProvider: {}", world.provider.getClass().getName());
                JustEnoughDimensions.logger.info("ChunkProvider: {}", cp.getClass().getName());
                JustEnoughDimensions.logger.info("ChunkProviderServer.chunkGenerator: {}",
                        ((cp instanceof ChunkProviderServer) ? ((ChunkProviderServer) cp).chunkGenerator.getClass().getName() : "null"));
                JustEnoughDimensions.logger.info("BiomeProvider: {}", world.getBiomeProvider().getClass().getName());
                JustEnoughDimensions.logger.info("============= JED DEBUG END ==========");

                sender.sendMessage(new TextComponentString("Debug output printed to console"));
            }
        }
        else
        {
            Entity entity = sender.getCommandSenderEntity();
            int dimension = entity != null ? entity.getEntityWorld().provider.getDimension() : 0;
            int trim = args.length >= 2 ? 2 : 1;

            if (args.length >= 2)
            {
                try { dimension = parseInt(args[1]); }
                catch (NumberInvalidException e)
                {
                    if (entity == null) { throw new WrongUsageException("jed.commands.usage.generic"); }
                    trim = 1;
                }
            }

            args = dropFirstStrings(args, trim);

            if (cmd.equals("defaultgametype"))
            {
                CommandJEDDefaultGameType.execute(this, dimension, args, server, sender);
            }
            else if (cmd.equals("difficulty"))
            {
                CommandJEDDifficulty.execute(this, dimension, args, sender);
            }
            else if (cmd.equals("gamerule"))
            {
                CommandJEDGameRule.execute(this, dimension, args, server, sender);
            }
            else if (cmd.equals("seed"))
            {
                this.commandSeed(dimension, sender);
            }
            else if (cmd.equals("time"))
            {
                CommandJEDTime.execute(this, dimension, args, sender);
            }
            else if (cmd.equals("weather"))
            {
                CommandJEDWeather.execute(this, dimension, args, sender);
            }
            else if (cmd.equals("worldborder"))
            {
                CommandJEDWorldBorder.execute(this, dimension, args, server, sender);
            }
            else
            {
                throw new WrongUsageException("jed.commands.usage.generic");
            }
        }
    }

    private void commandSeed(int dimension, ICommandSender sender) throws CommandException
    {
        World world = DimensionManager.getWorld(dimension);

        if (world == null)
        {
            throw new NumberInvalidException("jed.commands.error.dimension.notloaded", Integer.valueOf(dimension));
        }

        sender.sendMessage(new TextComponentString("DIM: " + dimension + " - Seed: " + world.getWorldInfo().getSeed()));
    }

    private GameRules getOverWorldGameRules(MinecraftServer server)
    {
        return server.worldServerForDimension(0).getGameRules();
    }

    private void unregister(int dimension) throws CommandException
    {
        if (DimensionManager.isDimensionRegistered(dimension))
        {
            if (DimensionManager.getWorld(dimension) == null)
            {
                DimensionManager.unregisterDimension(dimension);
            }
            else
            {
                throw new NumberInvalidException("jed.commands.error.dimension.loaded", Integer.valueOf(dimension));
            }
        }
        else
        {
            throw new NumberInvalidException("jed.commands.error.dimension.notregistered", Integer.valueOf(dimension));
        }
    }

    public static String[] dropFirstStrings(String[] input, int toDrop)
    {
        if (toDrop > input.length)
        {
            return new String[0];
        }

        String[] arr = new String[input.length - toDrop];
        System.arraycopy(input, toDrop, arr, 0, input.length - toDrop);
        return arr;
    }
}
