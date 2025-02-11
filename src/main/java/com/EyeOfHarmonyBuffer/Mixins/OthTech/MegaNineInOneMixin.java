package com.EyeOfHarmonyBuffer.Mixins.OthTech;

import com.EyeOfHarmonyBuffer.Config.MainConfig;
import com.EyeOfHarmonyBuffer.Mixins.Invoker.MegaNineInOneInvoker;
import com.newmaa.othtech.machine.OTEMegaNineInOne;
import com.newmaa.othtech.machine.machineclass.OTH_MultiMachineBase;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.stream.Stream;

@Mixin(value = OTEMegaNineInOne.class,remap = false)
public abstract class MegaNineInOneMixin extends OTH_MultiMachineBase<OTEMegaNineInOne> {

    public MegaNineInOneMixin(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    @Shadow
    public abstract int getCoilTier();

    @Inject(method = "createProcessingLogic",at = @At("HEAD"),cancellable = true)
    public void createProcessingLogic(CallbackInfoReturnable<ProcessingLogic> cir) {
        if(MainConfig.MegaNineInOneEnable){
            ProcessingLogic logic = new ProcessingLogic(){
                private ItemStack lastCircuit = null;

                @NotNull
                @Override
                public CheckRecipeResult process() {
                    return super.process();
                }

                @Override
                protected @NotNull CheckRecipeResult validateRecipe(@NotNull GTRecipe recipe) {
                    return CheckRecipeResultRegistry.SUCCESSFUL;
                }

                @Nonnull
                @Override
                protected Stream<GTRecipe> findRecipeMatches(@Nullable RecipeMap<?> map) {
                    ItemStack circuit = ((MegaNineInOneInvoker)machine).invokeGetCircuit(inputItems);
                    if (circuit == null) {
                        return Stream.empty();
                    }
                    if (!GTUtility.areStacksEqual(circuit, lastCircuit)) {
                        lastRecipe = null;
                        lastCircuit = circuit;
                    }

                    int circuitID = ((MegaNineInOneInvoker) machine).invokeGetCircuitID(circuit);
                    RecipeMap<?> foundMap = MegaNineInOneInvoker.invokeGetRecipeMap(circuitID);

                    if (foundMap == null) {
                        return Stream.empty();
                    }
                    return super.findRecipeMatches(foundMap);
                }

                @Override
                protected double calculateDuration(@Nonnull GTRecipe recipe, @Nonnull ParallelHelper helper,
                                                   @Nonnull OverclockCalculator calculator) {
                    return 10;
                }

                @NotNull
                @Override
                protected OverclockCalculator createOverclockCalculator(@NotNull GTRecipe recipe) {
                    return new OverclockCalculator()
                        //.setSpeedBoost(100.0) // 速度提升 100 倍
                        .setParallel(Integer.MAX_VALUE) // 最大并行数
                        .setEUt(0); // 不耗电
                }

                @NotNull
                @Override
                protected ParallelHelper createParallelHelper(@NotNull GTRecipe recipe) {
                    return new ParallelHelper()
                        .setRecipe(recipe)
                        .setItemInputs(inputItems)
                        .setFluidInputs(inputFluids)
                        .setAvailableEUt(Integer.MAX_VALUE) // 设置无限能量
                        .setMachine(machine, protectItems, protectFluids)
                        .setMaxParallel(Integer.MAX_VALUE) // 设置极大并行
                        .setEUtModifier(0.0) // 不耗电
                        .enableBatchMode(batchSize) // 启用批量模式
                        .setConsumption(true)
                        .setOutputCalculation(true);
                }

            };
            cir.setReturnValue(logic);
            cir.cancel();
        }
    }
}
