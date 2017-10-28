package com.enderio.core;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.enderio.core.api.common.config.IConfigHandler;
import com.enderio.core.common.CommonProxy;
import com.enderio.core.common.Handlers;
import com.enderio.core.common.Lang;
import com.enderio.core.common.OreDict;
import com.enderio.core.common.command.CommandReloadConfigs;
import com.enderio.core.common.command.CommandScoreboardInfo;
import com.enderio.core.common.compat.CompatRegistry;
import com.enderio.core.common.config.ConfigHandler;
import com.enderio.core.common.enchant.EnchantAutoSmelt;
import com.enderio.core.common.enchant.EnchantXPBoost;
import com.enderio.core.common.imc.IMCRegistry;
import com.enderio.core.common.network.EnderPacketHandler;
import com.enderio.core.common.util.EnderFileUtils;
import com.enderio.core.common.util.NullHelper;
import com.enderio.core.common.util.PermanentCache;
import com.enderio.core.common.util.stackable.Things;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.minecraft.command.CommandHandler;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms.IMCEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(modid = EnderCore.MODID, name = EnderCore.NAME, version = EnderCore.VERSION, dependencies = "after:ttCore", guiFactory = "com.enderio.core.common.config.BaseConfigFactory")
public class EnderCore implements IEnderMod {

  public static final @Nonnull String MODID = "endercore";
  public static final @Nonnull String DOMAIN = MODID.toLowerCase(Locale.US);
  public static final @Nonnull String NAME = "EnderCore";
  public static final @Nonnull String BASE_PACKAGE = "com.enderio";
  public static final @Nonnull String VERSION = "@VERSION@";

  public static final @Nonnull Logger logger = NullHelper.notnull(LogManager.getLogger(NAME), "failed to aquire logger");
  public static final @Nonnull Lang lang = new Lang(MODID);

  @Instance(MODID)
  public static EnderCore instance;

  @SidedProxy(serverSide = "com.enderio.core.common.CommonProxy", clientSide = "com.enderio.core.client.ClientProxy")
  public static CommonProxy proxy;

  public final @Nonnull List<IConfigHandler> configs = Lists.newArrayList();

  private final @Nonnull Set<String> invisibleRequesters = Sets.newHashSet();

  /**
   * Call this method BEFORE preinit (construction phase) to request that EnderCore start in invisible mode. This will disable ANY gameplay features unless the
   * user forcibly disables invisible mode in the config.
   */
  public void requestInvisibleMode() {
    final ModContainer activeModContainer = Loader.instance().activeModContainer();
    if (activeModContainer != null) {
      invisibleRequesters.add(activeModContainer.getName());
    } else {
      invisibleRequesters.add("null");
    }
  }

  public boolean invisibilityRequested() {
    return !invisibleRequesters.isEmpty();
  }

  public @Nonnull Set<String> getInvisibleRequsters() {
    return ImmutableSet.copyOf(invisibleRequesters);
  }

  @EventHandler
  public void preInit(@Nonnull FMLPreInitializationEvent event) {

    ConfigHandler.configFolder = event.getModConfigurationDirectory();
    ConfigHandler.enderConfigFolder = new File(ConfigHandler.configFolder.getPath() + "/" + MODID);
    ConfigHandler.configFile = new File(ConfigHandler.enderConfigFolder.getPath() + "/" + event.getSuggestedConfigurationFile().getName());

    if (!ConfigHandler.configFile.exists() && event.getSuggestedConfigurationFile().exists()) {
      try {
        FileUtils.copyFile(event.getSuggestedConfigurationFile(), ConfigHandler.configFile);
      } catch (IOException e) {
        Throwables.propagate(e);
      }
      EnderFileUtils.safeDelete(event.getSuggestedConfigurationFile());
    }

    ConfigHandler.instance().initialize(NullHelper.notnullJ(ConfigHandler.configFile, "it was there a second ago, I swear!"));
    Handlers.preInit(event);

    CompatRegistry.INSTANCE.handle(event);
    OreDict.registerVanilla();

    //EnchantXPBoost.register(); //todo: fix
    //EnchantAutoSmelt.register(); //todo: fix

    proxy.onPreInit(event);
  }

  @EventHandler
  public void init(@Nonnull FMLInitializationEvent event) {
    Things.init(event);
    EnderPacketHandler.init();

    for (IConfigHandler c : configs) {
      c.initHook();
    }

    Handlers.register(event);
    CompatRegistry.INSTANCE.handle(event);
    if (event.getSide().isServer()) {
      ((CommandHandler) FMLCommonHandler.instance().getMinecraftServerInstance().getCommandManager()).registerCommand(CommandReloadConfigs.SERVER);
    } else {
      ClientCommandHandler.instance.registerCommand(CommandReloadConfigs.CLIENT);
    }

    IMCRegistry.INSTANCE.init();
  }

  @EventHandler
  public void postInit(@Nonnull FMLPostInitializationEvent event) {
    for (IConfigHandler c : configs) {
      c.postInitHook();
    }

    CompatRegistry.INSTANCE.handle(event);
    ConfigHandler.instance().loadRightClickCrops();
  }

  @EventHandler
  public void onServerStarting(@Nonnull FMLServerStartingEvent event) {
    event.registerServerCommand(new CommandScoreboardInfo());
    PermanentCache.saveCaches();
  }

  @EventHandler
  public void onIMCEvent(@Nonnull IMCEvent event) {
    IMCRegistry.INSTANCE.handleEvent(event);
  }

  @Override
  public @Nonnull String modid() {
    return MODID;
  }

  @Override
  public @Nonnull String name() {
    return NAME;
  }

  @Override
  public @Nonnull String version() {
    return VERSION;
  }
}
