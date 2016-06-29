package com.enderio.core.common.fluid;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.enderio.core.api.common.util.ITankAccess;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class FluidWrapper {

  @CapabilityInject(IFluidHandler.class)
  private static final Capability<IFluidHandler> FLUID_HANDLER = null;

  public static @Nullable IFluidWrapper wrap(IBlockAccess world, BlockPos pos, EnumFacing side) {
    if (world == null || pos == null) {
      return null;
    }
    return wrap(world.getTileEntity(pos), side);
  }

  public static @Nullable IFluidWrapper wrap(@Nullable TileEntity te, EnumFacing side) {
    if (te != null && te.hasWorldObj() && !te.isInvalid()) {
      if (te instanceof IFluidWrapper) {
        return (IFluidWrapper) te;
      }
      if (te instanceof ITankAccess) {
        return wrap((ITankAccess) te);
      }
      if (te instanceof net.minecraftforge.fluids.IFluidHandler) {
        return wrap((net.minecraftforge.fluids.IFluidHandler) te, side);
      }
      if (te.hasCapability(FLUID_HANDLER, side)) {
        return wrap(te.getCapability(FLUID_HANDLER, side));
      }
    }
    return null;
  }

  public static TankAccessFluidWrapper wrap(final ITankAccess tankAccess) {
    return new TankAccessFluidWrapper(tankAccess);
  }

  public static LegacyFluidWrapper wrap(final net.minecraftforge.fluids.IFluidHandler fluidHandler, EnumFacing side) {
    if (fluidHandler != null) {
      return new LegacyFluidWrapper(fluidHandler, side);
    }
    return null;
  }

  public static CapabilityFluidWrapper wrap(final IFluidHandler fluidHandler) {
    if (fluidHandler != null) {
      return new CapabilityFluidWrapper(fluidHandler);
    }
    return null;
  }

  public static @Nullable IFluidWrapper wrap(@Nullable FluidTank tank) {
    if (tank != null) {
      return new FluidTankFluidWrapper(tank);
    }
    return null;
  }

  public static @Nullable IFluidWrapper wrap(@Nullable IFluidWrapper wrapper) {
    return wrapper;
  }

  public static @Nullable IFluidWrapper wrap(ItemStack itemStack) {
    if (itemStack != null && itemStack.getItem() instanceof IFluidContainerItem) {
      return new FluidContainerItemFluidWrapper((IFluidContainerItem) itemStack.getItem(), itemStack);
    }
    return wrap(FluidUtil.getFluidHandler(itemStack));
  }

  public static Map<EnumFacing, IFluidWrapper> wrapNeighbours(IBlockAccess world, BlockPos pos) {
    Map<EnumFacing, IFluidWrapper> res = new EnumMap<EnumFacing, IFluidWrapper>(EnumFacing.class);
    for (EnumFacing dir : EnumFacing.values()) {
      IFluidWrapper wrapper = wrap(world, pos.offset(dir), dir.getOpposite());
      if (wrapper != null) {
        res.put(dir, wrapper);
      }
    }
    return res;
  }

  // Some helpers:

  public static int transfer(FluidTank from, FluidTank to, int limit) {
    return transfer(wrap(from), wrap(to), limit);
  }

  public static int transfer(FluidTank from, IFluidWrapper to, int limit) {
    return transfer(wrap(from), wrap(to), limit);
  }

  public static int transfer(FluidTank from, IBlockAccess world, BlockPos topos, EnumFacing toside, int limit) {
    return transfer(wrap(from), wrap(world, topos, toside), limit);
  }

  public static int transfer(FluidTank from, TileEntity to, EnumFacing toside, int limit) {
    return transfer(wrap(from), wrap(to, toside), limit);
  }

  //

  public static int transfer(IFluidWrapper from, FluidTank to, int limit) {
    return transfer(wrap(from), wrap(to), limit);
  }

  public static int transfer(IFluidWrapper from, IBlockAccess world, BlockPos topos, EnumFacing toside, int limit) {
    return transfer(wrap(from), wrap(world, topos, toside), limit);
  }

  public static int transfer(IFluidWrapper from, TileEntity to, EnumFacing toside, int limit) {
    return transfer(wrap(from), wrap(to, toside), limit);
  }

  //

  public static int transfer(IBlockAccess world, BlockPos frompos, EnumFacing fromside, IFluidWrapper to, int limit) {
    return transfer(wrap(world, frompos, fromside), wrap(to), limit);
  }

  public static int transfer(IBlockAccess world, BlockPos frompos, EnumFacing fromside, FluidTank to, int limit) {
    return transfer(wrap(world, frompos, fromside), wrap(to), limit);
  }

  public static int transfer(IBlockAccess world, BlockPos frompos, EnumFacing fromside, BlockPos topos, EnumFacing toside, int limit) {
    return transfer(wrap(world, frompos, fromside), wrap(world, topos, toside), limit);
  }

  public static int transfer(IBlockAccess world, BlockPos frompos, EnumFacing fromside, TileEntity to, EnumFacing toside, int limit) {
    return transfer(wrap(world, frompos, fromside), wrap(to, toside), limit);
  }

  //

  public static int transfer(TileEntity from, EnumFacing fromside, IFluidWrapper to, int limit) {
    return transfer(wrap(from, fromside), wrap(to), limit);
  }

  public static int transfer(TileEntity from, EnumFacing fromside, FluidTank to, int limit) {
    return transfer(wrap(from, fromside), wrap(to), limit);
  }

  public static int transfer(TileEntity from, EnumFacing fromside, IBlockAccess world, BlockPos topos, EnumFacing toside, int limit) {
    return transfer(wrap(from, fromside), wrap(world, topos, toside), limit);
  }

  public static int transfer(TileEntity from, EnumFacing fromside, TileEntity to, EnumFacing toside, int limit) {
    return transfer(wrap(from, fromside), wrap(to, toside), limit);
  }

  //

  public static int transfer(IFluidWrapper from, IFluidWrapper to, int limit) {
    if (from == null || to == null || limit <= 0) {
      return 0;
    }

    FluidStack drainable = from.getAvailableFluid();
    if (drainable == null || drainable.amount <= 0) {
      return 0;
    }

    if (drainable.amount > limit) {
      drainable = drainable.copy();
      drainable.amount = limit;
    }

    int fillable = to.offer(drainable);
    if (fillable <= 0 || fillable > drainable.amount) {
      return 0;
    }

    if (fillable < drainable.amount) {
      drainable.amount = fillable;
    }

    return to.fill(from.drain(drainable));
  }

}
